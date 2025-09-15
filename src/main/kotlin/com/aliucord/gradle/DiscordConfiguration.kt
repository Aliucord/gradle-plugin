package com.aliucord.gradle

import com.aliucord.gradle.models.BuildInfo
import com.aliucord.gradle.utils.createProgressLogger
import com.aliucord.gradle.utils.downloadFromStream
import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.MultiDexFileReader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import java.net.URI

private const val DATA_URL = "https://raw.githubusercontent.com/Aliucord/Aliucord/builds/data.json"
private const val APK_URL = "https://aliucord.com/download/discord?v=%s"

internal fun ConfigurationContainer.getDiscord(): Configuration = getByName("discord")

internal fun registerDiscordConfiguration(project: Project) {
    project.configurations.register("discord") {
        isTransitive = false
    }

    project.afterEvaluate {
        downloadDiscordConfigurations(this)
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal fun downloadDiscordConfigurations(project: Project) {
    val configuration = project.configurations.getDiscord()
    val extension = project.extensions.getAliucord()

    require(configuration.dependencies.size <= 1) {
        "Only one Discord dependency is allowed per project but ${configuration.dependencies.size} were present!"
    }

    val dependency = configuration.dependencies.singleOrNull() ?: return

    val version = when (dependency.version) {
        "aliucord-SNAPSHOT" -> {
            project.logger.warn("Using aliucord-SNAPSHOT for Discord is discouraged to ensure reproducible builds!")
            project.logger.lifecycle("Fetching Aliucord's target Discord version")
            val stream = URI.create(DATA_URL).toURL().openStream()
            val data = Json.decodeFromStream<BuildInfo>(stream)

            project.logger.lifecycle("Fetched target Discord version: ${data.discordVersionName} (${data.discordVersionCode})")
            data.discordVersionCode
        }

        else -> dependency.version?.toIntOrNull()
            ?: throw GradleException("Invalid Discord APK version code")
    }

    // TODO: get rid of this
    val paths = DiscordInfo(extension, version)
    extension.discord = paths

    // Download APK from Aliucord's mirror
    if (!paths.apkFile.exists() && !paths.jarFile.exists()) {
        paths.cache.mkdirs()
        downloadFromStream(
            url = String.format(APK_URL, paths.version),
            output = paths.apkFile,
            progressLogger = createProgressLogger(project, "Download Discord v${paths.version}"),
        )
    }

    if (!paths.jarFile.exists()) {
        project.logger.lifecycle("Converting Discord APK to jar")

        Dex2jar.from(MultiDexFileReader.open(paths.apkFile.inputStream()))
            .skipDebug(false)
            .topoLogicalSort()
            .noCode(true)
            .to(paths.jarFile.toPath())
    }

    // TODO: use addProvider to make resolving dependencies lazy
    project.dependencies.add("compileOnly", project.files(paths.jarFile))
}
