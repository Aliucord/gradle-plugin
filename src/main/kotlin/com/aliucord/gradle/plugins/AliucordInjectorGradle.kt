package com.aliucord.gradle.plugins

import com.aliucord.gradle.Constants
import com.aliucord.gradle.registerDiscordConfiguration
import com.aliucord.gradle.task.DeployWithAdbTask
import org.gradle.api.Project
import org.gradle.api.tasks.Copy

/**
 * The Gradle plugin used to configure Aliucord's Injector subproject.
 * ID: `com.aliucord.injector`
 */
@Suppress("unused")
public abstract class AliucordInjectorGradle : AliucordBaseGradle() {
    override fun apply(project: Project) {
        registerTasks(project)
        registerDiscordConfiguration(project)
    }

    protected fun registerTasks(project: Project) {
        registerDecompileTask(project)

        // Compilation
        val compileDexTask = registerCompileDexTask(project)

        // Bundling
        val makeTask = project.tasks.register("make", Copy::class.java) {
            group = Constants.TASK_GROUP
            from(compileDexTask.map { it.outputs.files.singleFile })
            into(project.layout.buildDirectory)
            rename { return@rename "Injector.dex" }
        }

        // Deployment
        project.tasks.register("deployWithAdb", DeployWithAdbTask::class.java) {
            group = Constants.TASK_GROUP
            dependsOn(makeTask)
        }
    }
}
