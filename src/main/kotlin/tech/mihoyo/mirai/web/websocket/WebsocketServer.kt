package tech.mihoyo.mirai.web.websocket

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.Route
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.websocket.*
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import net.mamoe.mirai.console.plugins.ToBeRemoved
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.utils.currentTimeSeconds
import tech.mihoyo.mirai.BotSession
import tech.mihoyo.mirai.data.common.CQHeartbeatMetaEventDTO
import tech.mihoyo.mirai.data.common.CQIgnoreEventDTO
import tech.mihoyo.mirai.data.common.CQPluginStatusData
import tech.mihoyo.mirai.data.common.toCQDTO
import tech.mihoyo.mirai.util.logger
import tech.mihoyo.mirai.util.toJson
import tech.mihoyo.mirai.web.HeartbeatScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class WebsocketServerScope(coroutineContext: CoroutineContext) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineExceptionHandler { _, throwable ->
        logger.error("Exception in WebsocketServer", throwable)
    } + SupervisorJob()
}

@OptIn(ToBeRemoved::class)
class WebSocketServer(
    val session: BotSession
) {
    lateinit var server: ApplicationEngine
    private lateinit var serviceConfig: WebSocketServerServiceConfig

    init {
        if (session.config.exist("ws")) {
            serviceConfig = WebSocketServerServiceConfig(session.config.getConfigSection("ws"))
            logger.info("Bot: ${session.bot.id} 正向Websocket服务端是否配置开启: ${serviceConfig.enable}")
            if (serviceConfig.enable) {
                try {
                    server = embeddedServer(CIO, environment = applicationEngineEnvironment {
                        this.module { cqWebsocketServer(session, serviceConfig) }
                        connector {
                            this.host = serviceConfig.wsHost
                            this.port = serviceConfig.wsPort
                        }
                    })
                    server.start(false)
                } catch (e: Exception) {
                    logger.error("Bot:${session.bot.id} Websocket服务端模块启用失败")
                }
            }
        } else {
            logger.debug("${session.bot.id}未对ws进行配置")
        }
    }

    fun close() {
        server.stop(5000, 5000)
    }

}

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DuplicatedCode")
fun Application.cqWebsocketServer(session: BotSession, serviceConfig: WebSocketServerServiceConfig) {
    val scope = WebsocketServerScope(EmptyCoroutineContext)
    logger.debug("Bot: ${session.bot.id} 尝试开启正向Websocket服务端于端口: ${serviceConfig.wsPort}")
    install(DefaultHeaders)
    install(WebSockets)
    routing {
        logger.debug("Bot: ${session.bot.id} 正向Websocket服务端开始创建路由")
        val isRawMessage = serviceConfig.postMessageFormat != "array"
        cqWebsocket("/event", session, serviceConfig) { _session ->
            logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 /event 开始监听事件")
            val listener = _session.bot.subscribeAlways<BotEvent> {
                this.toCQDTO(isRawMessage).takeIf { it !is CQIgnoreEventDTO }?.apply {
                    val jsonToSend = this.toJson()
                    logger.debug("WS Server将要发送事件: $jsonToSend")
                    send(Frame.Text(jsonToSend))
                }
            }

            val heartbeatJob = if (session.heartbeatEnabled) emitHeartbeat(session, outgoing) else null

            try {
                incoming.consumeEach {}
            } finally {
                logger.info("Bot: ${session.bot.id} 正向Websocket服务端 /event 连接被关闭")
                listener.complete()
                heartbeatJob?.cancel()
            }
        }
        cqWebsocket("/api", session, serviceConfig) {
            try {
                logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 /api 开始处理API请求")
                incoming.consumeEach {
                    when (it) {
                        is Frame.Text -> {
                            scope.launch {
                                handleWebSocketActions(outgoing, session.cqApiImpl, it.readText())
                            }
                        }
                    }
                }
            } finally {
                logger.info("Bot: ${session.bot.id} 正向Websocket服务端 /api 连接被关闭")
            }
        }
        cqWebsocket("/", session, serviceConfig) { _session ->
            logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 / 开始监听事件")
            val listener = _session.bot.subscribeAlways<BotEvent> {
                this.toCQDTO(isRawMessage).takeIf { it !is CQIgnoreEventDTO }?.apply {
                    send(Frame.Text(this.toJson()))
                }
            }

            val heartbeatJob = if (session.heartbeatEnabled) emitHeartbeat(session, outgoing) else null

            try {
                logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 / 开始处理API请求")
                incoming.consumeEach {
                    when (it) {
                        is Frame.Text -> {
                            scope.launch {
                                handleWebSocketActions(outgoing, session.cqApiImpl, it.readText())
                            }
                        }
                    }
                }
            } finally {
                logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 / 连接被关闭")
                listener.complete()
                heartbeatJob?.cancel()
            }
        }
    }
}

private suspend fun emitHeartbeat(session: BotSession, outgoing: SendChannel<Frame>): Job {
    return HeartbeatScope(EmptyCoroutineContext).launch {
        while (true) {
            outgoing.send(
                Frame.Text(
                    CQHeartbeatMetaEventDTO(
                        session.botId,
                        currentTimeSeconds,
                        CQPluginStatusData(
                            good = session.bot.isOnline,
                            online = session.bot.isOnline
                        ),
                        session.heartbeatInterval
                    ).toJson()
                )
            )
            delay(session.heartbeatInterval)
        }
    }
}


private inline fun Route.cqWebsocket(
    path: String,
    session: BotSession,
    serviceConfig: WebSocketServerServiceConfig,
    crossinline body: suspend DefaultWebSocketServerSession.(BotSession) -> Unit
) {
    webSocket(path) {
        if (serviceConfig.accessToken != null && serviceConfig.accessToken != "") {
            val accessToken =
                call.parameters["access_token"] ?: call.request.headers["Authorization"]?.let {
                    Regex("""(?:[Tt]oken|Bearer)\s+(.*)""").find(it)?.groupValues?.get(1)
                }
            if (accessToken != serviceConfig.accessToken) {
                close(CloseReason(CloseReason.Codes.NORMAL, "accessToken不正确"))
                return@webSocket
            }
        }

        body(session)
    }
}

