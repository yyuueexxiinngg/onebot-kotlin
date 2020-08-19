package tech.mihoyo.mirai.web.http

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonException
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.subscribeAlways
import tech.mihoyo.mirai.MiraiApi
import tech.mihoyo.mirai.data.common.CQHeartbeatMetaEventDTO
import tech.mihoyo.mirai.data.common.CQIgnoreEventDTO
import tech.mihoyo.mirai.data.common.CQLifecycleMetaEventDTO
import tech.mihoyo.mirai.data.common.toCQDTO
import tech.mihoyo.mirai.util.logger
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
            val mirai = MiraiApi(bot)
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
                                    val eventDTO = this
                                    val jsonToSend = this.toJson()
                                    GlobalScope.launch(Dispatchers.IO) {
                                        report(
                                            mirai,
                                            config.postUrl,
                                            bot.id,
                                            jsonToSend,
                                            config.secret,
                                            eventDTO !is CQLifecycleMetaEventDTO
                                        )
                                    }
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
                                    val eventDTO = this
                                    val jsonToSend = this.toJson()
                                    GlobalScope.launch(Dispatchers.IO) {
                                        report(
                                            mirai,
                                            config.postUrl,
                                            bot.id,
                                            jsonToSend,
                                            config.secret,
                                            eventDTO !is CQLifecycleMetaEventDTO && eventDTO !is CQHeartbeatMetaEventDTO
                                        )
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    @OptIn(UnstableDefault::class)
    private suspend fun report(
        miraiApi: MiraiApi,
        url: String,
        botId: Long,
        json: String,
        secret: String,
        shouldHandleOperation: Boolean
    ) {
        val res = http.request<String?> {
            url(url)
            headers {
                append("User-Agent", "CQHttp/4.15.0")
                append("X-Self-ID", botId.toString())
                secret.takeIf { it != "" }?.apply {
                    append("X-Signature", getSha1Hash(botId, json))
                }
            }
            method = HttpMethod.Post
            body = TextContent(json, ContentType.Application.Json.withParameter("charset", "utf-8"))
        }
        logger.debug("收到上报响应  $res")
        if (shouldHandleOperation && res != null && res != "") {
            try {
                val respJson = Json.parseJson(res).jsonObject
                val sentJson = Json.parseJson(json).jsonObject
                val params = hashMapOf("context" to sentJson, "operation" to respJson)
                miraiApi.cqHandleQuickOperation(params)
            } catch (e: JsonException) {
                logger.error("解析HTTP上报返回数据成json失败")
            }
        }
    }

    private fun getSha1Hash(botId: Long, content: String): String {
        sha1UtilByBot[botId]?.apply {
            return "sha1=" + this.doFinal(content.toByteArray()).fold("", { str, it -> str + "%02x".format(it) })
        }
        return ""
    }

    override fun onDisable() {
        subscription?.complete()
    }
}