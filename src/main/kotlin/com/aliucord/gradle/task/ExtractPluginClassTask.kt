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

import com.googlecode.d2j.node.DexFileNode
import com.googlecode.d2j.reader.DexFileReader
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Parses the compiled dex output from [CompileDexTask] and extracts the class name
 * of a single class that was annotated with `@AliucordPlugin`.
 */
public abstract class ExtractPluginClassTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    public val inputs: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputFile
    public abstract val pluginClass: RegularFileProperty

    @TaskAction
    public fun extract() {
        // Open a reader for all dex files from the input
        val readers = inputs.asFileTree
            .asSequence()
            .filter { it.extension == "dex" }
            .map(::DexFileReader)

        // Get the root node and flatten all classes
        val readerFlags = DexFileReader.SKIP_CODE or
            DexFileReader.SKIP_DEBUG or
            DexFileReader.SKIP_EXCEPTION or
            DexFileReader.SKIP_FIELD_CONSTANT
        val classes = readers
            .map { reader -> DexFileNode().also { node -> reader.accept(node, readerFlags) } }
            .flatMap { it.clzs }

        // Find all classes annotated with @AliucordPlugin
        val pluginClasses = classes
            .filter { cls -> cls.anns?.any { ann -> ann.type == "Lcom/aliucord/annotations/AliucordPlugin;" } == true }
            .toList()

        require(pluginClasses.isNotEmpty()) {
            "No classes were found annotated with @AliucordPlugin! " +
                "An Aliucord plugin should have exactly one entrypoint class annotated with @AliucordPlugin"
        }
        require(pluginClasses.size == 1) {
            """
                More than one class was found annotated with @AliucordPlugin!
                An Aliucord plugin should have exactly one entrypoint class annotated with @AliucordPlugin.
                Found classes:

                ${pluginClasses.joinToString(separator = "\n") { it.className }}
            """.trimIndent()
        }

        val hasManifestOverride = pluginClasses.single()
            .methods
            .any {
                it.method.name == "getManifest" &&
                    it.method.desc == $$"()Lcom/aliucord/entities/Plugin$Manifest;"
            }
        require(!hasManifestOverride) { "Plugins cannot override getManifest()!" }

        val pluginClassName = pluginClasses.single()
            .className
            .removeSurrounding("L", ";")
            .replace('/', '.')

        this.pluginClass.get().asFile.writeText(pluginClassName)
    }
}
