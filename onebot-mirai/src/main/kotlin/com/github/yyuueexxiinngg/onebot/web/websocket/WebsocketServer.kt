package com.github.yyuueexxiinngg.onebot.web.websocket

import com.github.yyuueexxiinngg.onebot.BotSession
import com.github.yyuueexxiinngg.onebot.PluginSettings
import com.github.yyuueexxiinngg.onebot.data.common.CQHeartbeatMetaEventDTO
import com.github.yyuueexxiinngg.onebot.data.common.CQPluginStatusData
import com.github.yyuueexxiinngg.onebot.logger
import com.github.yyuueexxiinngg.onebot.util.currentTimeSeconds
import com.github.yyuueexxiinngg.onebot.util.toJson
import com.github.yyuueexxiinngg.onebot.web.HeartbeatScope
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class WebsocketServerScope(coroutineContext: CoroutineContext) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineExceptionHandler { _, throwable ->
        logger.error("Exception in WebsocketServer", throwable)
    } + SupervisorJob()
}

@OptIn(KtorExperimentalAPI::class)
class WebSocketServer(
    private val session: BotSession
) {
    private lateinit var server: ApplicationEngine

    init {
        val settings = session.settings.ws
        logger.info("Bot: ${session.bot.id} 正向Websocket服务端是否配置开启: ${settings.enable}")
        if (settings.enable) {
            try {
                server = embeddedServer(CIO, environment = applicationEngineEnvironment {
                    this.module { cqWebsocketServer(session, settings) }
                    connector {
                        this.host = settings.wsHost
                        this.port = settings.wsPort
                    }
                })
                server.start(false)
            } catch (e: Exception) {
                logger.error("Bot:${session.bot.id} Websocket服务端模块启用失败")
            }
        }

    }

    fun close() {
        server.stop(5000, 5000)
    }

}

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("DuplicatedCode")
fun Application.cqWebsocketServer(session: BotSession, settings: PluginSettings.WebsocketServerSettings) {
    val scope = WebsocketServerScope(EmptyCoroutineContext)
    logger.debug("Bot: ${session.bot.id} 尝试开启正向Websocket服务端于端口: ${settings.wsPort}")
    install(DefaultHeaders)
    install(WebSockets)
    routing {
        logger.debug("Bot: ${session.bot.id} 正向Websocket服务端开始创建路由")
        val isRawMessage = settings.postMessageFormat != "array"
        cqWebsocket("/event", session, settings) { _ ->
            logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 /event 开始监听事件")
            val listener = session.subscribeEvent(
                {
                    send(Frame.Text(it))
                },
                isRawMessage
            )

            val heartbeatJob = if (session.settings.heartbeat.enable) emitHeartbeat(session, outgoing) else null

            try {
                incoming.consumeEach { logger.warning("WS Server Event 路由只负责发送事件, 不响应收到的请求") }
            } finally {
                logger.info("Bot: ${session.bot.id} 正向Websocket服务端 /event 连接被关闭")
                session.unsubscribeEvent(listener, isRawMessage)
                heartbeatJob?.cancel()
            }
        }
        cqWebsocket("/api", session, settings) {
            try {
                logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 /api 开始处理API请求")
                incoming.consumeEach {
                    if (it is Frame.Text) {
                        scope.launch {
                            handleWebSocketActions(outgoing, session.cqApiImpl, it.readText())
                        }
                    }
                }
            } finally {
                logger.info("Bot: ${session.bot.id} 正向Websocket服务端 /api 连接被关闭")
            }
        }
        cqWebsocket("/", session, settings) { _ ->
            logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 / 开始监听事件")
            val listener = session.subscribeEvent(
                {
                    send(Frame.Text(it))
                },
                isRawMessage
            )

            val heartbeatJob = if (session.settings.heartbeat.enable) emitHeartbeat(session, outgoing) else null

            try {
                logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 / 开始处理API请求")
                incoming.consumeEach {
                    if (it is Frame.Text) {
                        scope.launch {
                            handleWebSocketActions(outgoing, session.cqApiImpl, it.readText())
                        }
                    }
                }
            } finally {
                logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 / 连接被关闭")
                session.unsubscribeEvent(listener, isRawMessage)
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
                        currentTimeSeconds(),
                        CQPluginStatusData(
                            good = session.bot.isOnline,
                            online = session.bot.isOnline
                        ),
                        session.settings.heartbeat.interval
                    ).toJson()
                )
            )
            delay(session.settings.heartbeat.interval)
        }
    }
}


private inline fun Route.cqWebsocket(
    path: String,
    session: BotSession,
    settings: PluginSettings.WebsocketServerSettings,
    crossinline body: suspend DefaultWebSocketServerSession.(BotSession) -> Unit
) {
    webSocket(path) {
        if (settings.accessToken != "") {
            val accessToken =
                call.parameters["access_token"] ?: call.request.headers["Authorization"]?.let {
                    Regex("""(?:[Tt]oken|Bearer)\s+(.*)""").find(it)?.groupValues?.get(1)
                }
            if (accessToken != settings.accessToken) {
                close(CloseReason(CloseReason.Codes.NORMAL, "accessToken不正确"))
                return@webSocket
            }
        }

        body(session)
    }
}

