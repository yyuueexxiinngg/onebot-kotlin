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
import kotlinx.coroutines.channels.consumeEach
import net.mamoe.mirai.Bot

import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.utils.currentTimeMillis
import tech.mihoyo.mirai.MiraiApi
import tech.mihoyo.mirai.data.common.*
import tech.mihoyo.mirai.web.HttpApiService
import tech.mihoyo.mirai.util.toJson
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException

class WebSocketReverseClient(
    /**
     * 插件对象
     */
    override val console: PluginBase
) : HttpApiService {
    /**
     * 反向Websocket配置, 一个Bot对应一个
     */
    private val configByBots: MutableMap<Long, WebSocketReverseClientConfig> = mutableMapOf()

    /**
     * Bot上线下线事件监听器
     */
    private var initialSubscription: Listener<BotEvent>? = null

    /**
     * 事件监听器, 一个Bot对应一个
     */
    private var subscriptionByBots: MutableMap<Long, Listener<BotEvent>?> = mutableMapOf()

    /**
     * Http 客户端, 一个Bot对应一个, 用以重连Websocket
     */
    private var httpClientByBots: MutableMap<Long, HttpClient> = mutableMapOf()

    /**
     * Websocket 会话, 一个Bot对应一个, 用以重连Websocket
     */
    private var websocketSessionByBots: MutableMap<Long, DefaultClientWebSocketSession> = mutableMapOf()

    override fun onLoad() {
    }

    @ExperimentalCoroutinesApi
    @KtorExperimentalAPI
    override fun onEnable() {
        initialSubscription = console.subscribeAlways {
            when (this) {
                is BotOnlineEvent -> {
                    // 初始化WebsocketSession并保存至Map
                    if (!configByBots.containsKey(bot.id)) {
                        // 获取当前Bot对应Config
                        val config = WebSocketReverseClientConfig(console.loadConfig("setting.yml"), bot)
                        configByBots[bot.id] = config
                        console.logger.info("Bot:${bot.id} 反向Websocket模块启用状态: ${config.enable}")
                        if (config.enable) {
                            httpClientByBots[bot.id] = HttpClient {
                                install(WebSockets)
                            }
                            GlobalScope.launch {
                                startWebsocketClient(bot)
                            }
                        }
                    }
                }
            }
        }

    }

    @KtorExperimentalAPI
    @ExperimentalCoroutinesApi
    private suspend fun startWebsocketClient(bot: Bot) {
        httpClientByBots[bot.id]?.apply {
            val config = configByBots[bot.id]!!
            val isRawMessage = config.postMessageFormat != "array"
            // 为Bot创建Adapter Api
            val mirai = MiraiApi(bot)
            try {
                val client = this
                client.ws(
                    host = config.reverseHost,
                    port = config.reversePort,
                    path = config.reversePath,
                    request = {
                        header("User-Agent", "MiraiHttp/0.1.0")
                        header("X-Self-ID", bot.id.toString())
                        header("X-Client-Role", "Universal")
                        config.accessToken?.let { header("Authorization", "Token ${config.accessToken}") }
                    }
                ) {
                    // 用来检测Websocket连接是否关闭
                    websocketSessionByBots[bot.id] = this
                    // 构建事件监听器
                    if (!subscriptionByBots.containsKey(bot.id)) {
                        // 通知服务方链接建立
                        send(Frame.Text(CQMetaEventDTO(bot.id, "connect", currentTimeMillis).toJson()))
                        subscriptionByBots[bot.id] = console.subscribeAlways {
                            // 保存Event以便在WebsocketSession Block中使用
                            val event = this
                            if (event is TempMessageEvent) mirai.cachedTempContact[event.sender.id] =
                                event.group.id
                            event.toCQDTO(isRawMessage = isRawMessage).takeIf { it !is CQIgnoreEventDTO }?.apply {
                                send(Frame.Text(this.toJson()))
                            }
                        }

                        startWebsocketConnectivityCheck(bot)

                        incoming.consumeEach {
                            when (it) {
                                is Frame.Text -> {
                                    handleWebSocketActions(outgoing, mirai, it.readText())
                                }
                                else -> console.logger.warning("Unsupported incomeing frame")
                            }
                        }

                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is ConnectException -> {
                        console.logger.warning("Websocket连接出错, 请检查服务器是否开启并确认正确监听端口, 将在${config.reconnectInterval / 1000}秒后重试连接")
                        delay(config.reconnectInterval)
                        startWebsocketClient(bot)
                    }
                    is EOFException -> {
                        console.logger.warning("Websocket连接出错, 服务器返回数据不正确, 请检查Websocket服务器是否配置正确, 将在${config.reconnectInterval / 1000}秒后重试连接")
                        delay(config.reconnectInterval)
                        startWebsocketClient(bot)
                    }
                    is IOException -> {
                        console.logger.warning("Websocket连接出错, 可能被服务器关闭, 将在${config.reconnectInterval / 1000}秒后重试连接")
                        delay(config.reconnectInterval)
                        startWebsocketClient(bot)
                    }
                    else -> console.logger.warning("Websocket连接出错, 未知错误, 放弃重试连接, 请检查配置正确后重启mirai  " + e.message + e.cause)
                }
            }
        }
    }

    @KtorExperimentalAPI
    @ExperimentalCoroutinesApi
    private fun startWebsocketConnectivityCheck(bot: Bot) {
        GlobalScope.launch {
            if (httpClientByBots.containsKey(bot.id)) {
                val config = configByBots[bot.id]!!
                var stillActive = true
                while (true) {
                    websocketSessionByBots[bot.id]?.apply {
                        if (!this.isActive) {
                            stillActive = false
                            this.cancel()
                        }
                    }

                    if (!stillActive) {
                        websocketSessionByBots.remove(bot.id)
                        subscriptionByBots[bot.id]?.apply {
                            this.complete()
                        }
                        subscriptionByBots.remove(bot.id)
                        httpClientByBots[bot.id].apply {
                            this!!.close()
                        }
                        console.logger.warning("Websocket连接已断开, 将在${config.reconnectInterval / 1000}秒后重试连接")
                        delay(config.reconnectInterval)
                        httpClientByBots[bot.id] = HttpClient {
                            install(WebSockets)
                        }
                        startWebsocketClient(bot)
                        break
                    }
                    delay(5000)
                }
                startWebsocketConnectivityCheck(bot)
            }
        }
    }

    override fun onDisable() {
        initialSubscription?.complete()
        websocketSessionByBots.forEach { it.value.cancel() }
        websocketSessionByBots.clear()
        subscriptionByBots.forEach { it.value?.complete() }
        subscriptionByBots.clear()
        httpClientByBots.forEach { it.value.close() }
        httpClientByBots.clear()
        console.logger.info("反向Websocket模块已禁用")
    }
}