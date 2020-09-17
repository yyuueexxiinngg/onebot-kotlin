package tech.mihoyo.mirai.web.websocket

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
import tech.mihoyo.mirai.BotSession
import tech.mihoyo.mirai.Settings
import tech.mihoyo.mirai.WsReverseConfig
import tech.mihoyo.mirai.data.common.*
import tech.mihoyo.mirai.util.logger
import tech.mihoyo.mirai.util.toJson
import tech.mihoyo.mirai.web.HeartbeatScope
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
    val session: BotSession
) {
    private val httpClients: MutableMap<String, HttpClient> = mutableMapOf()
    private var subscriptions: MutableMap<String, Listener<BotEvent>?> = mutableMapOf()
    private var websocketSessions: MutableMap<String, DefaultClientWebSocketSession> = mutableMapOf()
    private var heartbeatJobs: MutableMap<String, Job> = mutableMapOf()
    private var connectivityChecks: MutableList<String> = mutableListOf()
    private var closing = false
    private val scope = WebsocketReverseClientScope(EmptyCoroutineContext)

    init {
        if (session.config.ws_reverse.enable) {
            val serviceConfig = session.config.ws_reverse
            logger.debug("Host: ${serviceConfig.reverseHost}, Port: ${serviceConfig.reversePort}, Enable: ${serviceConfig.enable}, Use Universal: ${serviceConfig.useUniversal}")
                if (serviceConfig.enable) {
                    if (serviceConfig.useUniversal) {
                        scope.launch {
                            startGeneralWebsocketClient(session.bot, serviceConfig, "Universal")
                        }
                    } else {
                        scope.launch {
                            startGeneralWebsocketClient(session.bot, serviceConfig, "Api")
                        }
                        scope.launch {
                            startGeneralWebsocketClient(session.bot, serviceConfig, "Event")
                        }
                    }
                }
        } else {
            logger.debug("${session.bot.id}未对ws_reverse进行配置")
        }
    }

    @OptIn(KtorExperimentalAPI::class, ExperimentalCoroutinesApi::class)
    @Suppress("DuplicatedCode")
    private suspend fun startGeneralWebsocketClient(
        bot: Bot,
        config: WsReverseConfig,
        clientType: String
    ) {
        val httpClientKey = "${config.reverseHost}:${config.reversePort}-Client-$clientType"

        httpClients[httpClientKey] = HttpClient {
            install(WebSockets)
        }

        logger.debug("$httpClientKey 开始启动")
        val path = when (clientType) {
            "Api" -> config.reverseApiPath
            "Event" -> config.reverseEventPath
            else -> config.reversePath
        }

        try {
            if (!config.useTLS) {
                httpClients[httpClientKey]!!.ws(
                    host = config.reverseHost,
                    port = config.reversePort,
                    path = path,
                    request = {
                        header("User-Agent", "CQHttp/4.15.0")
                        header("X-Self-ID", bot.id.toString())
                        header("X-Client-Role", clientType)
                        config.accessToken?.let {
                            if (it != "") {
                                header(
                                    "Authorization",
                                    "Token ${config.accessToken}"
                                )
                            }
                        }
                    }
                ) {
                    // 用来检测Websocket连接是否关闭
                    websocketSessions[httpClientKey] = this
                    if (!subscriptions.containsKey(httpClientKey)) {
                        startWebsocketConnectivityCheck(bot, config, clientType)
                        logger.debug("$httpClientKey Websocket Client启动完毕")
                        when (clientType) {
                            "Api" -> listenApi(incoming, outgoing)
                            "Event" -> listenEvent(httpClientKey, config, this, clientType)
                            "Universal" -> {
                                listenEvent(httpClientKey, config, this, clientType)
                                listenApi(incoming, outgoing)
                            }
                        }
                    } else {
                        logger.warning("Websocket session alredy exist, $httpClientKey")
                    }
                }
            } else {
                httpClients[httpClientKey]!!.wss(
                    host = config.reverseHost,
                    port = config.reversePort,
                    path = path,
                    request = {
                        header("User-Agent", "CQHttp/4.15.0")
                        header("X-Self-ID", bot.id.toString())
                        header("X-Client-Role", clientType)
                        config.accessToken?.let {
                            if (it != "") {
                                header(
                                    "Authorization",
                                    "Token ${config.accessToken}"
                                )
                            }
                        }
                    }
                ) {
                    // 用来检测Websocket连接是否关闭
                    websocketSessions[httpClientKey] = this
                    if (!subscriptions.containsKey(httpClientKey)) {
                        startWebsocketConnectivityCheck(bot, config, clientType)
                        logger.debug("$httpClientKey Websocket Client启动完毕")
                        when (clientType) {
                            "Api" -> listenApi(incoming, outgoing)
                            "Event" -> listenEvent(httpClientKey, config, this, clientType)
                            "Universal" -> {
                                listenEvent(httpClientKey, config, this, clientType)
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
                    logger.warning("Websocket连接出错, 请检查服务器是否开启并确认正确监听端口, 将在${config.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey Path: $path")
                }
                is EOFException -> {
                    logger.warning("Websocket连接出错, 服务器返回数据不正确, 请检查Websocket服务器是否配置正确, 将在${config.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey Path: $path")
                }
                is IOException -> {
                    logger.warning("Websocket连接出错, 可能被服务器关闭, 将在${config.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey Path: $path")
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
            delay(config.reconnectInterval.toLong())
            if (!closing) startGeneralWebsocketClient(session.bot, config, clientType)
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
        config: WsReverseConfig,
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
        val isRawMessage = config.postMessageFormat != "array"
        subscriptions[httpClientKey] = session.bot.subscribeAlways {
            // 保存Event以便在WebsocketSession Block中使用
            if (this.bot.id == session.botId) {
                this.toCQDTO(isRawMessage = isRawMessage).takeIf { it !is CQIgnoreEventDTO }?.apply {
                    val jsonToSend = this.toJson()
                    logger.debug("WS Reverse将要发送事件: $jsonToSend")
                    if (websocketSession.isActive) {
                        websocketSession.outgoing.send(Frame.Text(jsonToSend))
                    } else {
                        logger.warning("WS Reverse事件发送失败, 连接已被关闭, 尝试重连中 $httpClientKey")
                        subscriptions[httpClientKey]?.complete()
                        startGeneralWebsocketClient(session.bot, config, clientType)
                    }
                }
            }
        }

        if (session.heartbeatEnabled) {
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
                                    session.heartbeatInterval.toLong()
                                ).toJson()
                            )
                        )
                        delay(session.heartbeatInterval.toLong())
                    } else {
                        logger.warning("WS Reverse事件发送失败, 连接已被关闭, 尝试重连中 $httpClientKey")
                        subscriptions[httpClientKey]?.complete()
                        startGeneralWebsocketClient(session.bot, config, clientType)
                        break
                    }
                }
            }
        }

        if (clientType != "Universal") websocketSession.incoming.consumeEach { logger.warning("WS Reverse Event 路由只负责发送事件, 不响应收到的请求") }
    }

    @OptIn(KtorExperimentalAPI::class, ExperimentalCoroutinesApi::class)
    private fun startWebsocketConnectivityCheck(bot: Bot, config: WsReverseConfig, clientType: String) {
        val httpClientKey = "${config.reverseHost}:${config.reversePort}-Client-$clientType"
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
                            logger.warning("Websocket连接已断开, 将在${config.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey")
                            delay(config.reconnectInterval.toLong())
                            httpClients.remove(httpClientKey)
                            httpClients[httpClientKey] = HttpClient {
                                install(WebSockets)
                            }
                            startGeneralWebsocketClient(bot, config, clientType)
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