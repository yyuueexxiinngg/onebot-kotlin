package tech.mihoyo.mirai

import kotlinx.coroutines.async
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.console.plugins.description
import net.mamoe.mirai.console.plugins.withDefault
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.subscribeAlways
import tech.mihoyo.mirai.SessionManager.allSession
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
        logger.info("Plugin loaded! ${description.version}")
        subscribeAlways<BotOnlineEvent> {
            if (!allSession.containsKey(bot.id)) {
                if (config.exist(bot.id.toString())) {
                    SessionManager.createBotSession(bot, config.getConfigSection(bot.id.toString()))
                } else {
                    logger.debug("${bot.id}未对CQHTTPMirai进行配置")
                }
            }
        }
        services.onEnable()
    }

    override fun onDisable() {
        services.onDisable()
        allSession.forEach { (_, session) -> session.close() }
    }

    private val imageFold: File = File(dataFolder, "images").apply { mkdirs() }

    internal fun image(imageName: String) = File(imageFold, imageName)

    fun saveImageAsync(name: String, data: ByteArray) =
        async {
            image(name).apply { writeBytes(data) }
        }
}