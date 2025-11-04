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

import com.aliucord.gradle.getAndroid
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.util.concurrent.TimeUnit

/**
 * A base task for handling and executing ADB commands on a configured set of devices.
 */
@DisableCachingByDefault
public abstract class AdbTask : DefaultTask() {
    @get:Input
    @set:Option(
        option = "wait-for-debugger",
        description = "Enables debugging flag when starting the discord activity",
    )
    public var waitForDebugger: Boolean = false

    @Optional
    @get:Input
    @set:Option(
        option = "device-serial",
        description = "The serial number of the device to deploy to, or a list of comma-separated serials. " +
            "Set this to 'all' to deploy to all connected devices. " +
            "If not set, then this defaults to any *single* connected device.",
    )
    public var deviceSerial: String? = null

    @get:InputFile
    public val adbExecutable: RegularFileProperty = project.objects.fileProperty()
        .fileProvider(project.provider { project.extensions.getAndroid().adbExecutable })

    /**
     * Runs a command inside an adb shell on all devices specified by [deviceSerial].
     */
    protected fun runAdbShell(vararg args: String) {
        val serialConfig = deviceSerial
        val allSerials = getAllSerials()
        val serials = when (serialConfig) {
            null -> {
                require(allSerials.size == 1) {
                    "Only one ADB device should be connected, but ${allSerials.size} were!"
                }
                listOf(allSerials.first())
            }

            "all" -> allSerials

            else -> {
                val expectedSerials = serialConfig.split(',')
                require(expectedSerials.isNotEmpty()) { "No device serials provided to deploy to!" }

                for (expectedSerial in expectedSerials) {
                    require(expectedSerial in allSerials) {
                        "Device $expectedSerial is not connected!"
                    }
                }

                expectedSerials
            }
        }

        for (serial in serials) {
            logger.info("Running command on device $serial")
            runAdbShell(emulatorSerial = serial, *args)
        }
    }

    private fun runAdbShell(emulatorSerial: String, vararg args: String): String =
        runAdbCommand("-s", emulatorSerial, "shell", *args)

    private fun getAllSerials(): List<String> {
        val output = runAdbCommand("devices")
        val devices = output
            .trim().lines()
            .drop(1) // Omit header
            .map { it.trim() }

        // Looking for "<serial>    device"
        val regex = "\\s+".toRegex()
        return devices.mapNotNull {
            val values = it.split(regex)

            if (values.size != 2)
                logger.info("adb devices returned unexpected output: $values")

            if (values[1] == "device") {
                logger.info("Found device: ${values[0]}")
                values[0]
            } else {
                logger.info("Found inactive device: ${values[0]} status: ${values[1]}")
                null
            }
        }
    }

    protected fun runAdbCommand(vararg args: String): String {
        val adbProcess = ProcessBuilder(adbExecutable.get().asFile.absolutePath, *args).start()

        try {
            adbProcess.waitFor(60, TimeUnit.SECONDS)

            if (adbProcess.exitValue() != 0) {
                throw AdbException("adb exited with a non-zero exit code. " +
                    "Command: adb ${args.joinToString(" ")} " +
                    "Error: " + adbProcess.errorReader().readText())
            } else {
                return adbProcess.inputReader().readText()
            }
        } catch (_: InterruptedException) {
            adbProcess.destroy()
            throw AdbException("adb command timed out. adb ${args.joinToString(" ")}")
        }
    }

    protected class AdbException(error: String) : Exception(error)
}
