package tech.mihoyo.mirai

import io.ktor.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.console.plugins.description
import net.mamoe.mirai.console.plugins.withDefault
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.TempMessageEvent
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

    @KtorExperimentalAPI
    @ExperimentalCoroutinesApi
    override fun onEnable() {
        logger.info("Plugin loaded! ${description.version}")
        subscribeAlways<BotEvent> {
            when (this) {
                is BotOnlineEvent -> {
                    if (!allSession.containsKey(bot.id)) {
                        if (config.exist(bot.id.toString())) {
                            SessionManager.createBotSession(bot, config.getConfigSection(bot.id.toString()))
                        } else {
                            logger.debug("${bot.id}未对CQHTTPMirai进行配置")
                        }
                    }
                }
                is TempMessageEvent -> {
                    allSession[bot.id]?.let {
                        (it as BotSession).cqApiImpl.cachedTempContact[this.sender.id] = this.group.id
                    }
                }
                is NewFriendRequestEvent -> {
                    allSession[bot.id]?.let {
                        (it as BotSession).cqApiImpl.cacheRequestQueue.add(this)
                    }
                }
                is MemberJoinRequestEvent -> {
                    allSession[bot.id]?.let {
                        (it as BotSession).cqApiImpl.cacheRequestQueue.add(this)
                    }
                }
            }
        }
        services.onEnable()
    }

    override fun onDisable() {
        services.onDisable()
        allSession.forEach { (_, session) -> session.close() }
    }

    private val imageFold: File = File(dataFolder, "image").apply { mkdirs() }

    internal fun image(imageName: String) = File(imageFold, imageName)

    fun saveImageAsync(name: String, data: ByteArray) =
        async {
            image(name).apply { writeBytes(data) }
        }

    fun saveImageAsync(name: String, data: String) =
        async {
            image(name).apply { writeText(data) }
        }
}