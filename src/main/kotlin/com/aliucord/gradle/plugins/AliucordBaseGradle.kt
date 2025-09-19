package com.aliucord.gradle.plugins

import com.aliucord.gradle.*
import com.aliucord.gradle.task.*
import com.android.build.gradle.tasks.ProcessLibraryManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

public abstract class AliucordBaseGradle : Plugin<Project> {
    public fun registerDecompileTask(project: Project): TaskProvider<GenSourcesTask> {
        return project.tasks.register("decompileDiscord", GenSourcesTask::class.java) {
            group = Constants.TASK_GROUP

            val discordConfiguration = project.configurations.getDiscord()
            val discordVersion = discordConfiguration.dependencies.single().version

            // This is ugly, but since resolving Discord as a dependency is a hacky workaround,
            // this is the only way I can figure out how that contains the chaos.
            val discordCache = project.gradle.gradleUserHomeDir.resolve("caches/aliucord/discord")
            inputApk.set(discordCache.resolve("discord-$discordVersion.apk"))
            outputJar.set(discordCache.resolve("discord-$discordVersion-sources.jar"))
        }
    }

    @Suppress("UnstableApiUsage")
    public fun registerCompileDexTask(project: Project): TaskProvider<CompileDexTask> {
        val intermediates = project.layout.buildDirectory.dir("intermediates")

        // Since the `implementation` is non-resolvable, wrap it in another configuration
        val implementationArtifacts = project.configurations.register("implementationArtifacts") {
            isCanBeResolved = true // Allow resolving artifacts
            isCanBeConsumed = false // Limited to this project
            isCanBeDeclared = false // No new artifacts can be added
            extendsFrom(project.configurations.getByName("implementation"))
        }

        val flattenDependenciesTask = project.tasks.register(
            "flattenAarDependencies",
            FlattenAarDependencies::class.java,
        ) {
            group = Constants.TASK_GROUP_INTERNAL
            outputDir.set(intermediates.map { it.dir("dex_dependencies") })
            dependencies.from(implementationArtifacts.map { configuration ->
                configuration.incoming.files
                    .filter { it.extension == "aar" }
            })
        }

        val compileDexTask = project.tasks.register("compileDex", CompileDexTask::class.java) {
            group = Constants.TASK_GROUP_INTERNAL
            outputDir.set(intermediates.map { it.dir("dex") })

            input.from(flattenDependenciesTask)
            input.from(implementationArtifacts.map { configuration ->
                configuration.incoming.files
                    .filter { it.extension == "jar" }
            })

            for (task in arrayOf("compileDebugJavaWithJavac", "compileDebugKotlin"))
                input.from(project.tasks.named(task))
        }

        return compileDexTask
    }

    public fun registerCompileResourcesTask(project: Project): TaskProvider<CompileResourcesTask> {
        val intermediates = project.layout.buildDirectory.dir("intermediates")

        return project.tasks.register("compileResources", CompileResourcesTask::class.java) {
            val android = project.extensions.getAndroid()
            val processManifestTask = project.tasks.named("processDebugManifest", ProcessLibraryManifest::class.java)

            group = Constants.TASK_GROUP_INTERNAL
            dependsOn(processManifestTask)

            input.set(android.sourceSets.getByName("main").res.srcDirs.single())
            manifestFile.set(processManifestTask.flatMap { it.manifestOutputFile })
            outputFile.set(intermediates.map { it.file("res.apk") })
        }
    }
}
