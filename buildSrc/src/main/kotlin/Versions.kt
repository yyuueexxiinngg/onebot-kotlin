import java.io.File
import java.util.concurrent.TimeUnit

object Versions {
    const val projectVersion = "0.3.3"

    const val miraiCoreVersion = "1.3.3"
    const val miraiConsoleVersion = "1.0.1"

    const val ktorVersion = "1.4.1"
    const val kotlinVersion = "1.4.10"
    const val kotlinSerializationVersion = "1.0.1"
    const val autoService = "1.0-rc7"
    const val logback = "1.2.3"
    const val gson = "2.8.6"
    const val yamlkt = "0.7.4"

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