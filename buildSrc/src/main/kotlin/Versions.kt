import java.io.File
import java.util.concurrent.TimeUnit

object Versions {
    const val projectVersion = "0.3.5"

    const val miraiCoreVersion = "2.9.2"
    const val miraiConsoleVersion = "2.9.2"

    const val ktorVersion = "1.5.4"
    const val kotlinVersion = "1.5.30"
    const val kotlinSerializationVersion = "1.2.2"
    const val autoService = "1.0.1"
    const val logback = "1.2.10"
    const val gson = "2.8.9"
    const val yamlkt = "0.10.2"
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