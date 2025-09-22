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

package com.aliucord.gradle.transformers

import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.MultiDexFileReader
import org.gradle.api.artifacts.transform.*
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider

/**
 * Artifact transformer to convert `apk` artifact types into `jar` types using Dex2Jar.
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
