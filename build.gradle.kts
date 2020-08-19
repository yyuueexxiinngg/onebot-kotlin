plugins {
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
    java
    id("com.github.johnrengelman.shadow") version "5.2.0"
}

group = "yyuueexxiinngg"
version = "0.2.2-embedded"

repositories {
    maven(url = "https://mirrors.huaweicloud.com/repository/maven")
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    jcenter()
    mavenCentral()
}

val miraiCoreVersion: String by rootProject.ext
val miraiConsoleVersion: String by rootProject.ext
val ktorVersion: String by rootProject.ext
val kotlinVersion = "1.3.72"

fun ktor(id: String, version: String = this@Build_gradle.ktorVersion) = "io.ktor:ktor-$id:$version"
fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
//    compileOnly("net.mamoe:mirai-core:$miraiCoreVersion")
    compileOnly("net.mamoe:mirai-console:$miraiConsoleVersion")
    compileOnly(kotlin("serialization", kotlinVersion))

    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("org.slf4j:slf4j-simple:1.7.9")
    api("com.github.ajalt:clikt:2.6.0")
    api("net.mamoe:mirai-console:$miraiConsoleVersion")
    api(kotlinx("serialization-runtime", "0.20.0"))
    api(ktor("server-cio"))
    api(ktor("client-cio"))
    api(ktor("websockets"))
    api(ktor("client-websockets"))

    api(kotlin("reflect", kotlinVersion))

    testImplementation(kotlin("stdlib-jdk8"))
//    testImplementation("net.mamoe:mirai-core:$miraiCoreVersion")
//    testImplementation("net.mamoe:mirai-core-qqandroid:$miraiCoreVersion")
//    testImplementation("net.mamoe:mirai-console:$miraiConsoleVersion")
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
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    val runEmbedded by creating(JavaExec::class.java) {
        group = "cqhttp-mirai"
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