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

import com.aliucord.gradle.entities.Author
import com.aliucord.gradle.entities.Links
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

public abstract class AliucordExtension @Inject constructor(private val project: Project) {
    public val projectType: Property<ProjectType> =
        project.objects.property(ProjectType::class.java).convention(ProjectType.PLUGIN)

    public val authors: ListProperty<Author> = project.objects.listProperty(Author::class.java)

    /**
     * Specify an author of this plugin.
     *
     * @param name      The user-facing name to display
     * @param id        The Discord ID of the author, optional.
     *                  This also will allow Aliucord to show a badge on your profile if the plugin is installed.
     * @param hyperlink Whether to hyperlink the Discord profile specified by [id].
     *                  Set this to false if you don't want to be spammed for support.
     */
    public fun author(name: String, id: Long = 0L, hyperlink: Boolean = true) {
        authors.add(Author(name, id, hyperlink))
    }

    public val links: Links = Links()

    /**
     * Set the source repository of this plugin.
     * If you are not posting the source on Github, then [updateUrl] and [buildUrl] will need
     * to be set manually to a compatible Github repository of builds.
     * Otherwise, if [updateUrl] and [buildUrl] have not set yet, then they will be generated based on the supplied url.
     *
     * @param url A repository url like `https://github.com/Aliucord/plugins-template`
     */
    public fun github(url: String) {
        links.github = url

        if (!updateUrl.isPresent && !buildUrl.isPresent) {
            updateUrl.set("$url/releases/latest/download/updater.json")
            buildUrl.set("$url/releases/download/${project.version}/${project.name}.zip")
        }
    }

    public val updateUrl: Property<String> = project.objects.property(String::class.java)
    public val changelog: Property<String> = project.objects.property(String::class.java)
    public val changelogMedia: Property<String> = project.objects.property(String::class.java)

    public val minimumDiscordVersion: Property<Int> = project.objects.property(Int::class.java)
    public val buildUrl: Property<String> = project.objects.property(String::class.java)

    public val excludeFromUpdaterJson: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    internal val userCache = project.gradle.gradleUserHomeDir.resolve("caches").resolve("aliucord")

    internal var discord: DiscordInfo? = null

    internal var pluginClassName: String? = null
}

internal class DiscordInfo(extension: AliucordExtension, val version: Int) {
    val cache = extension.userCache.resolve("discord")

    val apkFile = cache.resolve("discord-$version.apk")
    val jarFile = cache.resolve("discord-$version.jar")
}

public fun ExtensionContainer.getAliucord(): AliucordExtension {
    return getByName("aliucord") as AliucordExtension
}

public fun ExtensionContainer.findAliucord(): AliucordExtension? {
    return findByName("aliucord") as AliucordExtension?
}
