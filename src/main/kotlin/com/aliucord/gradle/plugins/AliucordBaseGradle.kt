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

package com.aliucord.gradle.plugins

import com.aliucord.gradle.Constants
import com.aliucord.gradle.getAndroid
import com.aliucord.gradle.task.CompileDexTask
import com.aliucord.gradle.task.CompileResourcesTask
import com.aliucord.gradle.transformers.Dex2JarTransform
import com.android.build.gradle.tasks.ProcessLibraryManifest
import org.gradle.api.*
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A base class for [AliucordCoreGradle], [AliucordInjectorGradle], and [AliucordPluginGradle]
 * containing shared project registration code.
 */
public abstract class AliucordBaseGradle : Plugin<Project> {
    private companion object {
        var legacyCacheDeleted = AtomicBoolean(false)
    }

    /**
     * Deletes the old Discord cache that lived under
     * `~/.gradle/caches/aliucord/discord/discord-{version}.{apk,jar}`
     * which was used by this Gradle plugin prior to v2. These files are not
     * tracked by Gradle's cache garbage collector so we have to handle it manually.
     */
    protected fun deleteLegacyCache(project: Project) {
        if (legacyCacheDeleted.getAndSet(true)) return

        project.gradle.gradleUserHomeDir
            .resolve("caches/aliucord")
            .deleteRecursively()
    }

    protected fun registerDex2jarTransformer(project: Project) {
        // Register a transform to convert "apk" artifact types to "jar"
        project.dependencies {
            registerTransform(Dex2JarTransform::class) {
                from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "apk")
                to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
            }
        }
    }

    @Suppress("UnstableApiUsage")
    protected fun registerCompileDexTask(project: Project): TaskProvider<CompileDexTask> {
        val intermediates = project.layout.buildDirectory.dir("intermediates")

        // Since the `implementation` is non-resolvable, wrap it in another configuration
        val implementationArtifacts = project.configurations.register("implementationArtifacts") {
            isCanBeResolved = true // Allow resolving artifacts
            isCanBeConsumed = false // Limited to this project
            isCanBeDeclared = false // No new artifacts can be added
            extendsFrom(project.configurations.getByName("implementation"))
        }

        val compileDexTask = project.tasks.register<CompileDexTask>("compileDex") {
            group = Constants.TASK_GROUP_INTERNAL
            outputDir.set(intermediates.map { it.dir("dex") })

            // Collect all dependencies as jars
            // `.aar` will have artifact transformers applied to extract their inner `classes.jar`
            input.from(implementationArtifacts.map { configuration ->
                configuration.incoming
                    .artifactView {
                        attributes.attribute(
                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                            ArtifactTypeDefinition.JAR_TYPE)
                    }
                    .files
            })


            input.from(project.tasks.named("compileDebugJavaWithJavac"))
            input.from(try {
                project.tasks.named("compileDebugKotlin")
            } catch (_: UnknownDomainObjectException) {
                null
            })
        }

        return compileDexTask
    }

    protected fun registerCompileResourcesTask(project: Project): TaskProvider<CompileResourcesTask> {
        val intermediates = project.layout.buildDirectory.dir("intermediates")

        return project.tasks.register<CompileResourcesTask>("compileResources") {
            val android = project.extensions.getAndroid()
            val processManifestTask = project.tasks.named<ProcessLibraryManifest>("processDebugManifest")

            group = Constants.TASK_GROUP_INTERNAL
            input.set(android.sourceSets.getByName("main").res.srcDirs.single())
            manifestFile.set(processManifestTask.flatMap { it.manifestOutputFile })
            outputFile.set(intermediates.map { it.file("res.apk") })
        }
    }
}
