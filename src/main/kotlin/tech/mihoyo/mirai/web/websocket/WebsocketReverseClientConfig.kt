package tech.mihoyo.mirai.web.websocket

import tech.mihoyo.mirai.util.ConfigSection

class WebSocketReverseServiceConfig(serviceConfig: ConfigSection) {
    /**
     * 是否开启
     */
    val enable: Boolean by serviceConfig.withDefault { false }

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
    val accessToken: String? by serviceConfig.withDefault { "" }

    /**
     * 反向Websocket端口
     */
    val reversePort: Int by serviceConfig.withDefault { 8080 }

    /**
     * 反向Websocket路径
     */
    val reversePath: String by serviceConfig.withDefault { "/ws" }

    /**
     * 反向Websocket Api路径  尚未实现
     */
    val reverseApiPath: String by serviceConfig.withDefault { reversePath }

    /**
     * 反向Websocket Event路径 尚未实现
     */
    val reverseEventPath: String by serviceConfig.withDefault { reversePath }

    /**
     * 是否使用Universal客户端
     */
    val useUniversal: Boolean by serviceConfig.withDefault { true }

    /**
     * 反向 WebSocket 客户端断线重连间隔
     */
    val reconnectInterval: Long by serviceConfig.withDefault { 3000 }

    /**
     * 反向 WebSocket 客户端是否通过HTTPS连接
     */
    val useTLS: Boolean by serviceConfig.withDefault { false }

}