@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
    alias(libs.plugins.publish)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    explicitApi()
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.android.repository)
    compileOnly(libs.android.sdk)
    compileOnly(libs.android.sdklib)

    implementation(libs.dex2jar)
    implementation(libs.jadx.core)
    implementation(libs.jadx.dexInput)
    implementation(libs.kotlinx.serialization)
}

gradlePlugin {
    plugins {
        create("aliucord-plugin") {
            id = "com.aliucord.plugin"
            implementationClass = "com.aliucord.gradle.plugins.AliucordPluginGradle"
        }
        create("aliucord-core") {
            id = "com.aliucord.core"
            implementationClass = "com.aliucord.gradle.plugins.AliucordCoreGradle"
        }
        create("aliucord-injector") {
            id = "com.aliucord.injector"
            implementationClass = "com.aliucord.gradle.plugins.AliucordInjectorGradle"
        }
    }
}

mavenPublishing {
    coordinates("com.aliucord", "gradle", "2.0.0")
    configureBasedOnAppliedPlugins()
}

publishing {
    repositories {
        maven {
            name = "aliucord"
            url = uri("https://maven.aliucord.com/releases")
            credentials {
                username = System.getenv("MAVEN_RELEASE_USERNAME")
                password = System.getenv("MAVEN_RELEASE_PASSWORD")
            }
        }
    }
}
