plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id("com.github.johnrengelman.shadow")
    id("com.github.gmazzo.buildconfig")
}

dependencies {
    kapt("com.google.auto.service", "auto-service", Versions.autoService)

    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("net.mamoe:mirai-core-api-jvm:${Versions.miraiCoreVersion}")
    compileOnly("net.mamoe:mirai-core-jvm:${Versions.miraiCoreVersion}")
    compileOnly("net.mamoe:mirai-console:${Versions.miraiConsoleVersion}")
    compileOnly("net.mamoe:mirai-console-terminal:${Versions.miraiConsoleVersion}")
    compileOnly(kotlin("serialization", Versions.kotlinVersion))
    compileOnly("com.google.auto.service", "auto-service-annotations", Versions.autoService)

    implementation("net.mamoe.yamlkt:yamlkt:${Versions.yamlkt}")
    implementation(kotlin("reflect", Versions.kotlinVersion))
    implementation(kotlinx("serialization-cbor", Versions.kotlinSerializationVersion))
    implementation(kotlinx("serialization-json", Versions.kotlinSerializationVersion))
    implementation(ktor("server-cio"))
    implementation(ktor("websockets"))
    implementation(ktor("client-okhttp"))
    implementation(ktor("client-websockets"))
    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("com.google.code.gson:gson:${Versions.gson}")
    implementation("io.github.pcmind:leveldb:${Versions.leveldb}")
    implementation("org.iq80.snappy:snappy:${Versions.snappy}")
//    implementation("org.mapdb:mapdb:${Versions.mapdb}")

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation("net.mamoe:mirai-core-api-jvm:${Versions.miraiCoreVersion}")
    testImplementation("net.mamoe:mirai-core-jvm:${Versions.miraiCoreVersion}")
    testImplementation("net.mamoe:mirai-console:${Versions.miraiConsoleVersion}")
    testImplementation("net.mamoe:mirai-console-terminal:${Versions.miraiConsoleVersion}")
}

tasks {
    buildConfig {
        packageName("com.github.yyuueexxiinngg.onebot")
        val commitHash = "git rev-parse --short HEAD".runCommand(projectDir)
        buildConfigField("String", "VERSION", "\"${Versions.projectVersion}\"")
        if (commitHash != null) {
            buildConfigField("String", "COMMIT_HASH", "\"$commitHash\"")
        }
    }

    shadowJar {
        dependsOn(generateBuildConfig)
    }

    val runMiraiConsole by creating(JavaExec::class.java) {
        group = "mirai"
        main = "mirai.RunMirai"
        dependsOn(shadowJar)
        dependsOn(testClasses)

        val testConsoleDir = "../test"

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
            args(Versions.miraiCoreVersion, Versions.miraiConsoleVersion)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin.sourceSets.all {
    languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
}

kotlin.target.compilations.all {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=enable"
    kotlinOptions.jvmTarget = "1.8"
}