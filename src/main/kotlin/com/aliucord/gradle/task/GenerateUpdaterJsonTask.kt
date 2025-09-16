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

import com.aliucord.gradle.entities.UpdateInfo
import com.aliucord.gradle.findAliucord
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

public abstract class GenerateUpdaterJsonTask : DefaultTask() {
    @get:OutputFile
    @get:PathSensitive(PathSensitivity.NONE)
    public abstract val outputFile: RegularFileProperty

    @TaskAction
    public fun generateUpdaterJson() {
        val map = HashMap<String, UpdateInfo>()

        for (subproject in project.allprojects) {
            val aliucord = subproject.extensions.findAliucord() ?: continue

            if (!aliucord.deploy.get()) {
                continue
            }

            map[subproject.name] = UpdateInfo(
                minimumDiscordVersion = aliucord.minimumDiscordVersion.get(),
                version = subproject.version.toString(),
                build = aliucord.buildUrl.orNull,
                changelog = aliucord.changelog.orNull,
                changelogMedia = aliucord.changelogMedia.orNull,
                hidden = aliucord.deployHidden.orNull,
            )
        }

        outputFile.asFile.get().writeText(Json.encodeToString(map))
    }
}
