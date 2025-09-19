package com.aliucord.gradle.plugins

import com.aliucord.gradle.*
import com.aliucord.gradle.entities.Links
import com.aliucord.gradle.entities.PluginManifest
import com.aliucord.gradle.task.*
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

/**
 * The Gradle plugin used to build Aliucord plugins.
 * ID: `com.aliucord.plugin`
 */
@Suppress("unused")
public abstract class AliucordPluginGradle : AliucordBaseGradle() {
    override fun apply(project: Project) {
        project.extensions.create("aliucord", AliucordExtension::class.java, project)
        registerTasks(project)
        registerDiscordConfiguration(project)
    }

    protected fun registerTasks(project: Project) {
        val extension = project.extensions.getAliucord()
        val intermediates = project.layout.buildDirectory.dir("intermediates")

        // Plugin updater generation
        if (project.rootProject.tasks.findByName("generateUpdaterJson") == null) {
            project.rootProject.tasks.register("generateUpdaterJson", GenerateUpdaterJsonTask::class.java) {
                group = Constants.TASK_GROUP
                outputFile.set(project.layout.buildDirectory.file("updater.json"))
            }
        }

        registerDecompileTask(project)

        // Compilation
        val compileDexTask = registerCompileDexTask(project)
        val compileResourcesTask = registerCompileResourcesTask(project)

        // Bundling
        val extractPluginClassTask = project.tasks.register("extractPluginClass", ExtractPluginClassTask::class.java) {
            group = Constants.TASK_GROUP_INTERNAL
            dependsOn(compileDexTask)

            this.inputs.setFrom(compileDexTask.map { it.outputs })
            this.pluginClass.set(intermediates.map { it.file("pluginClass.txt") })
        }

        val makeTask = project.tasks.register("make", Zip::class.java) {
            group = Constants.TASK_GROUP
            isPreserveFileTimestamps = false
            archiveBaseName.set(project.name)
            archiveVersion.set("")
            destinationDirectory.set(project.layout.buildDirectory)

            val manifestFile = intermediates.map { it.file("manifest.json") }
            val pluginClassNameFile = extractPluginClassTask.flatMap { it.pluginClass }
            val resourcesFile = compileResourcesTask.flatMap { it.outputFile }
            val resourcesFileTree = resourcesFile.map {
                if (it.asFile.exists()) {
                    project.zipTree(it)
                } else {
                    emptyList()
                }
            }

            dependsOn(pluginClassNameFile)
            from(manifestFile)
            from(compileDexTask.map { it.outputs.files.singleFile })
            from(resourcesFileTree) {
                exclude("AndroidManifest.xml")
            }

            require(project.version != "unspecified") {
                "No project version is set! A version is required to package an Aliucord plugin."
            }

            // Write manifest
            doFirst {
                val manifest = PluginManifest(
                    pluginClassName = pluginClassNameFile.get().asFile.readText(),
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
                    changelogMedia = extension.changelogMedia.orNull
                )
                manifestFile.get().asFile.writeText(Json.encodeToString(manifest))
            }

            doLast {
                logger.lifecycle("Built plugin at ${outputs.files.singleFile}")
            }
        }

        // Deployment
        project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
            group = Constants.TASK_GROUP
            dependsOn(makeTask)
        }
    }
}
