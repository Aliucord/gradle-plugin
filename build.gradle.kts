plugins {
    `kotlin-dsl`
    alias(libs.plugins.publish)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.android.sdk)

    implementation(libs.dex2jar)
    implementation(libs.jadx.core)
    implementation(libs.jadx.dexInput)
    implementation(libs.jadb)
}

gradlePlugin {
    plugins {
        create("aliucord") {
            id = "com.aliucord.gradle"
            implementationClass = "com.aliucord.gradle.AliucordPlugin"
        }
    }
}

mavenPublishing {
    coordinates("com.aliucord", "gradle", "2.0.0")
}

publishing {
    repositories {
        maven {
            name = "aliucord"
            url = uri("https://maven.aliucord.com/snapshots")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
