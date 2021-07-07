import java.io.File
import java.util.concurrent.TimeUnit

object Versions {
    const val projectVersion = "0.3.5"

    const val miraiCoreVersion = "2.7-M2"
    const val miraiConsoleVersion = "2.7-M2"

    const val ktorVersion = "1.5.0"
    const val kotlinVersion = "1.5.10" // https://youtrack.jetbrains.com/issue/KT-47161
    const val kotlinSerializationVersion = "1.1.0"
    const val autoService = "1.0-rc7"
    const val logback = "1.2.3"
    const val gson = "2.8.6"
    const val yamlkt = "0.9.0"
    const val leveldb = "1.2"
    const val snappy = "0.4"
    const val silk4j = "1.2-dev"

    // OneBot Kotlin
    const val clikt = "3.0.1"
}

fun ktor(id: String, version: String = Versions.ktorVersion) = "io.ktor:ktor-$id:$version"
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