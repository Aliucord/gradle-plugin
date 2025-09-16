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
import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions.ErrorFormatMode
import com.android.builder.dexing.*
import com.android.builder.dexing.r8.ClassFileProviderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.getByName
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

// TODO: include `implementation` dependencies when building

public abstract class CompileDexTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    public val input: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    @TaskAction
    public fun compileDex() {
        val android = project.extensions.getByName<BaseExtension>("android")
        val minSdkVersion = requireNotNull(android.defaultConfig.minSdkVersion?.apiLevel) {
            "minSdkVersion is required to compile to dex!"
        }

        val bootClasspath = ClassFileProviderFactory(android.bootClasspath.map(File::toPath))
        val classpath = ClassFileProviderFactory(listOf<Path>())

        val dexBuilder = DexArchiveBuilder.createD8DexBuilder(
            DexParameters(
                minSdkVersion = minSdkVersion,
                debuggable = true,
                dexPerClass = false,
                withDesugaring = true,
                desugarBootclasspath = bootClasspath,
                desugarClasspath = classpath,
                coreLibDesugarConfig = null,
                enableApiModeling = true,
                messageReceiver = MessageReceiverImpl(
                    ErrorFormatMode.HUMAN_READABLE,
                    LoggerFactory.getLogger(CompileDexTask::class.java)
                )
            )
        )

        try {
            outputDir.asFile.get().mkdirs()

            dexBuilder.convert(
                input = input.asFileTree
                    // For each input path...
                    .map { path ->
                        ClassFileInputs
                            // ... scan for class files
                            .fromPath(path.toPath())
                            // ... stream opening each class file
                            .entries { _, _ -> true }
                    }
                    // ... flatten class files from all inputs
                    .stream().flatMap { it },
                dexOutput = outputDir.asFile.get().toPath(),
                globalSyntheticsOutput = null,
            )
        } finally {
            bootClasspath.close()
            classpath.close()
        }
    }
}
