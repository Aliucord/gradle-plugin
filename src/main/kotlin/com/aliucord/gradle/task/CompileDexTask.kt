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
import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions.ErrorFormatMode
import com.android.builder.dexing.*
import com.android.builder.dexing.r8.ClassFileProviderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

/**
 * Converts compiled class files from jars to a single dex file. [input] may contain compiled jars, class files,
 * and compiled dependencies as well to be bundled into the dex.
 */
public abstract class CompileDexTask : DefaultTask() {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    public abstract val input: ConfigurableFileCollection

    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    private val minSdkVersion: Int
    private val bootClasspath: List<Path>

    init {
        val android = project.extensions.getAndroid()
        minSdkVersion = requireNotNull(android.defaultConfig.minSdkVersion?.apiLevel) {
            "minSdkVersion is required to compile to dex!"
        }
        bootClasspath = android.bootClasspath.map(File::toPath)
    }

    @TaskAction
    public fun compileDex() {
        val illegalDependencies = arrayOf("kotlin-stdlib", "material", "constraintlayout", "appcompat")
        val illegalDependency = input.asFileTree.find { f -> illegalDependencies.any { f.name.startsWith(it) } }
        if (illegalDependency != null)
            throw GradleException("${illegalDependency.name} is defined as an 'implementation' dependency! " +
                "It should be explicitly defined as a 'compileOnly' dependency! " +
                "Please read the Aliucord Gradle plugin v2 migration guide!")

        val bootClasspath = ClassFileProviderFactory(bootClasspath)
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
            outputDir.get().asFile.mkdirs()

            dexBuilder.convert(
                input = input.files
                    // For each input path...
                    .filter(File::exists)
                    .map { path ->
                        ClassFileInputs
                            // ... scan for class files
                            .fromPath(path.toPath())
                            // ... stream opening each class file
                            .entries { _, _ -> true }
                    }
                    // ... flatten class files from all inputs
                    .stream().flatMap { it },
                dexOutput = outputDir.get().asFile.toPath(),
                globalSyntheticsOutput = null,
            )
        } finally {
            bootClasspath.close()
            classpath.close()
        }
    }
}
