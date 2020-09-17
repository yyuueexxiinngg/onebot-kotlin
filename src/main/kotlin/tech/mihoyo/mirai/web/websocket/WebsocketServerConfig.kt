package tech.mihoyo.mirai.web.websocket

import tech.mihoyo.mirai.util.ConfigSection

class WebSocketServerServiceConfig(serviceConfig: ConfigSection) {
    val enable: Boolean by serviceConfig.withDefault { false }

    val postMessageFormat: String by serviceConfig.withDefault { "string" }

    val wsHost: String by serviceConfig.withDefault { "0.0.0.0" }

    val wsPort: Int by serviceConfig.withDefault { 6700 }

    val accessToken: String? by serviceConfig.withDefault { "" }
}