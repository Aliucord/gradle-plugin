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

import com.aliucord.gradle.models.UpdateInfo
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.util.zip.CRC32

/**
 * Generates the `updater.json` containing metadata about the most recent build of all plugins.
 * This is used by Aliucord to determine whether there is a compatible plugin update to install.
 */
public abstract class GenerateUpdaterJsonTask : DefaultTask() {
    @get:OutputFile
    public abstract val outputFile: RegularFileProperty

    @get:Nested
    public abstract val pluginConfigs: ListProperty<PluginInfo>

    @TaskAction
    public fun generateUpdaterJson() {
        val map = HashMap<String, UpdateInfo>()

        for (plugin in pluginConfigs.get()) {
            if (!plugin.deploy.get()) {
                continue
            }

            require(plugin.version.get() != "unspecified") {
                "No project version is set for plugin '${plugin.name.get()}'! " +
                    "A version is required to deploy an Aliucord plugin."
            }

            val pluginFile = plugin.buildFile.get().asFile
            val pluginChecksum = CRC32().apply { update(pluginFile.readBytes()) }.value.toUInt()

            map[plugin.name.get()] = UpdateInfo(
                hidden = plugin.deployHidden.orNull,
                version = plugin.version.get(),
                buildUrl = plugin.buildUrl.orNull,
                buildCrc32 = pluginChecksum.toHexString(HexFormat.UpperCase),
                changelog = plugin.changelog.orNull,
                changelogMedia = plugin.changelogMedia.orNull,
                minimumDiscordVersion = plugin.minimumDiscordVersion.get(),
                minimumAliucordVersion = plugin.minimumAliucordVersion.get(),
                minimumKotlinVersion = plugin.minimumKotlinVersion.get(),
                minimumApiLevel = plugin.minimumApiLevel.get(),
            )
        }

        outputFile.get().asFile.writeText(Json.encodeToString(map))
    }

    // AliucordPluginExtension's properties are bound to an instance of this class
    // in order to pass it to this task. Passing the extension directly breaks configuration caching.
    public abstract class PluginInfo { // @formatter:off
        @get:Input @get:Optional public abstract val name: Property<String>
        @get:Input @get:Optional public abstract val version: Property<String>
        @get:Input @get:Optional public abstract val deploy: Property<Boolean>
        @get:Input @get:Optional public abstract val deployHidden: Property<Boolean>
        @get:Input @get:Optional public abstract val changelog: Property<String>
        @get:Input @get:Optional public abstract val changelogMedia: Property<String>
        @get:Input @get:Optional public abstract val buildUrl: Property<String>
        @get:Input @get:Optional public abstract val minimumDiscordVersion: Property<Int>
        @get:Input @get:Optional public abstract val minimumAliucordVersion: Property<String>
        @get:Input @get:Optional public abstract val minimumKotlinVersion: Property<String>
        @get:Input @get:Optional public abstract val minimumApiLevel: Property<Int>
        @get:InputFile @get:Optional public abstract val buildFile: RegularFileProperty
    } // @formatter:on
}
