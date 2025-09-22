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

package com.aliucord.gradle.models

import kotlinx.serialization.Serializable

/**
 * The manifest embedded into a plugin zip containing info about the plugin,
 * as well as the dependencies that were used to build it. These are used
 * to determine whether the plugin may be safely loaded by Aliucord.
 */
@Serializable
internal data class PluginManifest(
    val name: String,
    val version: String,
    val description: String?,
    val authors: List<Author>,
    val links: Links,
    val changelog: String?,
    val changelogMedia: String?,

    val pluginClassName: String,

    var minimumDiscordVersion: Int? = null,
    var minimumAliucordVersion: String? = null,
    var minimumKotlinVersion: String? = null,
    var minimumApiLevel: Int? = null,
    val updateUrl: String?,
) {
    @Serializable
    internal data class Author(
        val name: String,
        val id: Long,
        val hyperlink: Boolean,
    )

    @Serializable
    internal data class Links(
        var github: String? = null,
        var source: String? = null,
    )
}
