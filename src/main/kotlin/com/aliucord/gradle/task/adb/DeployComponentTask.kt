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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault

/**
 * Pushes a custom component build to a device(s) with Aliucord Manager installed.
 *
 * For example, when deploying Aliucord Injector, the dex is pushed to `/data/local/tmp/aliucord`,
 * an intent is launched, starting Aliucord Manager to import the component to its internal storage.
 * Aliucord Manager then prompts to start a new installation of Aliucord.
 */
@DisableCachingByDefault
public abstract class DeployComponentTask : AdbTask() {
    @get:Input
    public abstract var componentType: String

    @get:InputFile
    public abstract val componentFile: RegularFileProperty

    @TaskAction
    public fun deploy() {
        val componentFile = componentFile.get().asFile
        val remoteComponentPath = "/data/local/tmp/${componentFile.name}"

        runAdbShell(
            "am", "start",
            "-n", "com.aliucord.manager/.MainActivity",
            "-a", "com.aliucord.manager.IMPORT_COMPONENT",
            "--es", "aliucord.file", "'$remoteComponentPath'",
            "--es", "aliucord.componentType", "'$componentType'",
        )
        runAdbCommand("push", "\"${componentFile.absolutePath}\"", "\"$remoteComponentPath\"")

        // Wait a bit to let Aliucord Manager import the component
        Thread.sleep(2000)

        runAdbShell("rm", "-rf", "'$remoteComponentPath'")
    }
}
