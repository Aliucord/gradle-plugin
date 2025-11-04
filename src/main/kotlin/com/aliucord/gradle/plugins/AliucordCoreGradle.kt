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
import com.aliucord.gradle.task.AlignTask
import com.aliucord.gradle.task.adb.DeployPrebuiltTask
import com.aliucord.gradle.task.adb.RestartAliucordTask
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.kotlin.dsl.register

/**
 * The Gradle plugin used to build Aliucord's core subproject.
 * ID: `com.aliucord.core`
 */
@Suppress("unused")
public abstract class AliucordCoreGradle : AliucordBaseGradle() {
    override fun apply(target: Project) {
        registerTasks(target)
        registerDex2jarTransformer(target)
        deleteLegacyCache(target)
    }

    protected fun registerTasks(project: Project) {
        // Compilation
        val compileDexTask = registerCompileDexTask(project)
        val compileResourcesTask = registerCompileResourcesTask(project)

        // Bundling
        val packageTask = project.tasks.register<Zip>("package") {
            group = Constants.TASK_GROUP_INTERNAL
            entryCompression = ZipEntryCompression.STORED
            isPreserveFileTimestamps = false
            archiveBaseName.set(project.name + "-unaligned")
            archiveVersion.set("")
            destinationDirectory.set(project.layout.buildDirectory.dir("intermediates"))

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
        }

        val makeTask = project.tasks.register<AlignTask>("make") {
            group = Constants.TASK_GROUP
            inputZip.fileProvider(packageTask.map { it.outputs.files.singleFile })
            outputZip.set(project.layout.buildDirectory.file("outputs/${project.name}.zip"))

            doLast {
                logger.lifecycle("Built Aliucord core at ${outputs.files.singleFile}")
            }
        }

        // Deployment
        val restartAliucordTask = project.tasks.register<RestartAliucordTask>("restartAliucord") {
            group = Constants.TASK_GROUP
        }

        project.tasks.register<DeployPrebuiltTask>("deployWithAdb") {
            group = Constants.TASK_GROUP
            deployType = DeployPrebuiltTask.DeployType.Core
            deployFile.fileProvider(makeTask.map { it.outputs.files.singleFile })
            finalizedBy(restartAliucordTask)
        }
    }
}
