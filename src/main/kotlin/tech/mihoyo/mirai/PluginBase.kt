package tech.mihoyo.mirai

import kotlinx.coroutines.async
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.console.plugins.withDefault
import tech.mihoyo.mirai.web.HttpApiServices
import java.io.File

object PluginBase : PluginBase() {
    private val config = loadConfig("setting.yml")
    val debug by config.withDefault { false }
    var services: HttpApiServices = HttpApiServices(this)

    override fun onLoad() {
        services.onLoad()
    }

    override fun onEnable() {
        logger.info("Plugin loaded!")
        services.onEnable()
    }

    override fun onDisable() {
        services.onDisable()
    }

    private val imageFold: File = File(dataFolder, "images").apply { mkdirs() }

    internal fun image(imageName: String) = File(imageFold, imageName)

    fun saveImageAsync(name: String, data: ByteArray) =
        async {
            image(name).apply { writeBytes(data) }
        }
}