@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven {
            name = "aliucord"
            url = uri("https://maven.aliucord.com/snapshots")
        }
    }
}

rootProject.name = "gradle-plugin"
