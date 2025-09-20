package com.aliucord.gradle.transformers

import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.MultiDexFileReader
import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider

/**
 * Artifact transformer to convert "apk" artifact types into "jar" types using `dex2jar`.
 */
public abstract class Dex2JarTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    public abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val inputFile = inputArtifact.get().asFile
        val outputFile = outputs.file("jars/" + inputFile.nameWithoutExtension + ".jar")

        Dex2jar.from(MultiDexFileReader.open(inputFile.readBytes()))
            .skipDebug(false)
            .topoLogicalSort()
            .noCode(false)
            .to(outputFile.toPath())
    }
}
