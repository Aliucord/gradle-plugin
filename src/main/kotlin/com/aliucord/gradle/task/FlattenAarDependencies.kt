package com.aliucord.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import java.util.zip.ZipFile

public abstract class FlattenAarDependencies : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public abstract val dependencies: ConfigurableFileCollection

    @get:OutputDirectory
    public abstract val outputDir: DirectoryProperty

    @TaskAction
    public fun extract() {
        val outputDir = outputDir.get().asFile
            .apply { exists() && deleteRecursively() }
            .apply { mkdirs() }

        for (file in dependencies.files) {
            if (file.extension != "aar") continue

            ZipFile(file).use { zip ->
                val entry = zip.getEntry("classes.jar") ?: continue
                val jar = zip.getInputStream(entry).use { it.readBytes() }

                outputDir.resolve(file.nameWithoutExtension + ".jar")
                    .apply { exists() || createNewFile() }
                    .apply { writeBytes(jar) }
            }
        }
    }
}
