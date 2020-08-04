package tech.mihoyo.mirai.web.websocket

import net.mamoe.mirai.console.plugins.ConfigSection

class WebSocketServerServiceConfig(serviceConfig: ConfigSection) {
    val enable: Boolean by serviceConfig.withDefault { false }

    val postMessageFormat: String by serviceConfig.withDefault { "string" }

    val wsHost: String by serviceConfig.withDefault { "0.0.0.0" }

    val wsPort: Int by serviceConfig.withDefault { 8080 }

    val accessToken: String? by serviceConfig.withDefault { "" }
}