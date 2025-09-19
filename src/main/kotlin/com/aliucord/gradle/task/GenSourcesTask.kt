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

import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.impl.NoOpCodeCache
import jadx.api.impl.SimpleCodeWriter
import jadx.plugins.input.dex.DexInputPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import java.util.function.Function
import java.util.zip.*

public abstract class GenSourcesTask : DefaultTask() {
    @get:InputFile
    public abstract val inputApk: RegularFileProperty

    @get:OutputFile
    public abstract val outputJar: RegularFileProperty

    @TaskAction
    public fun genSources() {
        val inputFile = inputApk.get().asFile
        val outputFile = outputJar.get().asFile
        val tmpFile = outputFile.resolveSibling(outputFile.nameWithoutExtension + ".tmp.jar")

        val args = JadxArgs().apply {
            setInputFile(inputFile)
            outDirSrc = tmpFile
            isSkipResources = true
            isShowInconsistentCode = true
            isRespectBytecodeAccModifiers = true
            isFsCaseSensitive = true
            isGenerateKotlinMetadata = true
            isDebugInfo = false
            isInlineAnonymousClasses = false
            isInlineMethods = false
            isReplaceConsts = false
            codeCache = NoOpCodeCache()
            codeWriterProvider = Function { SimpleCodeWriter(it) }
            threadsCount = Runtime.getRuntime()
                .availableProcessors()
                .minus(1)
                .coerceAtLeast(1)
        }

        JadxDecompiler(args).use { decompiler ->
            decompiler.registerPlugin(DexInputPlugin())
            decompiler.load()
            decompiler.save()
        }

        // Repack sources to be under src/main/java/*
        ZipInputStream(tmpFile.inputStream()).use { zip ->
            ZipOutputStream(outputFile.outputStream()).use { out ->
                var entry: ZipEntry? = null
                while (zip.nextEntry.also { entry = it } != null) {
                    if (entry!!.isDirectory) continue

                    val newEntry = ZipEntry("src/main/java/" + entry.name).apply {
                        method = ZipEntry.DEFLATED
                    }

                    out.putNextEntry(newEntry)
                    out.write(zip.readAllBytes())
                    out.closeEntry()
                }
            }
        }
        tmpFile.delete()
    }
}
