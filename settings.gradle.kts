pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.github.johnrengelman.shadow" -> useModule("com.github.jengelman.gradle.plugins:shadow:${requested.version}")
            }
        }
    }

    repositories {
        maven(url = "https://mirrors.huaweicloud.com/repository/maven")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "onebot"
include(":onebot-mirai")
include(":onebot-kotlin")