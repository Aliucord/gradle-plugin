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
import com.aliucord.gradle.task.adb.DeployComponentTask
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

/**
 * The Gradle plugin used to configure Aliucord's Injector subproject.
 * ID: `com.aliucord.injector`
 */
@Suppress("unused")
public abstract class AliucordInjectorGradle : AliucordBaseGradle() {
    override fun apply(target: Project) {
        registerTasks(target)
        registerDex2jarTransformer(target)
        deleteLegacyCache(target)
    }

    protected fun registerTasks(project: Project) {
        // Compilation
        val compileDexTask = registerCompileDexTask(project)

        // Bundling
        project.tasks.register<Copy>("make") {
            group = Constants.TASK_GROUP
            from(compileDexTask.map { it.outputs.files.singleFile })
            into(project.layout.buildDirectory.dir("outputs"))
            rename { "Injector.dex" }
        }

        // Deployment
        project.tasks.register<DeployComponentTask>("deployWithAdb") {
            group = Constants.TASK_GROUP
            componentType = "injector"
            componentFile.fileProvider(compileDexTask.map { it.outputs.files.asFileTree.singleFile })
            componentVersion = project.version.toString()
        }
    }
}
