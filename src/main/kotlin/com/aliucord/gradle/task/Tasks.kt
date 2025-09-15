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

import com.aliucord.gradle.ProjectType
import com.aliucord.gradle.entities.PluginManifest
import com.aliucord.gradle.getAliucord
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.tasks.ProcessLibraryManifest
import groovy.json.JsonBuilder
import org.gradle.api.Project
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

const val TASK_GROUP = "aliucord"

fun registerTasks(project: Project) {
    val extension = project.extensions.getAliucord()
    val intermediates = project.layout.buildDirectory.dir("intermediates").get()

    if (project.rootProject.tasks.findByName("generateUpdaterJson") == null) {
        project.rootProject.tasks.register("generateUpdaterJson", GenerateUpdaterJsonTask::class.java) {
            group = TASK_GROUP

            outputs.upToDateWhen { false }

            outputFile.set(project.layout.buildDirectory.file("updater.json"))
        }
    }

    project.tasks.register("genSources", GenSourcesTask::class.java) {
        group = TASK_GROUP
    }

    val pluginClassFile = intermediates.file("pluginClass").asFile

    val compileDex = project.tasks.register("compileDex", CompileDexTask::class.java) {
        group = TASK_GROUP

        this.pluginClassFile.set(pluginClassFile)

        for (name in arrayOf("compileDebugJavaWithJavac", "compileDebugKotlin")) {
            val task = project.tasks.findByName(name)
            if (task != null) {
                dependsOn(task)
                input.from(task.outputs)
            }
        }

        outputFile.set(intermediates.file("classes.dex"))
    }

    val compileResources = project.tasks.register("compileResources", CompileResourcesTask::class.java) {
        group = TASK_GROUP

        val processManifestTask = project.tasks.getByName("processDebugManifest") as ProcessLibraryManifest
        dependsOn(processManifestTask)

        val android = project.extensions.getByName("android") as BaseExtension
        input.set(android.sourceSets.getByName("main").res.srcDirs.single())
        manifestFile.set(processManifestTask.manifestOutputFile)

        outputFile.set(intermediates.file("res.apk"))

        doLast {
            val resApkFile = outputFile.asFile.get()

            if (resApkFile.exists()) {
                project.tasks.named("make", AbstractCopyTask::class.java) {
                    from(project.zipTree(resApkFile)) {
                        exclude("AndroidManifest.xml")
                    }
                }
            }
        }
    }

    project.afterEvaluate {
        project.tasks.register(
            "make",
            if (extension.projectType.get() == ProjectType.INJECTOR) Copy::class.java else Zip::class.java
        ) {
            group = TASK_GROUP
            dependsOn(compileDex)

            if (extension.projectType.get() == ProjectType.PLUGIN) {
                val manifestFile = intermediates.file("manifest.json").asFile

                from(manifestFile)
                doFirst {
                    require(project.version != "unspecified") {
                        "No version is set"
                    }

                    if (extension.pluginClassName == null) {
                        if (pluginClassFile.exists()) {
                            extension.pluginClassName = pluginClassFile.readText()
                        }
                    }

                    require(extension.pluginClassName != null) {
                        "No plugin class found, make sure your plugin class is annotated with @AliucordPlugin"
                    }

                    manifestFile.writeText(
                        JsonBuilder(
                            PluginManifest(
                                pluginClassName = extension.pluginClassName!!,
                                name = project.name,
                                version = project.version.toString(),
                                description = project.description,
                                authors = extension.authors.get(),
                                links = extension.links,
                                updateUrl = extension.updateUrl.orNull,
                                changelog = extension.changelog.orNull,
                                changelogMedia = extension.changelogMedia.orNull
                            )
                        ).toPrettyString()
                    )
                }
            }

            from(compileDex.map { it.outputFile })

            if (extension.projectType.get() == ProjectType.INJECTOR) {
                into(project.layout.buildDirectory)
                rename { return@rename "Injector.dex" }

                doLast {
                    logger.lifecycle("Copied Injector.dex to ${project.layout.buildDirectory}")
                }
            } else {
                val zip = this as Zip
                zip.dependsOn(compileResources.get())
                zip.isPreserveFileTimestamps = false
                zip.archiveBaseName.set(project.name)
                zip.archiveVersion.set("")
                zip.destinationDirectory.set(project.layout.buildDirectory)

                doLast {
                    logger.lifecycle("Made Aliucord package at ${outputs.files.singleFile}")
                }
            }
        }

        project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
            group = TASK_GROUP
            dependsOn("make")
        }
    }
}
