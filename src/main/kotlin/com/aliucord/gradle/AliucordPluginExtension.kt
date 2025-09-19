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

import com.aliucord.gradle.models.Author
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Gets the registered Aliucord extension from this project.
 * This is only available when applying the `com.aliucord.plugin` Gradle plugin, for use in Aliucord plugins.
 * ```kt
 * project.extensions.getAliucord()
 * ```
 */
public fun ExtensionContainer.getAliucord(): AliucordExtension =
    getByName("aliucord") as AliucordExtension

/**
 * Attempts to get a registered Aliucord extension from this project.
 * This is only available when applying the `com.aliucord.plugin` Gradle plugin, for use in Aliucord plugins.
 * ```kt
 * project.extensions.findAliucord()
 * ```
 */
public fun ExtensionContainer.findAliucord(): AliucordExtension? =
    findByName("aliucord") as AliucordExtension?

/**
 * The interface through which Aliucord plugins are configured through.
 */
public abstract class AliucordExtension(private val project: Project) {
    /**
     * Whether to build and include this project with the other plugins for updating.
     * After publishing, this plugin will by default appear on plugin repositories unless [deployHidden] is set.
     */
    public val deploy: Property<Boolean> = project.objects
        .property(Boolean::class.java)
        .convention(true)

    /**
     * Whether to set the plugin as hidden for published builds, hiding it from plugin repositories.
     * This is useful in situations such as:
     * - Deploying updates to WIP plugins that aren't fully finished yet.
     * - Deploying an EOL update to a plugin to self-delete itself.
     */
    public val deployHidden: Property<Boolean> = project.objects
        .property(Boolean::class.java)
        .convention(false)

    /**
     * Specifies the minimum Discord version required to load this plugin.
     * If this is not explicitly set, then it is populated by the version of Discord specified in dependencies.
     */
    public val minimumDiscordVersion: Property<Int> = project.objects.property(Int::class.java)

    /**
     * The plugin's full changelog, including all previous versions. Markdown is allowed.
     * Example:
     * ```kt
     * changelog.set(
     *     """
     *     # 1.2.0
     *     * Add original emojis as an option
     *
     *     # 1.1.0
     *     * Fix target resource IDs
     *
     *     # 1.0.0
     *     * Released
     *     """.trimIndent()
     * )
     * ```
     */
    public val changelog: Property<String> = project.objects.property(String::class.java)

    /**
     * A url to a video or image to be shown on the plugin's changelog page.
     */
    public val changelogMedia: Property<String> = project.objects.property(String::class.java)

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

    /**
     * Sets the source repository, update url, and build url of this plugin.
     *
     * If you are not posting the source on GitHub, then [updateUrl] and [buildUrl] will need to be set manually
     * to a compatible GitHub repository of builds. See their respective documentation for more info.
     * Otherwise, if [updateUrl] and [buildUrl] both have not been set yet,
     * they will be generated based on the supplied url.
     *
     * @param repoUrl A repository url such as `https://github.com/Aliucord/plugins-template`, in this exact format.
     *                Using GitHub to distribute releases is required.
     */
    public fun github(repoUrl: String) {
        githubUrl.set(repoUrl)

        if (!updateUrl.isPresent && !buildUrl.isPresent) {
            val repo = repoUrl.removePrefix("https://github.com/")

            updateUrl.set("https://raw.githubusercontent.com/$repo/refs/heads/builds/updater.json")
            buildUrl.set("https://raw.githubusercontent.com/$repo/refs/heads/builds/${project.name}.zip")
        }
    }

    /**
     * Specifies the GitHub repository from which builds are distributed.
     * This link is shown on the plugin's card with a GitHub icon after installing.
     *
     * This property is populated through [github] by default.
     */
    public val githubUrl: Property<String> = project.objects.property(String::class.java)

    /**
     * Specifies a source code url, such as a link to a subdirectory in a GitHub repository containing
     * this specific plugin's source code. This url does not have to be pointing to GitHub if the source
     * is hosted elsewhere. This link is also shown on the plugin's card after installing.
     *
     * This property is optional and not populated by anything by default.
     */
    public val sourceUrl: Property<String> = project.objects.property(String::class.java)

    /**
     * Specifies the URL at which this plugin zip is going to be available for download.
     * This must be hosted on GitHub.
     *
     * This property is populated through [github] by default.
     * If explicitly setting this field, it must be a url in the format of:
     * - `https://raw.githubusercontent.com/${USERNAME}/${REPOSITORY}/refs/heads/builds/${PLUGINNAME}.zip`
     * - `https://raw.githubusercontent.com/${USERNAME}/${REPOSITORY}/refs/heads/builds/%s.zip`
     *
     * Note that the 2nd one is the legacy format, which includes `%s` to be replaced with the plugin's name at runtime.
     */
    public val buildUrl: Property<String> = project.objects.property(String::class.java)

    /**
     * Specifies the URL at which the updater metadata is available. This must be hosted on GitHub.
     *
     * This property is populated through [github] by default.
     * If explicitly setting this field, it must be a url in the format of:
     * `https://raw.githubusercontent.com/${USERNAME}/${REPOSITORY}/refs/heads/builds/updater.json`
     */
    public val updateUrl: Property<String> = project.objects.property(String::class.java)

    internal val authors: ListProperty<Author> = project.objects
        .listProperty(Author::class.java)
}
