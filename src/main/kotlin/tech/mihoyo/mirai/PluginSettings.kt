package tech.mihoyo.mirai

import net.mamoe.mirai.console.data.*
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.util.ConsoleExperimentalAPI

@OptIn(ExperimentalPluginConfig::class, ConsoleExperimentalAPI::class)
@Serializable
object PluginSettings : AutoSavePluginConfig() {
    var debug by value(false)
    var proxy by value("")
    var bots: MutableMap<String, BotSettings>? by value()

    @Serializable
    object BotSettings {
        var cacheImage by value(false)
        var cacheRecord by value(false)
        var heartbeat: HeartbeatSettings by value()
        var http: HTTPSettings by value()
        @ValueName("ws_reverse")
        var wsReverse: MutableList<WebsocketReverseClientSettings>? by value()
        var ws: WebsocketServerSettings by value()
    }

    @Serializable
    object HeartbeatSettings {
        var enable by value(false)
        var interval by value(15000L)
    }

    @Serializable
    object HTTPSettings {
        var enable by value(false)
        var host by value("0.0.0.0")
        var port by value(5700)
        var accessToken by value("")

        var postMessageFormat by value("string")
        var postUrl by value("")
        var secret by value("")
    }

    @Serializable
    object WebsocketReverseClientSettings {
        var enable by value(false)
        var postMessageFormat by value("string")
        var reverseHost by value("127.0.0.1")
        var reversePort by value(8080)
        var accessToken by value("")
        var reversePath by value("/ws")
        var reverseApiPath by value(reversePath)
        var reverseEventPath by value(reversePath)
        var useUniversal by value(true)
        var reconnectInterval by value(3000L)
        var useTLS by value(false)
    }

    @Serializable
    object WebsocketServerSettings {
        var enable by value(false)
        var postMessageFormat by value("string")
        var wsHost by value("0.0.0.0")
        var wsPort by value(6700)
        var accessToken by value("")
    }
}