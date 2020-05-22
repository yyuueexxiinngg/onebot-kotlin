package tech.mihoyo.mirai.web.websocket

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugins.Config

class WebSocketReverseClientConfig(config: Config, bot: Bot) {
    /**
     * 当前Bot的总配置
     */
    @Suppress("UNCHECKED_CAST")
    private val  botConfig = config[bot.id.toString()] as? Map<String, Any> ?: emptyMap()

    /**
     * Bot对应反向Websocket配置
     */
    @Suppress("UNCHECKED_CAST")
    private val serviceConfig = botConfig["ws_reverse"] as? Map<String, Any> ?: emptyMap()

    /**
     * 是否开启
     */
    val enable: Boolean by serviceConfig.withDefault { true }

    /**
     * 上报消息格式
     */
    val postMessageFormat: String by serviceConfig.withDefault { "string" }

    /**
     * 反向Websocket主机
     */
    val reverseHost: String by serviceConfig.withDefault { "127.0.0.1" }

    /**
     * 反向Websocket口令
     */
    val accessToken: String? by serviceConfig.withDefault { null }

    /**
     * 反向Websocket端口
     */
    val reversePort: Int by serviceConfig.withDefault { 8080 }

    /**
     * 反向Websocket路径
     */
    val reversePath: String by serviceConfig.withDefault { "/ws" }

    /**
     * 反向 WebSocket 客户端断线重连间隔
     */
    val reconnectInterval: Long by serviceConfig.withDefault { 3000 }

    /**
     * 反向Websocket Api路径  尚未实现
     */
//    val reverseApiPath: String by botConfig.withDefault { "/ws" }

    /**
     * 反向Websocket Event路径 尚未实现
     */
//    val reverseEventPath: String by botConfig.withDefault { "/ws" }

}