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
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import jadx.api.impl.NoOpCodeCache
import jadx.api.impl.SimpleCodeWriter
import jadx.plugins.input.dex.DexInputPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.util.function.Function

public abstract class GenSourcesTask : DefaultTask() {
    @TaskAction
    public fun genSources() {
        val extension = project.extensions.getAliucord()
        val discord = extension.discord!!

        val sourcesJarFile = discord.cache.resolve("discord-${discord.version}-sources.jar")

        val args = JadxArgs()
        args.setInputFile(discord.apkFile)
        args.outDirSrc = sourcesJarFile
        args.isSkipResources = true
        args.isShowInconsistentCode = true
        args.isRespectBytecodeAccModifiers = true
        args.isFsCaseSensitive = true
        args.isGenerateKotlinMetadata = false
        args.isDebugInfo = false
        args.isInlineAnonymousClasses = false
        args.isInlineMethods = false
        args.isReplaceConsts = false

        args.codeCache = NoOpCodeCache()
        args.codeWriterProvider = Function { SimpleCodeWriter(it) }

        JadxDecompiler(args).use { decompiler ->
            decompiler.registerPlugin(DexInputPlugin())
            decompiler.load()
            decompiler.save()
        }
    }
}
