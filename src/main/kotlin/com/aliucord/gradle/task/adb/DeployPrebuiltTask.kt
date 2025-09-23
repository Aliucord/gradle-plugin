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

package com.aliucord.gradle.task.adb

import kotlinx.serialization.json.Json
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import java.io.File

private const val REMOTE_ALIUCORD_DIR = "/storage/emulated/0/Aliucord"

/**
 * Pushes either an Aliucord core or an Aliucord plugin to the device.
 *
 * - When deploying plugins, the plugin is pushed to `/storage/emulated/0/Aliucord/plugins`, corresponding to the
 * primary Android user's external storage. The plugin is then forcefully enabled by changing Aliucord's settings.
 * - When deploying Aliucord Core, the bundle is pushed to `/storage/emulated/0/Aliucord/Aliucord.zip`, similarly to
 * when pushing plugins. Aliucord's settings are then changed to force enable using the local core bundle.
 */
@DisableCachingByDefault
public abstract class DeployPrebuiltTask : AdbTask() {
    public enum class DeployType {
        Core,
        Plugin,
    }

    @get:Input
    public abstract var deployType: DeployType

    @get:InputFile
    public abstract val deployFile: RegularFileProperty

    @TaskAction
    public fun deploy() {
        createAliucordDirs()

        when (deployType) {
            DeployType.Core -> deployCore(deployFile.get().asFile)
            DeployType.Plugin -> deployPlugin(deployFile.get().asFile)
        }
    }

    private fun deployCore(file: File) {
        runAdbCommand(
            "push",
            file.absolutePath,
            "$REMOTE_ALIUCORD_DIR/Aliucord.zip"
        )
        editAliucordSettings {
            set("AC_from_storage", true)
        }

        logger.lifecycle("Deployed Aliucord core to configured devices")
    }

    private fun deployPlugin(file: File) {
        runAdbCommand(
            "push",
            file.absolutePath,
            "$REMOTE_ALIUCORD_DIR/plugins/${file.name}"
        )
        editAliucordSettings {
            set("AC_PM_${file.nameWithoutExtension}", true)
        }

        logger.lifecycle("Deployed plugin ${file.nameWithoutExtension} to configured devices")
    }

    /**
     * Creates the Aliucord directory on the device along with all the subfolders (plugins, themes, settings).
     */
    protected fun createAliucordDirs() {
        runAdbShell(
            "mkdir",
            "-v", // Verbose
            "-p", // Create all parents
            "$REMOTE_ALIUCORD_DIR/plugins",
            "$REMOTE_ALIUCORD_DIR/themes",
            "$REMOTE_ALIUCORD_DIR/settings",
        )
    }

    /**
     * Reads Aliucord core's settings from the device, then applies [block] to it,
     * and writes it back to the device.
     */
    protected fun editAliucordSettings(block: (MutableMap<Any?, Any?>).() -> Unit) {
        val localSettingsFile = temporaryDir.resolve("settings.json")
        val remoteSettingsPath = "$REMOTE_ALIUCORD_DIR/settings/Aliucord.json"
        val settings = ByteArrayOutputStream()

        try {
            runAdbCommand("pull", remoteSettingsPath, localSettingsFile.absolutePath)
        } catch (e: AdbException) {
            if (e.message?.contains("No such file or directory") == true) {
                settings.reset()
            } else {
                throw e
            }
        }

        val json = if (settings.size() == 0) {
            mutableMapOf()
        } else {
            Json.decodeFromString<Map<Any, Any>>(settings.toString(Charsets.UTF_8))
        }
        val modifiedJson = block(json.toMutableMap())
        val outJson = Json.encodeToString(modifiedJson)

        localSettingsFile.writeText(outJson)
        runAdbCommand("push", localSettingsFile.absolutePath, remoteSettingsPath)
    }
}
