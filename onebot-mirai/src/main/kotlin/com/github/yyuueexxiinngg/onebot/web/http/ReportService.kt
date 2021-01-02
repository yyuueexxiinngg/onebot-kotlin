package com.github.yyuueexxiinngg.onebot.web.http

import com.github.yyuueexxiinngg.onebot.BotEventListener
import com.github.yyuueexxiinngg.onebot.BotSession
import com.github.yyuueexxiinngg.onebot.MiraiApi
import com.github.yyuueexxiinngg.onebot.data.common.CQHeartbeatMetaEventDTO
import com.github.yyuueexxiinngg.onebot.data.common.CQLifecycleMetaEventDTO
import com.github.yyuueexxiinngg.onebot.data.common.CQPluginStatusData
import com.github.yyuueexxiinngg.onebot.logger
import com.github.yyuueexxiinngg.onebot.util.currentTimeSeconds
import com.github.yyuueexxiinngg.onebot.util.toJson
import com.github.yyuueexxiinngg.onebot.web.HeartbeatScope
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.net.SocketTimeoutException
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
    private val session: BotSession
) {

    private val http = HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
        install(HttpTimeout)
    }

    private var sha1Util: Mac? = null

    private var subscription: Pair<BotEventListener, Boolean>? = null

    private var heartbeatJob: Job? = null

    private val scope = ReportServiceScope(EmptyCoroutineContext)

    private val settings = session.settings.http

    init {
        scope.launch {
            startReportService()
        }
    }

    private suspend fun startReportService() {
        if (settings.postUrl != "") {
            if (settings.secret != "") {
                val mac = Mac.getInstance("HmacSHA1")
                val secret = SecretKeySpec(settings.secret.toByteArray(), "HmacSHA1")
                mac.init(secret)
                sha1Util = mac
            }

            report(
                session.cqApiImpl,
                settings.postUrl,
                session.bot.id,
                CQLifecycleMetaEventDTO(session.botId, "enable", currentTimeSeconds()).toJson(),
                settings.secret,
                false
            )

            subscription =
                Pair(
                    session.subscribeEvent(
                        { jsonToSend ->
                            scope.launch(Dispatchers.IO) {
                                report(
                                    session.cqApiImpl,
                                    settings.postUrl,
                                    session.bot.id,
                                    jsonToSend,
                                    settings.secret,
                                    true
                                )
                            }
                        },
                        settings.postMessageFormat == "string"
                    ),
                    settings.postMessageFormat == "string"
                )

            if (session.settings.heartbeat.enable) {
                heartbeatJob = HeartbeatScope(EmptyCoroutineContext).launch {
                    while (true) {
                        report(
                            session.cqApiImpl,
                            settings.postUrl,
                            session.bot.id,
                            CQHeartbeatMetaEventDTO(
                                session.botId,
                                currentTimeSeconds(),
                                CQPluginStatusData(
                                    good = session.bot.isOnline,
                                    online = session.bot.isOnline
                                ),
                                session.settings.heartbeat.interval
                            ).toJson(),
                            settings.secret,
                            false
                        )
                        delay(session.settings.heartbeat.interval)
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
        try {
            val res = http.request<String?> {
                url(url)
                headers {
                    append("User-Agent", "CQHttp/4.15.0")
                    append("X-Self-ID", botId.toString())
                    secret.takeIf { it != "" }?.apply {
                        append("X-Signature", getSha1Hash(json))
                    }
                }
                if (settings.timeout > 0) {
                    timeout { socketTimeoutMillis = settings.timeout }
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
        } catch (e: Exception) {
            if (e is SocketTimeoutException) {
                logger.warning("HTTP上报超时")
            } else {
                logger.error(e)
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
        if (settings.postUrl != "") {
            report(
                session.cqApiImpl,
                settings.postUrl,
                session.bot.id,
                CQLifecycleMetaEventDTO(session.botId, "disable", currentTimeSeconds()).toJson(),
                settings.secret,
                false
            )
        }
        http.close()
        heartbeatJob?.cancel()
        subscription?.let { session.unsubscribeEvent(it.first, it.second) }
    }
}