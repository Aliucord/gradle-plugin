package com.aliucord.gradle.plugins

import com.aliucord.gradle.*
import com.aliucord.gradle.models.Links
import com.aliucord.gradle.models.PluginManifest
import com.aliucord.gradle.task.*
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression

/**
 * The Gradle plugin used to build Aliucord plugins.
 * ID: `com.aliucord.plugin`
 */
@Suppress("unused")
public abstract class AliucordPluginGradle : AliucordBaseGradle() {
    override fun apply(project: Project) {
        if (project == project.rootProject) {
            registerRootTasks(project)
        } else {
            project.extensions.create("aliucord", AliucordExtension::class.java, project)
            registerTasks(project)
            registerDiscordConfiguration(project)
        }
    }

    protected fun registerRootTasks(rootProject: Project) {
        rootProject.tasks.register("generateUpdaterJson", GenerateUpdaterJsonTask::class.java) {
            val plugins = rootProject.allprojects
                .filter { it.extensions.findAliucord() != null }
                .map { project ->
                    project.objects.newInstance(GenerateUpdaterJsonTask.PluginInfo::class.java).apply {
                        name.set(project.provider { project.name })
                        version.set(project.provider { project.version.toString() })

                        val aliucord = project.extensions.getAliucord()
                        deploy.set(aliucord.deploy)
                        deployHidden.set(aliucord.deployHidden)
                        changelog.set(aliucord.changelog)
                        changelogMedia.set(aliucord.changelogMedia)
                        buildUrl.set(aliucord.buildUrl)
                        minimumDiscordVersion.set(aliucord.minimumDiscordVersion)
                    }
                }

            group = Constants.TASK_GROUP_INTERNAL
            outputFile.set(rootProject.layout.buildDirectory.file("updater.json"))
            pluginConfigs.set(plugins)
        }
    }

    protected fun registerTasks(project: Project) {
        val extension = project.extensions.getAliucord()
        val intermediates = project.layout.buildDirectory.dir("intermediates")

        registerDecompileTask(project)

        // Compilation
        val compileDexTask = registerCompileDexTask(project)
        val compileResourcesTask = registerCompileResourcesTask(project)

        // Bundling
        val extractPluginClassTask = project.tasks.register("extractPluginClass", ExtractPluginClassTask::class.java) {
            group = Constants.TASK_GROUP_INTERNAL
            dependsOn(compileDexTask)

            this.inputs.setFrom(compileDexTask.map { it.outputs.files.singleFile })
            this.pluginClass.set(intermediates.map { it.file("pluginClass.txt") })
        }

        val makeTask = project.tasks.register("make", Zip::class.java) {
            group = Constants.TASK_GROUP
            entryCompression = ZipEntryCompression.STORED
            isPreserveFileTimestamps = false
            archiveBaseName.set(project.name)
            archiveVersion.set("")
            destinationDirectory.set(project.layout.buildDirectory)

            val manifestFile = intermediates.map { it.file("manifest.json") }
            val pluginClassNameFile = extractPluginClassTask.flatMap { it.pluginClass }
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

            // Write manifest to be zipped
            val manifest = PluginManifest(
                pluginClassName = "PLACEHOLDER",
                name = project.name,
                version = project.version.toString(),
                description = project.description,
                authors = extension.authors.get(),
                links = Links(
                    github = extension.githubUrl.orNull,
                    source = extension.sourceUrl.orNull,
                ),
                updateUrl = extension.updateUrl.orNull,
                changelog = extension.changelog.orNull,
                changelogMedia = extension.changelogMedia.orNull,
            )
            doFirst {
                val newManifest = manifest.copy(
                    pluginClassName = pluginClassNameFile.get().asFile.readText(),
                )
                manifestFile.get().asFile.writeText(Json.encodeToString(newManifest))
            }

            doLast {
                logger.lifecycle("Built plugin at ${outputs.files.singleFile}")
            }
        }

        // Deployment
        project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
            group = Constants.TASK_GROUP
            deployType = "plugin"
            deployFile.fileProvider(makeTask.map { it.outputs.files.single() })
        }
    }
}
