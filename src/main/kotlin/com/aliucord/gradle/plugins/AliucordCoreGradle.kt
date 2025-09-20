package com.aliucord.gradle.plugins

import com.aliucord.gradle.Constants
import com.aliucord.gradle.task.DeployWithAdbTask
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression

/**
 * The Gradle plugin used to build Aliucord's core subproject.
 * ID: `com.aliucord.core`
 */
@Suppress("unused")
public abstract class AliucordCoreGradle : AliucordBaseGradle() {
    override fun apply(target: Project) {
        registerTasks(target)
        registerDex2jarTransformer(target)
    }

    protected fun registerTasks(project: Project) {
        // Compilation
        val compileDexTask = registerCompileDexTask(project)
        val compileResourcesTask = registerCompileResourcesTask(project)

        // Bundling
        val makeTask = project.tasks.register("make", Zip::class.java) {
            group = Constants.TASK_GROUP
            entryCompression = ZipEntryCompression.STORED
            isPreserveFileTimestamps = false
            archiveBaseName.set(project.name)
            archiveVersion.set("")
            destinationDirectory.set(project.layout.buildDirectory.dir("outputs"))

            val resourcesFile = compileResourcesTask.flatMap { it.outputFile }
            val resourcesFileTree = project.zipTree(resourcesFile)
            val resources = resourcesFile.map {
                if (it.asFile.exists()) {
                    resourcesFileTree
                } else {
                    emptyList()
                }
            }

            from(compileDexTask.map { it.outputs.files.singleFile })
            from(resources) {
                exclude("AndroidManifest.xml")
            }

            doLast {
                logger.lifecycle("Built Aliucord core at ${outputs.files.singleFile}")
            }
        }

        // Deployment
        project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
            group = Constants.TASK_GROUP
            deployType = "core"
            deployFile.fileProvider(makeTask.map { it.outputs.files.single() })
        }
    }
}
