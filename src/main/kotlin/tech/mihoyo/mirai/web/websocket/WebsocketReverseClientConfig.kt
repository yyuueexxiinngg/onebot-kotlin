package tech.mihoyo.mirai.web.websocket

import tech.mihoyo.mirai.WsReverseConfig

class WebSocketReverseServiceConfig(serviceConfig: WsReverseConfig) {
    /**
     * 是否开启
     */
    val enable: Boolean by lazy {serviceConfig.enable}

    /**
     * 上报消息格式
     */
    val postMessageFormat: String by lazy {serviceConfig.postMessageFormat}

    /**
     * 反向Websocket主机
     */
    val reverseHost: String by lazy {serviceConfig.reverseHost}

    /**
     * 反向Websocket口令
     */
    val accessToken: String? by lazy {serviceConfig.accessToken}

    /**
     * 反向Websocket端口
     */
    val reversePort: Int by lazy {serviceConfig.reversePort}

    /**
     * 反向Websocket路径
     */
    val reversePath: String by lazy {serviceConfig.reversePath}

    /**
     * 反向Websocket Api路径  尚未实现
     */
    val reverseApiPath: String by lazy {serviceConfig.reversePath}

    /**
     * 反向Websocket Event路径 尚未实现
     */
    val reverseEventPath: String by lazy {serviceConfig.reversePath}

    /**
     * 是否使用Universal客户端
     */
    val useUniversal: Boolean by lazy {serviceConfig.useUniversal}

    /**
     * 反向 WebSocket 客户端断线重连间隔
     */
    val reconnectInterval: Long by lazy {serviceConfig.reconnectInterval.toLong()}

    /**
     * 反向 WebSocket 客户端是否通过HTTPS连接
     */
    val useTLS: Boolean by lazy {serviceConfig.useTLS}

}