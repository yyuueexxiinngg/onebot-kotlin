package tech.mihoyo.mirai.web.websocket

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.ws
import io.ktor.client.request.header

import io.ktor.http.cio.websocket.Frame
import io.ktor.client.features.websocket.WebSockets
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import net.mamoe.mirai.Bot

import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.utils.currentTimeMillis
import tech.mihoyo.mirai.BotSession
import tech.mihoyo.mirai.data.common.*
import tech.mihoyo.mirai.util.logger
import tech.mihoyo.mirai.util.toJson
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class WebSocketReverseClient(
    val session: BotSession
) {
    private val httpClients: MutableMap<String, HttpClient> = mutableMapOf()
    private var serviceConfig: List<WebSocketReverseServiceConfig> = mutableListOf()
    private var subscriptions: MutableMap<String, Listener<BotEvent>?> = mutableMapOf()
    private var websocketSessions: MutableMap<String, DefaultClientWebSocketSession> = mutableMapOf()
    private var connectivityChecks: MutableList<String> = mutableListOf()

    init {
        if (session.config.exist("ws_reverse")) {
            serviceConfig = session.config.getConfigSectionList("ws_reverse").map { WebSocketReverseServiceConfig(it) }
            serviceConfig.forEach {
                logger.debug("Host: ${it.reverseHost}, Port: ${it.reversePort}, Enable: ${it.enable}, Use Universal: ${it.useUniversal}")
                if (it.enable) {
                    GlobalScope.launch {
                        if (it.useUniversal) {
                            startGeneralWebsocketClient(session.bot, it, "Universal")
                        } else {
                            startGeneralWebsocketClient(session.bot, it, "Api")
                            startGeneralWebsocketClient(session.bot, it, "Event")
                        }
                    }
                }
            }
        } else {
            logger.debug("${session.bot.id}未对ws_reverse进行配置")
        }
    }

    private suspend fun startGeneralWebsocketClient(
        bot: Bot,
        config: WebSocketReverseServiceConfig,
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
                    // 通知服务方链接建立
                    send(Frame.Text(CQMetaEventDTO(bot.id, "connect", currentTimeMillis).toJson()))
                    startWebsocketConnectivityCheck(bot, config, clientType)
                    logger.debug("$httpClientKey Websocket Client启动完毕")
                    when (clientType) {
                        "Api" -> listenApi(incoming, outgoing)
                        "Event" -> listenEvent(httpClientKey, config, outgoing)
                        "Universal" -> {
                            listenEvent(httpClientKey, config, outgoing)
                            listenApi(incoming, outgoing)
                        }
                    }
                } else {
                    logger.warning("Websocket session alredy exist, $httpClientKey")
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
                is CancellationException -> logger.info("Websocket连接关闭中, Host: $httpClientKey Path: $path")
                else -> {
                    logger.warning("Websocket连接出错, 未知错误, 请检查配置, 如配置错误请修正后重启mirai " + e.message + e.javaClass.name)
                }
            }
            httpClients[httpClientKey]?.apply { this.close() }
            httpClients.remove(httpClientKey)
            delay(config.reconnectInterval)
            startGeneralWebsocketClient(session.bot, config, clientType)
        }
    }

    private suspend fun listenApi(incoming: ReceiveChannel<Frame>, outgoing: SendChannel<Frame>) {
        incoming.consumeEach {
            when (it) {
                is Frame.Text -> {
                    handleWebSocketActions(outgoing, session.cqApiImpl, it.readText())
                }
                else -> logger.warning("Unsupported incomeing frame")
            }
        }
    }

    private suspend fun listenEvent(
        httpClientKey: String,
        config: WebSocketReverseServiceConfig,
        outgoing: SendChannel<Frame>
    ) {
        val isRawMessage = config.postMessageFormat != "array"
        subscriptions[httpClientKey] = session.bot.subscribeAlways {
            // 保存Event以便在WebsocketSession Block中使用
            if (this.bot.id == session.botId) {
                val event = this
                when (event) {
                    is TempMessageEvent -> session.cqApiImpl.cachedTempContact[event.sender.id] =
                        event.group.id
                    is NewFriendRequestEvent -> session.cqApiImpl.cacheRequestQueue.add(event)
                    is MemberJoinRequestEvent -> session.cqApiImpl.cacheRequestQueue.add(event)
                }
                event.toCQDTO(isRawMessage = isRawMessage).takeIf { it !is CQIgnoreEventDTO }?.apply {
                    outgoing.send(Frame.Text(this.toJson()))
                }
            }
        }
    }

    @KtorExperimentalAPI
    @ExperimentalCoroutinesApi
    private fun startWebsocketConnectivityCheck(bot: Bot, config: WebSocketReverseServiceConfig, clientType: String) {
        val httpClientKey = "${config.reverseHost}:${config.reversePort}-Client-$clientType"
        if (httpClientKey !in connectivityChecks) {
            GlobalScope.launch {
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
                            websocketSessions.remove(httpClientKey)
                            subscriptions[httpClientKey]?.apply {
                                this.complete()
                            }
                            subscriptions.remove(httpClientKey)
                            httpClients[httpClientKey].apply {
                                this!!.close()
                            }
                            logger.warning("Websocket连接已断开, 将在${config.reconnectInterval / 1000}秒后重试连接, Host: $httpClientKey")
                            delay(config.reconnectInterval)
                            httpClients.remove(httpClientKey)
                            httpClients[httpClientKey] = HttpClient {
                                install(WebSockets)
                            }
                            startGeneralWebsocketClient(bot, config, clientType)
                        }
                    }
                }
            }
        }
    }

    fun close() {
        websocketSessions.forEach { it.value.cancel() }
        websocketSessions.clear()
        subscriptions.forEach { it.value?.complete() }
        subscriptions.clear()
        httpClients.forEach { it.value.close() }
        httpClients.clear()
        logger.info("反向Websocket模块已禁用")
    }
}