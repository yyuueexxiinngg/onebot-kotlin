plugins {
    java
    kotlin("jvm") version Versions.kotlinVersion
    kotlin("plugin.serialization") version Versions.kotlinVersion
    kotlin("kapt") version Versions.kotlinVersion
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.github.gmazzo.buildconfig") version "2.0.2"
}

allprojects {
    group = "com.github.yyuueexxiinngg"
    version = Versions.projectVersion

    repositories {
        maven(url = "https://maven.pkg.github.com/mzdluo123/silk4j") {
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}