plugins {
    java
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.mamoe:mirai-core-api-jvm:${Versions.miraiCoreVersion}")
    implementation("net.mamoe:mirai-core-jvm:${Versions.miraiCoreVersion}")
    implementation("net.mamoe:mirai-core-utils-jvm:${Versions.miraiCoreVersion}")
    implementation("net.mamoe:mirai-console:${Versions.miraiConsoleVersion}")
    implementation("net.mamoe:mirai-console-terminal:${Versions.miraiConsoleVersion}")
    implementation("com.github.ajalt.clikt:clikt:${Versions.clikt}")

    implementation(project(":onebot-mirai"))
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "com.github.yyuueexxiinngg.onebot.MainKt"
    }
}

tasks {
    val runEmbedded by creating(JavaExec::class.java) {
        group = "onebot-kotlin"
        main = "com.github.yyuueexxiinngg.onebot.MainKt"
        workingDir = File("../test")
        dependsOn(shadowJar)
        dependsOn(testClasses)
        doFirst {
            classpath = sourceSets["test"].runtimeClasspath
            standardInput = System.`in`
            args("--backend", "mirai")
        }
    }
}

kotlin.sourceSets.all {
    languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
}

kotlin.target.compilations.all {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    kotlinOptions.jvmTarget = "1.8"
}