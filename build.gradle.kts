plugins {
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.serialization") version "1.4.0"
    kotlin("kapt") version "1.4.0"
    java
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.github.gmazzo.buildconfig") version "2.0.2"
}

val projectVersion = "0.3.0"
version = projectVersion
group = "yyuueexxiinngg"

repositories {
    maven(url = "https://mirrors.huaweicloud.com/repository/maven")
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    gradlePluginPortal()
    jcenter()
    mavenCentral()
}

val miraiCoreVersion = "1.3.3"
val miraiConsoleVersion = "1.0-RC-1"
val ktorVersion = "1.4.1"
val kotlinVersion = "1.4.0"
val kotlinSerializationVersion = "1.0.1"
val autoService = "1.0-rc7"

fun ktor(id: String, version: String = this@Build_gradle.ktorVersion) = "io.ktor:ktor-$id:$version"
fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"
fun String.runCommand(workingDir: File): String? {
    return try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        proc.inputStream.bufferedReader().readText().trim()
    } catch (e: java.io.IOException) {
        e.printStackTrace()
        null
    }
}

dependencies {
    kapt("com.google.auto.service", "auto-service", autoService)

    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("serialization", kotlinVersion))
    compileOnly("com.google.auto.service", "auto-service-annotations", autoService)

    implementation(kotlinx("serialization-cbor", kotlinSerializationVersion))
    implementation(kotlinx("serialization-json", kotlinSerializationVersion))
    implementation("ch.qos.logback:logback-classic:1.2.3")

    api("com.github.ajalt:clikt:2.6.0")
    api("net.mamoe:mirai-core:$miraiCoreVersion")
    api("net.mamoe:mirai-core-qqandroid:$miraiCoreVersion")
    api("net.mamoe:mirai-console:$miraiConsoleVersion")
    api("net.mamoe:mirai-console-terminal:$miraiConsoleVersion")
    implementation("com.google.code.gson:gson:2.8.6")

    api(ktor("server-cio"))
    api(ktor("client-okhttp"))
    api(ktor("websockets"))
    api(ktor("client-websockets"))
    api(kotlin("reflect", kotlinVersion))

    testImplementation(kotlin("stdlib-jdk8"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes["Main-Class"] = "tech.mihoyo.MainKt"
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    buildConfig {
        val commitHash = "git rev-parse --short HEAD".runCommand(projectDir)
        buildConfigField("String", "VERSION", "\"$projectVersion\"")
        if (commitHash != null) { buildConfigField("String", "COMMIT_HASH", "\"$commitHash\"") }
    }

    shadowJar {
        dependsOn(generateBuildConfig)
    }

    val runEmbedded by creating(JavaExec::class.java) {
        group = "onebot-kotlin"
        main = "tech.mihoyo.MainKt"
        workingDir = File("test")
        dependsOn(shadowJar)
        dependsOn(testClasses)
        doFirst {
            classpath = sourceSets["test"].runtimeClasspath
            standardInput = System.`in`
            args("--backend", "mirai")
        }
    }

    val runMiraiConsole by creating(JavaExec::class.java) {
        group = "mirai"
        main = "mirai.RunMirai"
        dependsOn(shadowJar)
        dependsOn(testClasses)

        val testConsoleDir = "test"

        doFirst {
            fun removeOldVersions() {
                File("$testConsoleDir/plugins/").walk()
                    .filter { it.name.matches(Regex("""${project.name}-.*-all.jar""")) }
                    .forEach {
                        it.delete()
                        println("deleting old files: ${it.name}")
                    }
            }

            fun copyBuildOutput() {
                File("build/libs/").walk()
                    .filter { it.name.contains("-all") }
                    .maxBy { it.lastModified() }
                    ?.let {
                        println("Coping ${it.name}")
                        it.inputStream()
                            .transferTo(File("$testConsoleDir/plugins/${it.name}").apply { createNewFile() }
                                .outputStream())
                        println("Copied ${it.name}")
                    }
            }

            workingDir = File(testConsoleDir)
            workingDir.mkdir()
            File(workingDir, "plugins").mkdir()
            removeOldVersions()
            copyBuildOutput()

            classpath = sourceSets["test"].runtimeClasspath
            standardInput = System.`in`
            args(miraiCoreVersion, miraiConsoleVersion)
        }
    }
}