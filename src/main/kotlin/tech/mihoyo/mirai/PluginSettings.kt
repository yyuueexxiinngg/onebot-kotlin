package tech.mihoyo.mirai

import kotlinx.serialization.SerialName
import net.mamoe.mirai.console.data.*
import kotlinx.serialization.Serializable

object PluginSettings : AutoSavePluginConfig("settings") {
    var debug by value(false)
    var proxy by value("")
    var bots: MutableMap<String, BotSettings>? by value(mutableMapOf("12345654321" to BotSettings()))

    @Serializable
    data class BotSettings(
        var cacheImage: Boolean = false,
        var cacheRecord: Boolean = false,
        var heartbeat: HeartbeatSettings = HeartbeatSettings(),
        var http: HTTPSettings = HTTPSettings(),
        @SerialName("ws_reverse")
        var wsReverse: MutableList<WebsocketReverseClientSettings> = mutableListOf(WebsocketReverseClientSettings()),
        var ws: WebsocketServerSettings = WebsocketServerSettings()
    )

    @Serializable
    data class HeartbeatSettings(
        var enable: Boolean = false,
        var interval: Long = 1500L
    )

    @Serializable
    data class HTTPSettings(
        var enable: Boolean = false,
        var host: String = "0.0.0.0",
        var port: Int = 5700,
        var accessToken: String = "",
        var postUrl: String = "",
        var postMessageFormat: String = "string",
        var secret: String = ""
    )

    @Serializable
    data class WebsocketReverseClientSettings(
        var enable: Boolean = false,
        var postMessageFormat: String = "string",
        var reverseHost: String = "127.0.0.1",
        var reversePort: Int = 8080,
        var accessToken: String = "",
        var reversePath: String = "/ws",
        var reverseApiPath: String = "/api",
        var reverseEventPath: String = "/event",
        var useUniversal: Boolean = true,
        var useTLS: Boolean = false,
        var reconnectInterval: Long = 3000L,
    )

    @Serializable
    data class WebsocketServerSettings(
        var enable: Boolean = false,
        var postMessageFormat: String = "string",
        var wsHost: String = "0.0.0.0",
        var wsPort: Int = 6700,
        var accessToken: String = ""
    )
}