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

package com.aliucord.gradle

import com.android.build.gradle.BaseExtension
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.kotlin.dsl.getByName

internal object Constants {
    const val TASK_GROUP = "aliucord"
    const val TASK_GROUP_INTERNAL = "aliucord-internal"
}

/**
 * Retrieves a base configuration for configuring an app or library Android plugin.
 * AGP must be registered in the containing project.
 */
internal fun ExtensionContainer.getAndroid(): BaseExtension =
    getByName<BaseExtension>("android")
