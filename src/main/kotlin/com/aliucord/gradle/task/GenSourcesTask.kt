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

public abstract class GenSourcesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val inputAPK: RegularFileProperty

    @get:OutputFile
    public abstract val outputJar: RegularFileProperty

    @TaskAction
    public fun genSources() {
        val inputFile = inputAPK.get().asFile
        val outputFile = inputFile.resolveSibling(inputFile.nameWithoutExtension + "-sources.jar")

        outputJar.set(outputFile)

        val args = JadxArgs().apply {
            setInputFile(inputFile)
            outDirSrc = outputFile
            isSkipResources = true
            isShowInconsistentCode = true
            isRespectBytecodeAccModifiers = true
            isFsCaseSensitive = true
            isGenerateKotlinMetadata = false // Aliucord JADX specific, omit Kotlin @Metadata
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
    }
}
