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

import com.aliucord.gradle.*
import com.aliucord.gradle.models.PluginManifest
import com.aliucord.gradle.task.ExtractPluginClassTask
import com.aliucord.gradle.task.GenerateUpdaterJsonTask
import com.aliucord.gradle.task.adb.DeployPrebuiltTask
import com.aliucord.gradle.task.adb.RestartAliucordTask
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.kotlin.dsl.*

/**
 * The Gradle plugin used to build Aliucord plugins.
 * ID: `com.aliucord.plugin`
 */
@Suppress("unused")
public abstract class AliucordPluginGradle : AliucordBaseGradle() {
    override fun apply(target: Project) {
        if (target == target.rootProject) {
            registerRootTasks(target)
        } else {
            target.extensions.create<AliucordExtension>("aliucord")
            registerTasks(target)
            registerDex2jarTransformer(target)
        }
        deleteLegacyCache(target)
    }

    protected fun registerRootTasks(rootProject: Project) {
        rootProject.tasks.register<GenerateUpdaterJsonTask>("generateUpdaterJson") {
            val plugins = rootProject.allprojects
                .filter { it.extensions.findAliucord() != null }
                .map { project ->
                    val aliucord = project.extensions.getAliucord()
                    val android = project.extensions.getAndroid()
                    val compileOnlyConfiguration = project.configurations.getByName("compileOnly")

                    // Retrieve various dependency versions that this plugin is built with
                    val discordDependency = compileOnlyConfiguration.dependencies
                        .find { it.group == "com.discord" && it.name == "discord" }
                    val kotlinDependency = compileOnlyConfiguration.dependencies
                        .find { it.group == "org.jetbrains.kotlin" && it.name == "kotlin-stdlib" }
                    val aliucordDependency = compileOnlyConfiguration.dependencies
                        .find { it.group == "com.aliucord" && it.name == "Aliucord" }

                    project.objects.newInstance<GenerateUpdaterJsonTask.PluginInfo>().apply {
                        name.set(project.provider { project.name })
                        version.set(project.provider { project.version.toString() })
                        deploy.set(aliucord.deploy)
                        deployHidden.set(aliucord.deployHidden)
                        changelog.set(aliucord.changelog)
                        changelogMedia.set(aliucord.changelogMedia)
                        buildUrl.set(aliucord.buildUrl)
                        minimumDiscordVersion.set(aliucord.minimumDiscordVersion
                            // TODO: this may fail when the version is a wildcard
                            .orElse(project.provider { discordDependency?.version?.toInt() }))
                        minimumAliucordVersion.set(aliucordDependency?.version)
                        // If this is null, an earlier task will fail
                        minimumApiLevel.set(android.defaultConfig.minSdkVersion?.apiLevel)
                        minimumKotlinVersion.set(kotlinDependency?.version)
                        buildFile.fileProvider(project.tasks.named("make")
                            .map { it.outputs.files.singleFile })
                    }
                }

            group = Constants.TASK_GROUP
            outputFile.set(rootProject.layout.buildDirectory.file("outputs/updater.json"))
            pluginConfigs.set(plugins)
        }
    }

    protected fun registerTasks(project: Project) {
        val extension = project.extensions.getAliucord()
        val intermediates = project.layout.buildDirectory.dir("intermediates")

        // Compilation
        val compileDexTask = registerCompileDexTask(project)
        val compileResourcesTask = registerCompileResourcesTask(project)

        // Bundling
        val extractPluginClassTask = project.tasks.register<ExtractPluginClassTask>("extractPluginClass") {
            group = Constants.TASK_GROUP_INTERNAL

            this.inputs.setFrom(compileDexTask.map { it.outputs.files.singleFile })
            this.pluginClassNameFile.set(intermediates.map { it.file("pluginClass.txt") })
        }

        val makeTask = project.tasks.register<Zip>("make") {
            group = Constants.TASK_GROUP
            entryCompression = ZipEntryCompression.STORED
            isPreserveFileTimestamps = false
            archiveBaseName.set(project.name)
            archiveVersion.set("")
            destinationDirectory.set(project.layout.buildDirectory.dir("outputs"))

            val manifestFile = intermediates.map { it.file("manifest.json") }
            val pluginClassNameFile = extractPluginClassTask.flatMap { it.pluginClassNameFile }
            val resourcesFile = compileResourcesTask.flatMap { it.outputFile }
            val resourcesFileTree = project.zipTree(resourcesFile)
            val resources = resourcesFile.map {
                if (it.asFile.exists()) {
                    resourcesFileTree
                } else {
                    emptyList()
                }
            }

            from(manifestFile)
            from(compileDexTask.map { it.outputs.files.singleFile })
            from(resources) {
                exclude("AndroidManifest.xml")
            }
            dependsOn(pluginClassNameFile)

            val aliucord = project.extensions.getAliucord()
            val android = project.extensions.getAndroid()
            val compileOnlyConfiguration = project.configurations.getByName("compileOnly")

            // Retrieve various dependency versions that this plugin is built with
            val discordDependency = compileOnlyConfiguration.dependencies
                .find { it.group == "com.discord" && it.name == "discord" }
            val kotlinDependency = compileOnlyConfiguration.dependencies
                .find { it.group == "org.jetbrains.kotlin" && it.name == "kotlin-stdlib" }
            val aliucordDependency = compileOnlyConfiguration.dependencies
                .find { it.group == "com.aliucord" && it.name == "Aliucord" }
            val minimumDiscordVersion = aliucord.minimumDiscordVersion
                // TODO: this may fail when the version is a wildcard
                .orElse(project.provider { discordDependency?.version?.toInt() })

            // Write manifest to be zipped
            val manifest = PluginManifest(
                pluginClassName = "PLACEHOLDER",
                name = project.name,
                version = project.version.toString(),
                description = project.description,
                authors = extension.authors.get(),
                links = PluginManifest.Links(
                    github = extension.githubUrl.orNull,
                    source = extension.sourceUrl.orNull,
                ),
                updateUrl = extension.updateUrl.orNull,
                changelog = extension.changelog.orNull,
                changelogMedia = extension.changelogMedia.orNull,
                minimumAliucordVersion = aliucordDependency?.version,
                minimumKotlinVersion = kotlinDependency?.version,
                minimumApiLevel = android.defaultConfig.minSdkVersion?.apiLevel,
            )
            doFirst {
                val newManifest = manifest.copy(
                    pluginClassName = pluginClassNameFile.get().asFile.readText(),
                    minimumDiscordVersion = minimumDiscordVersion.get(),
                )
                manifestFile.get().asFile.writeText(Json.encodeToString(newManifest))
            }

            doLast {
                logger.lifecycle("Built plugin at ${outputs.files.singleFile}")
            }
        }

        // Deployment
        val restartAliucordTask = project.tasks.register<RestartAliucordTask>("restartAliucord") {
            group = Constants.TASK_GROUP
        }

        project.tasks.register<DeployPrebuiltTask>("deployWithAdb") {
            group = Constants.TASK_GROUP
            deployType = DeployPrebuiltTask.DeployType.Plugin
            deployFile.fileProvider(makeTask.map { it.outputs.files.single() })
            finalizedBy(restartAliucordTask)
        }
    }
}
