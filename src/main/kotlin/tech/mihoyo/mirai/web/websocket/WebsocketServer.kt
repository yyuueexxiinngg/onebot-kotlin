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
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.*
import io.ktor.websocket.*
import io.ktor.websocket.WebSockets
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.subscribeAlways
import tech.mihoyo.mirai.BotSession
import tech.mihoyo.mirai.data.common.CQIgnoreEventDTO
import tech.mihoyo.mirai.data.common.toCQDTO
import tech.mihoyo.mirai.util.logger
import tech.mihoyo.mirai.util.toJson

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class WebSocketServer(
    val session: BotSession
) {
    lateinit var server: ApplicationEngine
    private var serviceConfig = WebSocketServerServiceConfig(session.config.getConfigSection("ws"))

    init {
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
    }

    fun close() {
        server.stop(5000, 5000)
    }

}

@ExperimentalCoroutinesApi
fun Application.cqWebsocketServer(session: BotSession, serviceConfig: WebSocketServerServiceConfig) {
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
                    send(Frame.Text(this.toJson()))
                }
            }
            try {
                for (frame in incoming) {
                    outgoing.send(frame)
                }
            } finally {
                listener.complete()
            }
        }
        cqWebsocket("/api", session, serviceConfig) {
            logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 /api 开始处理API请求")
            incoming.consumeEach {
                when (it) {
                    is Frame.Text -> {
                        handleWebSocketActions(outgoing, session.cqApiImpl, it.readText())
                    }
                }
            }
        }
        cqWebsocket("/", session, serviceConfig) { _session ->
            logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 / 开始监听事件")
            val listener = _session.bot.subscribeAlways<BotEvent> {
                this.toCQDTO(isRawMessage).takeIf { it !is CQIgnoreEventDTO }?.apply {
                    send(Frame.Text(this.toJson()))
                }
            }
            try {
                for (frame in incoming) {
                    outgoing.send(frame)
                }
            } finally {
                listener.complete()
            }
            logger.debug("Bot: ${session.bot.id} 正向Websocket服务端 / 开始处理API请求")
            incoming.consumeEach {
                when (it) {
                    is Frame.Text -> {
                        handleWebSocketActions(outgoing, session.cqApiImpl, it.readText())
                    }
                }
            }
        }
    }
}

@ContextDsl
private inline fun Route.cqWebsocket(
    path: String,
    session: BotSession,
    serviceConfig: WebSocketServerServiceConfig,
    crossinline body: suspend DefaultWebSocketServerSession.(BotSession) -> Unit
) {
    webSocket(path) {
        if (serviceConfig.accessToken != null && serviceConfig.accessToken != "") {
            val accessToken = call.parameters["access_token"]
            if (accessToken != serviceConfig.accessToken) {
                close(CloseReason(CloseReason.Codes.NORMAL, "accessToken不正确"))
                return@webSocket
            }
        }

        body(session)
    }
}

