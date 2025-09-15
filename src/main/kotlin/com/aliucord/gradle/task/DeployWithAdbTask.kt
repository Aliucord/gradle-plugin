/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.aliucord.gradle.task

import com.aliucord.gradle.ProjectType
import com.aliucord.gradle.getAliucord
import com.android.build.gradle.BaseExtension
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import se.vidstige.jadb.*
import java.io.ByteArrayOutputStream
import java.io.File

abstract class DeployWithAdbTask : DefaultTask() {
    @get:Input
    @set:Option(option = "wait-for-debugger", description = "Enables debugging flag when starting the discord activity")
    var waitForDebugger: Boolean = false

    @TaskAction
    fun deployWithAdb() {
        val extension = project.extensions.getAliucord()
        val android = project.extensions.getByName("android") as BaseExtension

        AdbServerLauncher(Subprocess(), android.adbExecutable.absolutePath).launch()
        val jadbConnection = JadbConnection()
        val devices = jadbConnection.devices.filter {
            try {
                it.state == JadbDevice.State.Device
            } catch (e: JadbException) {
                false
            }
        }

        require(devices.size == 1) {
            "Only one ADB device should be connected, but ${devices.size} were!"
        }

        val device = devices[0]

        val make = project.tasks.getByName("make") as AbstractCopyTask

        var file = make.outputs.files.singleFile

        if (extension.projectType.get() == ProjectType.INJECTOR) {
            file = file.resolve("Injector.dex")
        }

        when (extension.projectType.get()) {
            ProjectType.CORE -> deployCore(device, file)
            ProjectType.INJECTOR -> deployInjector(device, file)
            ProjectType.PLUGIN -> deployPlugin(device, file)
        }

        logger.lifecycle("Deployed $file to ${device.serial}")
    }

    private fun deployCore(device: JadbDevice, file: File) {
        createAliucordDirs(device)
        device.push(file, RemoteFile("$REMOTE_ALIUCORD_DIR/Aliucord.zip"))
        editAliucordSettings(device) {
            // Force enable using custom Aliucord core
            set("AC_from_storage", true)
        }
        restartAliucord(device)
    }

    private fun deployInjector(device: JadbDevice, file: File) {
        device.executeShell("mkdir", "-p", "$REMOTE_TMP/aliucord")
        device.push(file, RemoteFile("$REMOTE_TMP/aliucord/${file.name}"))
        restartManagerImport(device, "aliucord/${file.name}", "injector")
    }

    private fun deployPlugin(device: JadbDevice, file: File) {
        createAliucordDirs(device)
        device.push(file, RemoteFile("$REMOTE_ALIUCORD_DIR/plugins/${file.name}"))
        editAliucordSettings(device) {
            // Force enable the plugin in settings
            set("AC_PM_${file.nameWithoutExtension}", true)
        }
        restartAliucord(device)
    }

    /**
     * Creates the Aliucord directory on the device along with all the subfolders (plugins, themes, settings).
     */
    private fun createAliucordDirs(device: JadbDevice) {
        device.executeShell(
            "mkdir",
            "-v", // Verbose
            "-p", // Create all parents
            "$REMOTE_ALIUCORD_DIR/plugins",
            "$REMOTE_ALIUCORD_DIR/themes",
            "$REMOTE_ALIUCORD_DIR/settings",
        )
    }

    /**
     * Force (re)starts the main Aliucord activity on the device.
     */
    private fun restartAliucord(device: JadbDevice) {
        val args = arrayListOf(
            "start",
            "-S", // Force restart app
            "-n", "com.aliucord/com.discord.app.AppActivity\$Main",
        )
        if (waitForDebugger)
            args += "-D"

        val response = device.executeShell("am", *args.toTypedArray())
            .readAllBytes()
            .decodeToString()

        if (response.contains("Error")) {
            logger.error(response)
            throw GradleException("Failed to deploy core to device")
        }
    }

    private fun restartManagerImport(device: JadbDevice, componentPath: String, componentType: String) {
        val args = arrayListOf(
            "start",
            "-n", "com.aliucord.manager/.MainActivity",
            "-a", "com.aliucord.manager.IMPORT_COMPONENT",
            "--es", "aliucord.file", componentPath,
            // TODO: change customComponentType to componentType in Manager
            "--es", "aliucord.componentType", componentType,
        )

        val response = device.executeShell("am", *args.toTypedArray())
            .readAllBytes()
            .decodeToString()

        if (response.contains("Error")) {
            logger.error(response)
            throw GradleException("Failed to start custom component import on device")
        }
    }

    /**
     * Reads Aliucord core's settings from the device, then applies [block] to it,
     * and writes it back to the device.
     */
    private fun editAliucordSettings(device: JadbDevice, block: (MutableMap<Any?, Any?>).() -> Unit) {
        val settingsFile = RemoteFile("$REMOTE_ALIUCORD_DIR/settings/Aliucord.json")
        val settings = ByteArrayOutputStream()

        try {
            device.pull(settingsFile, settings)
        } catch (e: JadbException) {
            if (e.message?.contains("No such file or directory") == true) {
                settings.reset()
            } else {
                throw e
            }
        }

        val json = if (settings.size() == 0) {
            mutableMapOf<Any, Any>()
        } else {
            JsonSlurper().parse(settings.toByteArray()) as Map<*, *>
        }
        val modifiedJson = block(json.toMutableMap())
        val outJson = JsonOutput.toJson(modifiedJson)

        device.push(
            /* source = */ outJson.byteInputStream(),
            /* lastModified = */ System.currentTimeMillis(),
            /* mode = */ 432, // ug=rw
            /* remote = */ settingsFile,
        )
    }

    private companion object {
        const val REMOTE_ALIUCORD_DIR = "/storage/emulated/0/Aliucord"
        const val REMOTE_TMP = "/data/local/tmp"
    }
}
