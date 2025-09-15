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

import com.aliucord.gradle.getAliucord
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.errors.MessageReceiverImpl
import com.android.build.gradle.options.SyncOptions.ErrorFormatMode
import com.android.builder.dexing.*
import com.android.builder.dexing.r8.ClassFileProviderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.getByName
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

abstract class CompileDexTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    val input: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:OutputFile
    abstract val pluginClassFile: RegularFileProperty

    @TaskAction
    fun compileDex() {
        val android = project.extensions.getByName<BaseExtension>("android")

        val dexOutputDir = outputFile.get().asFile.parentFile

        val bootClassPath = ClassFileProviderFactory(android.bootClasspath.map(File::toPath))
        val classPath = ClassFileProviderFactory(listOf<Path>())
        val dexBuilder = DexArchiveBuilder.createD8DexBuilder(
            DexParameters(
                minSdkVersion = android.defaultConfig.minSdkVersion?.apiLevel ?: 24,
                debuggable = true,
                dexPerClass = false,
                withDesugaring = true,
                desugarBootclasspath = bootClassPath,
                desugarClasspath = classPath,
                coreLibDesugarConfig = null,
                enableApiModeling = true,
                messageReceiver = MessageReceiverImpl(
                    ErrorFormatMode.HUMAN_READABLE,
                    LoggerFactory.getLogger(CompileDexTask::class.java)
                )
            )
        )

        try {
            val fileStreams = input.map { input ->
                ClassFileInputs.fromPath(input.toPath()).use { it.entries { _, _ -> true } }
            }.toTypedArray()

            Arrays.stream(fileStreams).flatMap { it }
                .use { classesInput ->
                    val files = classesInput.collect(Collectors.toList())

                    dexBuilder.convert(
                        files.stream(),
                        dexOutputDir.toPath(),
                        null
                    )

                    for (file in files) {
                        val reader = ClassReader(file.readAllBytes())
                        val classNode = ClassNode()

                        reader.accept(classNode, 0)

                        for (annotation in classNode.visibleAnnotations.orEmpty() + classNode.invisibleAnnotations.orEmpty()) {
                            if (annotation.desc == "Lcom/aliucord/annotations/AliucordPlugin;") {
                                val aliucord = project.extensions.getAliucord()

                                require(aliucord.pluginClassName == null) {
                                    "Only 1 active plugin class per project is supported"
                                }

                                for (method in classNode.methods) {
                                    if (method.name == "getManifest" && method.desc == "()Lcom/aliucord/entities/Plugin\$Manifest;") {
                                        throw IllegalArgumentException("Plugin class cannot override getManifest, use manifest.json system!")
                                    }
                                }

                                aliucord.pluginClassName = classNode.name.replace('/', '.')
                                    .also { pluginClassFile.asFile.orNull?.writeText(it) }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            logger.error("Failed to compile dex", e)
        } finally {
            bootClassPath.close()
            classPath.close()
        }

        logger.lifecycle("Compiled dex to ${outputFile.get()}")
    }
}
