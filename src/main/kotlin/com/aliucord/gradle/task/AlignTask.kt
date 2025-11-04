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

import com.aliucord.gradle.getAndroid
import com.android.build.gradle.internal.SdkComponentsBuildService
import com.android.build.gradle.internal.services.getBuildService
import com.android.sdklib.BuildToolInfo
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.internal.ExecActionFactory
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Aligns a zip file to be loaded by Android.
 * This includes aligning native libraries, `resources.so`, and compiled dex files.
 */
public abstract class AlignTask : DefaultTask() {
    @get:InputFile
    public abstract val inputZip: RegularFileProperty

    @get:OutputFile
    public abstract val outputZip: RegularFileProperty

    @get:Inject
    protected abstract val execActionFactory: ExecActionFactory

    private val zipAlignExecutable: Provider<File>

    init {
        val android = project.extensions.getAndroid()
        val sdkService = getBuildService<SdkComponentsBuildService, SdkComponentsBuildService.Parameters>(
            buildServiceRegistry = project.gradle.sharedServices)
        val sdkLoader = sdkService.map {
            it.sdkLoader(
                compileSdkVersion = project.provider { android.compileSdkVersion },
                buildToolsRevision = project.provider { android.buildToolsRevision },
            )
        }

        usesService(sdkService)
        zipAlignExecutable = sdkLoader.flatMap { it.buildToolInfoProvider }
            .map { File(it.getPath(BuildToolInfo.PathId.ZIP_ALIGN)) }
    }

    @TaskAction
    public fun align() {
        execActionFactory.newExecAction().run {
            executable = zipAlignExecutable.get().absolutePath
            args("-v") // Verbose output
            args("-f") // Overwrite existing
            args("-P", "16") // Align native libs to a 16KiB page size
            args("4") // Align all other files to 4 bytes
            args(inputZip.get().asFile.absolutePath)
            args(outputZip.get().asFile.absolutePath)

            val output = ByteArrayOutputStream()
            isIgnoreExitValue = true
            standardOutput = output
            errorOutput = output

            val result = execute()
            if (result.exitValue != 0) {
                logger.error(output.toString())
                result.assertNormalExitValue()
            } else {
                logger.info(output.toString())
            }
        }
    }
}
