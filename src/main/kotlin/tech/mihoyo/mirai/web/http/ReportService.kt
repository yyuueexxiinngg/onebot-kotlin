package tech.mihoyo.mirai.web.http

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.subscribeAlways
import org.bouncycastle.util.encoders.Hex
import tech.mihoyo.mirai.data.common.CQIgnoreEventDTO
import tech.mihoyo.mirai.data.common.toCQDTO
import tech.mihoyo.mirai.util.toJson
import tech.mihoyo.mirai.web.HttpApiService
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class ReportService(
    /**
     * 插件对象
     */
    override val console: PluginBase
) : HttpApiService {

    private val http = HttpClient()

    private val configByBots: MutableMap<Long, ReportServiceConfig> = mutableMapOf()

    private val sha1UtilByBot: MutableMap<Long, Mac> = mutableMapOf()

    private var subscription: Listener<BotEvent>? = null

    override fun onLoad() {
    }

    override fun onEnable() {
        subscription = console.subscribeAlways {
            when (this) {
                is BotOnlineEvent -> {
                    if (!configByBots.containsKey(bot.id)) {
                        val config = ReportServiceConfig(console.loadConfig("setting.yml"), bot)
                        configByBots[bot.id] = config
                        if (config.postUrl != "") {
                            if (config.secret != "") {
                                val mac = Mac.getInstance("HmacSHA1")
                                val secret = SecretKeySpec(config.secret.toByteArray(), "HmacSHA1")
                                mac.init(secret)
                                sha1UtilByBot[bot.id] = mac
                            }

                            this.toCQDTO(isRawMessage = config.postMessageFormat == "string")
                                .takeIf { it !is CQIgnoreEventDTO }?.apply {
                                    report(config.postUrl, bot.id, this.toJson(), config.secret)
                                }
                        }
                    }
                }
                else -> {
                    val event = this
                    configByBots[bot.id]?.apply {
                        if (this.postUrl != "") {
                            val config = this
                            event.toCQDTO(isRawMessage = config.postMessageFormat == "string")
                                .takeIf { it !is CQIgnoreEventDTO }?.apply {
                                    report(config.postUrl, bot.id, this.toJson(), config.secret)
                                }
                        }
                    }
                }
            }
        }
    }

    private suspend fun report(url: String, botId: Long, json: String, secret: String): String {
        return http.request {
            url(url)
            headers {
                append("User-Agent", "MiraiHttp/0.1.0")
                append("X-Self-ID", botId.toString())
                secret.takeIf { it != "" }?.apply {
                    println("Triggered")
                    println(this)
                    append("X-Signature", getSha1Hash(botId, json))
                }
            }
            method = HttpMethod.Post
            body = TextContent(json, ContentType.Application.Json)
        }
    }

    private fun getSha1Hash(botId: Long, content: String): String {
        sha1UtilByBot[botId]?.apply {
            return this.doFinal(content.toByteArray()).fold("", { str, it -> str + "%02x".format(it) })
        }
        return ""
    }

    override fun onDisable() {
        subscription?.complete()
    }
}