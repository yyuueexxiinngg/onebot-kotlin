package tech.mihoyo.mirai.web.http

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.utils.currentTimeMillis
import tech.mihoyo.mirai.BotSession
import tech.mihoyo.mirai.MiraiApi
import tech.mihoyo.mirai.data.common.*
import tech.mihoyo.mirai.util.logger
import tech.mihoyo.mirai.util.toJson
import tech.mihoyo.mirai.web.HeartbeatScope
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ReportServiceScope(coroutineContext: CoroutineContext) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineExceptionHandler { _, throwable ->
        logger.error("Exception in ReportService", throwable)
    } + SupervisorJob()
}


class ReportService(
    val session: BotSession
) {

    private val http = HttpClient()

    private var sha1Util: Mac? = null

    private var subscription: Listener<BotEvent>? = null

    private var heartbeatJob: Job? = null

    private lateinit var serviceConfig: ReportServiceConfig

    private val scope = ReportServiceScope(EmptyCoroutineContext)

    init {
        if (session.config.exist("http")) {
            serviceConfig = ReportServiceConfig(session.config.getConfigSection("http"))
            scope.launch {
                startReportService()
            }
        }
    }

    private suspend fun startReportService() {
        if (serviceConfig.postUrl != null && serviceConfig.postUrl != "") {
            report(
                session.cqApiImpl,
                serviceConfig.postUrl!!,
                session.bot.id,
                CQLifecycleMetaEventDTO(session.botId, "enable", currentTimeMillis).toJson(),
                serviceConfig.secret,
                false
            )

            subscription = session.bot.subscribeAlways {
                if (serviceConfig.secret != "") {
                    val mac = Mac.getInstance("HmacSHA1")
                    val secret = SecretKeySpec(serviceConfig.secret.toByteArray(), "HmacSHA1")
                    mac.init(secret)
                    sha1Util = mac
                }

                this.toCQDTO(isRawMessage = serviceConfig.postMessageFormat == "string")
                    .takeIf { it !is CQIgnoreEventDTO }?.apply {
                        val eventDTO = this
                        val jsonToSend = this.toJson()
                        scope.launch(Dispatchers.IO) {
                            report(
                                session.cqApiImpl,
                                serviceConfig.postUrl!!,
                                bot.id,
                                jsonToSend,
                                serviceConfig.secret,
                                true
                            )
                        }
                    }
            }

            if (session.heartbeatEnabled) {
                heartbeatJob = HeartbeatScope(EmptyCoroutineContext).launch {
                    while (true) {
                        report(
                            session.cqApiImpl,
                            serviceConfig.postUrl!!,
                            session.bot.id,
                            CQHeartbeatMetaEventDTO(
                                session.botId,
                                currentTimeMillis,
                                CQPluginStatusData(
                                    good = session.bot.isOnline,
                                    plugins_good = session.bot.isOnline,
                                    online = session.bot.isOnline
                                )
                            ).toJson(),
                            serviceConfig.secret,
                            false
                        )
                        delay(session.heartbeatInterval)
                    }
                }
            }
        }
    }

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
                    append("X-Signature", getSha1Hash(json))
                }
            }
            method = HttpMethod.Post
            body = TextContent(json, ContentType.Application.Json.withParameter("charset", "utf-8"))
        }
        if (res != "") logger.debug("收到上报响应  $res")
        if (shouldHandleOperation && res != null && res != "") {
            try {
                val respJson = Json.parseToJsonElement(res).jsonObject
                val sentJson = Json.parseToJsonElement(json).jsonObject
                val params = hashMapOf("context" to sentJson, "operation" to respJson)
                miraiApi.cqHandleQuickOperation(params)
            } catch (e: SerializationException) {
                logger.error("解析HTTP上报返回数据成json失败")
            }
        }
    }

    private fun getSha1Hash(content: String): String {
        sha1Util?.apply {
            return "sha1=" + this.doFinal(content.toByteArray()).fold("", { str, it -> str + "%02x".format(it) })
        }
        return ""
    }

    suspend fun close() {
        if (serviceConfig.postUrl != null && serviceConfig.postUrl != "") {
            report(
                session.cqApiImpl,
                serviceConfig.postUrl!!,
                session.bot.id,
                CQLifecycleMetaEventDTO(session.botId, "disable", currentTimeMillis).toJson(),
                serviceConfig.secret,
                false
            )
        }
        http.close()
        heartbeatJob?.cancel()
        subscription?.complete()
    }
}