<div align="center">
    <h1>Aliucord Gradle Plugin</h1>
    <p>The Gradle build plugin for the Aliucord project & plugins </p>
    <span>
        <a href="https://discord.gg/EsNDvBaHVU"><img alt="Discord" src="https://img.shields.io/discord/811255666990907402?logo=discord&logoColor=white&style=for-the-badge&color=5865F2"/></a>
        <a href="https://github.com/Aliucord/Aliucord/actions/workflows/build.yml?query=branch%3Amain"><img alt="Build Status" src="https://img.shields.io/github/actions/workflow/status/Aliucord/gradle-plugin/build.yml?branch=main&label=Build&logo=github&style=for-the-badge"/></a>
        <img alt="Latest version" src="https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.aliucord.com%2Freleases%2Fcom%2Faliucord%2Fgradle%2Fmaven-metadata.xml&style=for-the-badge&logo=apachemaven">
        <a href="https://github.com/Aliucord/Aliucord/blob/main/LICENSE"><img alt="License" src="https://img.shields.io/github/license/Aliucord/gradle-plugin?style=for-the-badge&color=181717"/></a>
        <img alt="Code size" src="https://img.shields.io/github/languages/code-size/Aliucord/gradle-plugin?style=for-the-badge&color=181717"/>
    </span>
</div>

## Configuration (plugins)

Please refer to [Aliucord/plugins-template](https://github.com/Aliucord/plugins-template) for a complete
example on how to use this Gradle plugin from the context of an Aliucord plugin.

To add it to your project, add the Aliucord Maven repository as well as the plugin artifact.
The examples below assume you are using Gradle's "settings" dependency resolution management, and
Gradle's version catalogs to manage dependency versions.

You will also need to specify the Discord version you will be building your plugins against.
At the moment, only v126.21 (126021) is available.
You will also need to explicitly specify the Kotlin stdlib (if you are using Kotlin) as a `compileOnly` dependency.

```kotlin
// settings.gradle.kts

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven("https://maven.aliucord.com/releases")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliucord.com/releases")
    }
}
```

```toml
# gradle/libs.versions.toml

[versions]
aliucord-gradle = "2.0.0"
android = "8.13.0"
discord = "126021"
kotlin = "2.2.0"

[libraries]
discord = { module = "com.discord:discord", version.ref = "discord" }
kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }

[plugins]
aliucord-plugin = { id = "com.aliucord.plugin", version.ref = "aliucord-gradle" }
android-library = { id = "com.android.library", version.ref = "android" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

You will then need to apply the plugin in your root project, as well as all plugin subprojects.
For plugin subproject, you must also then configure the `aliucord` Gradle extension.

```kotlin
// build.gradle.kts (root)

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.aliucord.plugin) apply true // Applying is not a mistake!
}
```

```kotlin
// build.gradle.kts (plugins)

plugins {
    alias(libs.plugins.kotlin.android) apply true
    alias(libs.plugins.android.library) apply true
    alias(libs.plugins.aliucord.plugin) apply true
}

dependencies {
    compileOnly(libs.discord)
    compileOnly(libs.kotlin.stdlib)
}

aliucord {
    // ... Refer to plugins-template ...
}
```

If you are applying the plugins to multiple projects at once using `subprojects { ... }`, the syntax will vary.
Please refer to the plugins-template example here:
[`build.gradle.kts`](https://github.com/Aliucord/plugins-template/blob/main/plugins/build.gradle.kts)

## Usage (plugins)

To build plugins using this Gradle plugin as a build tool:

1. Create an entrypoint class extending the `Plugin` class from Aliucord, and is annotated with `@AliucordPlugin`.
2. Run the `make` task for the subproject to build the plugin. If you need the output plugin zip, the task
   will print a path to the built plugin upon completion.
3. To quickly test plugins with a live Aliucord installation (with a physical device or an emulator), run the
   `deployWithAdb` task for the subproject. This will automatically build, deploy, and restart Aliucord for
   you to immediately test your changes.
