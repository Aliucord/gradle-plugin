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

/**
 * OptIn annotation for sensitive parts of the Aliucord Gradle Plugin API that
 * should not be used under normal circumstances and usage.
 */
@RequiresOptIn(message = "This API should not be used under normal circumstances. " +
    "Use only if you know the full implications of this API.")
@Retention(AnnotationRetention.BINARY)
public annotation class SensitiveAliucordApi
