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
        val readers = inputs
            .asSequence()
            .filter { it.extension == "dex" }
            .map(::DexFileReader)
        // Get the root node and flatten all classes
        val classes = readers
            .map { reader -> DexFileNode().also { node -> reader.accept(node) } }
            .flatMap { it.clzs }

        // Find all classes annotated with @AliucordPlugin
        val pluginClasses = classes
            .filter { it.anns.any { it.type == "Lcom/aliucord/annotations/AliucordPlugin;" } }
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
            .replace('/', '.')

        this.pluginClass.get().asFile.writeText(pluginClassName)
    }
}
