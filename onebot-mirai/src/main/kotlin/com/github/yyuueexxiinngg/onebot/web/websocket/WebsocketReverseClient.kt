package com.github.yyuueexxiinngg.onebot.web.websocket

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.*
import io.ktor.client.request.header

import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import net.mamoe.mirai.Bot

import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.utils.currentTimeSeconds
import com.github.yyuueexxiinngg.onebot.BotSession
import com.github.yyuueexxiinngg.onebot.PluginSettings
import com.github.yyuueexxiinngg.onebot.data.common.*
import com.github.yyuueexxiinngg.onebot.util.EventFilter
import com.github.yyuueexxiinngg.onebot.util.logger
import com.github.yyuueexxiinngg.onebot.util.toJson
import com.github.yyuueexxiinngg.onebot.web.HeartbeatScope
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class WebsocketReverseClientScope(coroutineContext: CoroutineContext) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineExceptionHandler { _, throwable ->
        logger.error("Exception in WebsocketReverseClient", throwable)
    } + SupervisorJob()
}

class WebSocketReverseClient(
    private val session: BotSession
) {
    private val httpClients: MutableMap<String, HttpClient> = mutableMapOf()
    private var settings: MutableList<PluginSettings.WebsocketReverseClientSettings>? = session.settings.wsReverse
    private var subscriptions: MutableMap<String, Listener<BotEvent>?> = mutableMapOf()
    private var websocketSessions: MutableMap<String, DefaultClientWebSocketSession> = mutableMapOf()
    private var heartbeatJobs: MutableMap<String, Job> = mutableMapOf()
    private var connectivityChecks: MutableList<String> = mutableListOf()
    private var closing = false
    private val scope = WebsocketReverseClientScope(EmptyCoroutineContext)

    init {
//        if (session.config.exist("ws_reverse")) {
        settings?.forEach {
            logger.debug("Host: ${it.reverseHost}, Port: ${it.reversePort}, Enable: ${it.enable}, Use Universal: ${it.useUniversal}")
            if (it.enable) {
                if (it.useUniversal) {
                    scope.launch {
                        startGeneralWebsocketClient(session.bot, it, "Universal")
                    }
                } else {
                    scope.launch {
                        startGeneralWebsocketClient(session.bot, it, "Api")
                    }
                    scope.launch {
                        startGeneralWebsocketClient(session.bot, it, "Event")
                    }
                }
            }
        }
//        } else {
//            logger.debug("${session.bot.id}未对ws_reverse进行配置")
//    }
    }

    @OptIn(KtorExperimentalAPI::class, ExperimentalCoroutinesApi::class)
    @Suppress("DuplicatedCode")
    private suspend fun startGeneralWebsocketClient(
        bot: Bot,
        settings: PluginSettings.WebsocketReverseClientSettings,
        clientType: String
    ) {
        val httpClientKey = "${settings.reverseHost}:${settings.reversePort}-Client-$clientType"

        httpClients[httpClientKey] = HttpClient {
            install(WebSockets)
        }

        logger.debug("$httpClientKey 开始启动")
        val path = when (clientType) {
            "Api" -> settings.reverseApiPath
            "Event" -> settings.reverseEventPath
            else -> settings.reversePath
        }

        try {
            if (!settings.useTLS) {
                httpClients[httpClientKey]!!.ws(
                    host = settings.reverseHost,
                    port = settings.reversePort,
                    path = path,
                    request = {
                        header("User-Agent", "CQHttp/4.15.0")
                        header("X-Self-ID", bot.id.toString())
                        header("X-Client-Role", clientType)
                        settings.accessToken.let {
                            if (it != "") {
                                header(
                                    "Authorization",
                                    "Token ${settings.accessToken}"
                                )
                            }
                        }
                    }
                ) {
                    // 用来检测Websocket连接是否关闭
                    websocketSessions[httpClientKey] = this
                    if (!subscriptions.containsKey(httpClientKey)) {
                        startWebsocketConnectivityCheck(bot, settings, clientType)
                        logger.debug("$httpClientKey Websocket Client启动完毕")
                        when (clientType) {
                            "Api" -> listenApi(incoming, outgoing)
                            "Event" -> listenEvent(httpClientKey, settings, this, clientType)
                            "Universal" -> {
                                listenEvent(httpClientKey, settings, this, clientType)
                                listenApi(incoming, outgoing)
                            }
                        }
                    } else {
                        logger.warning("Websocket session alredy exist, $httpClientKey")
                    }
                }
            } else {
                httpClients[httpClientKey]!!.wss(
                    host = settings.reverseHost,
                    port = settings.reversePort,
                    path = path,
                    request = {
                        header("User-Agent", "CQHttp/4.15.0")
                        header("X-Self-ID", bot.id.toString())
                        header("X-Client-Role", clientType)
                        settings.accessToken.let {
                            if (it != "") {
                                header(
                                    "Authorization",
                                    "Token ${settings.accessToken}"
                                )
                            }
                        }
                    }
                ) {
                    // 用来检测Websocket连接是否关闭
                    websocketSessions[httpClientKey] = this
                    if (!subscriptions.containsKey(httpClientKey)) {
                        startWebsocketConnectivityCheck(bot, settings, clientType)
                        logger.debug("$httpClientKey Websocket Client启动完毕")
                        when (clientType) {
                            "Api" -> listenApi(incoming, outgoing)
                            "Event" -> listenEvent(httpClientKey, settings, this, clientType)
                            "Universal" -> {
                                listenEvent(httpClientKey, settings, this, clientType)
                                listenApi(incoming, outgoing)
                            }
                        }
                    } else {
                        logger.warning("Websocket session alredy exist, $httpClientKey")
                    }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is ConnectException -> {
                    logger.warning("Websocket连接出错, 请检查服务器是否开启并确认正确监听端口, 将在${settings.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey Path: $path")
                }
                is EOFException -> {
                    logger.warning("Websocket连接出错, 服务器返回数据不正确, 请检查Websocket服务器是否配置正确, 将在${settings.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey Path: $path")
                }
                is IOException -> {
                    logger.warning("Websocket连接出错, 可能被服务器关闭, 将在${settings.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey Path: $path")
                }
                is CancellationException -> {
                    closing = true
                    logger.info("Websocket连接关闭中, Host: $httpClientKey Path: $path")
                }
                else -> {
                    logger.warning("Websocket连接出错, 未知错误, 请检查配置, 如配置错误请修正后重启mirai " + e.message + e.javaClass.name)
                }
            }
            httpClients[httpClientKey]?.apply { this.close() }
            httpClients.remove(httpClientKey)
            delay(settings.reconnectInterval)
            if (!closing) startGeneralWebsocketClient(session.bot, settings, clientType)
            else {
                logger.info("反向Websocket连接关闭中, Host: $httpClientKey Path: $path")
                closing = false
            }
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun listenApi(incoming: ReceiveChannel<Frame>, outgoing: SendChannel<Frame>) {
        incoming.consumeEach {
            when (it) {
                is Frame.Text -> {
                    scope.launch {
                        handleWebSocketActions(outgoing, session.cqApiImpl, it.readText())
                    }
                }
                else -> logger.warning("Unsupported incomeing frame")
            }
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun listenEvent(
        httpClientKey: String,
        settings: PluginSettings.WebsocketReverseClientSettings,
        websocketSession: DefaultClientWebSocketSession,
        clientType: String
    ) {
        // 通知服务方链接建立
        websocketSession.outgoing.send(
            Frame.Text(
                CQLifecycleMetaEventDTO(
                    session.bot.id,
                    "connect",
                    currentTimeSeconds
                ).toJson()
            )
        )
        val isRawMessage = settings.postMessageFormat != "array"
        subscriptions[httpClientKey] = session.bot.subscribeAlways {
            // 保存Event以便在WebsocketSession Block中使用
            if (this.bot.id == session.botId) {
                this.toCQDTO(isRawMessage = isRawMessage).takeIf { it !is CQIgnoreEventDTO }?.apply {
                    val jsonToSend = this.toJson()
                    logger.debug("WS Reverse将要发送事件: $jsonToSend")

                    if (websocketSession.isActive) {
                        if (!EventFilter.eval(jsonToSend)) {
                            logger.debug("事件被Event Filter命中, 取消发送")
                        } else {
                            websocketSession.outgoing.send(Frame.Text(jsonToSend))
                        }
                    } else {
                        logger.warning("WS Reverse事件发送失败, 连接已被关闭, 尝试重连中 $httpClientKey")
                        subscriptions[httpClientKey]?.complete()
                        startGeneralWebsocketClient(session.bot, settings, clientType)
                    }
                }
            }
        }

        if (session.settings.heartbeat.enable) {
            heartbeatJobs[httpClientKey] = HeartbeatScope(EmptyCoroutineContext).launch {
                while (true) {
                    if (websocketSession.isActive) {
                        websocketSession.outgoing.send(
                            Frame.Text(
                                CQHeartbeatMetaEventDTO(
                                    session.botId,
                                    currentTimeSeconds,
                                    CQPluginStatusData(
                                        good = session.bot.isOnline,
                                        online = session.bot.isOnline
                                    ),
                                    session.settings.heartbeat.interval
                                ).toJson()
                            )
                        )
                        delay(session.settings.heartbeat.interval)
                    } else {
                        logger.warning("WS Reverse事件发送失败, 连接已被关闭, 尝试重连中 $httpClientKey")
                        subscriptions[httpClientKey]?.complete()
                        startGeneralWebsocketClient(session.bot, settings, clientType)
                        break
                    }
                }
            }
        }

        if (clientType != "Universal") websocketSession.incoming.consumeEach { logger.warning("WS Reverse Event 路由只负责发送事件, 不响应收到的请求") }
    }

    @OptIn(KtorExperimentalAPI::class, ExperimentalCoroutinesApi::class)
    private fun startWebsocketConnectivityCheck(
        bot: Bot,
        settings: PluginSettings.WebsocketReverseClientSettings,
        clientType: String
    ) {
        val httpClientKey = "${settings.reverseHost}:${settings.reversePort}-Client-$clientType"
        if (httpClientKey !in connectivityChecks) {
            connectivityChecks.add(httpClientKey)
            scope.launch {
                if (httpClients.containsKey(httpClientKey)) {
                    var stillActive = true
                    while (true) {
                        websocketSessions[httpClientKey]?.apply {
                            if (!this.isActive) {
                                stillActive = false
                                this.cancel()
                            }
                        }

                        if (!stillActive) {
                            heartbeatJobs[httpClientKey]?.apply { this.cancel() }
                            websocketSessions.remove(httpClientKey)
                            subscriptions[httpClientKey]?.apply {
                                this.complete()
                            }
                            subscriptions.remove(httpClientKey)
                            httpClients[httpClientKey].apply {
                                this?.close()
                            }
                            logger.warning("Websocket连接已断开, 将在${settings.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey")
                            delay(settings.reconnectInterval)
                            httpClients.remove(httpClientKey)
                            httpClients[httpClientKey] = HttpClient {
                                install(WebSockets)
                            }
                            startGeneralWebsocketClient(bot, settings, clientType)
                        }
                        delay(5000)
                    }
                }
            }
        }
    }

    fun close() {
        closing = true
        heartbeatJobs.forEach { it.value.cancel() }
        heartbeatJobs.clear()
        websocketSessions.forEach { it.value.cancel() }
        websocketSessions.clear()
        subscriptions.forEach { it.value?.complete() }
        subscriptions.clear()
        httpClients.forEach { it.value.close() }
        httpClients.clear()
        logger.info("反向Websocket模块已禁用")
    }
}