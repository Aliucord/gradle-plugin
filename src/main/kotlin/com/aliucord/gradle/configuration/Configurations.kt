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

package com.aliucord.gradle.configuration

import org.gradle.api.Project

fun registerConfigurations(project: Project) {
    val providers = arrayOf(DiscordConfigurationProvider())

    for (provider in providers) {
        project.configurations.register(provider.name) {
            isTransitive = false
        }
    }

    project.afterEvaluate {
        for (provider in providers) {
            val configuration = project.configurations.getByName(provider.name)
            val dependencies = configuration.dependencies

            require(dependencies.size <= 1) {
                "Only one '${provider.name}' dependency should be specified, but ${dependencies.size} were!"
            }

            for (dependency in dependencies) {
                provider.provide(project, dependency)
            }
        }
    }
}