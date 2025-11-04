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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.process.internal.ExecActionFactory
import java.io.File
import javax.inject.Inject

/**
 * Compiles an Android project's resources using aapt, and outputs an apk containing no code.
 */
public abstract class CompileResourcesTask : DefaultTask() {
    @get:InputDirectory
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    public abstract val input: DirectoryProperty

    @get:InputFile
    public abstract val manifestFile: RegularFileProperty

    @get:OutputFile
    public abstract val outputFile: RegularFileProperty

    @get:Inject
    protected abstract val execActionFactory: ExecActionFactory

    private val androidJar: Provider<File>
    private val aaptExecutable: Provider<File>

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
        androidJar = sdkLoader.flatMap { it.androidJarProvider }
        aaptExecutable = sdkLoader.flatMap { it.buildToolInfoProvider }
            .map { File(it.getPath(BuildToolInfo.PathId.AAPT2)) }
    }

    @TaskAction
    public fun compile() {
        val tmpRes = File.createTempFile("res", ".zip")

        execActionFactory.newExecAction().apply {
            executable = aaptExecutable.get().path
            args("compile")
            args("--dir", input.asFile.get().path)
            args("-o", tmpRes.path)
            execute()
        }

        execActionFactory.newExecAction().apply {
            executable = aaptExecutable.get().path
            args("link")
            args("-I", androidJar.get().path)
            args("-R", tmpRes.path)
            args("--manifest", manifestFile.get().asFile.path)
            args("-o", outputFile.get().asFile.path)
            args("--auto-add-overlay")
            execute()
        }

        tmpRes.delete()
    }
}
