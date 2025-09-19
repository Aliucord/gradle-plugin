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

import com.android.build.gradle.BaseExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.getByName
import java.io.File

public abstract class CompileResourcesTask : Exec() {
    @get:InputDirectory
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    public abstract val input: DirectoryProperty

    @get:InputFile
    public abstract val manifestFile: RegularFileProperty

    @get:OutputFile
    public abstract val outputFile: RegularFileProperty

    override fun exec() {
        val android = project.extensions.getByName<BaseExtension>("android")

        val aaptExecutable = android.sdkDirectory.resolve("build-tools")
            .resolve(android.buildToolsVersion)
            .resolve(if (OperatingSystem.current().isWindows) "aapt2.exe" else "aapt2")

        val tmpRes = File.createTempFile("res", ".zip")

        execActionFactory.newExecAction().apply {
            executable = aaptExecutable.path
            args("compile")
            args("--dir", input.asFile.get().path)
            args("-o", tmpRes.path)
            execute()
        }

        execActionFactory.newExecAction().apply {
            executable = aaptExecutable.path
            args("link")
            args(
                "-I",
                android.sdkDirectory
                    .resolve("platforms")
                    .resolve(android.compileSdkVersion!!)
                    .resolve("android.jar")
            )
            args("-R", tmpRes.path)
            args("--manifest", manifestFile.asFile.get().path)
            args("-o", outputFile.asFile.get().path)
            args("--auto-add-overlay")
            execute()
        }

        tmpRes.delete()
    }
}
