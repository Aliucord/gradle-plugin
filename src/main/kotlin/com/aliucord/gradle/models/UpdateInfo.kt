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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single item of the updater manifest that is consumed by Aliucord to determine whether
 * a compatible update for a plugin exists to be installed.
 */
@Serializable
internal data class UpdateInfo(
    var hidden: Boolean? = false,
    var version: String? = null,
    @SerialName("build")
    var buildUrl: String? = null,
    var buildCrc32: String? = null,
    var changelog: String? = null,
    var changelogMedia: String? = null,
    var minimumDiscordVersion: Int? = null,
    var minimumAliucordVersion: String? = null,
    var minimumKotlinVersion: String? = null,
    var minimumApiLevel: Int? = null,
)
