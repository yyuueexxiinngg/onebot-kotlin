package com.github.yyuueexxiinngg.onebot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.yamlkt.Comment

object PluginSettings : AutoSavePluginConfig("settings") {
    var proxy by value("")
    var db by value(DBSettings())
    var bots: MutableMap<String, BotSettings>? by value(mutableMapOf("12345654321" to BotSettings()))

    @Serializable
    data class DBSettings(
        var enable: Boolean = true,
//        @Comment("数据库的最大容量限制,单位GB,非正数视为无限制,超出此大小后旧记录将被删除")
//        var maxSize: Double = 0.0
    )

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
        var secret: String = "",
        @Comment("上报超时时间, 单位毫秒, 须大于0才会生效")
        var timeout: Long = 0L
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