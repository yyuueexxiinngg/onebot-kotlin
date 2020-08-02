package tech.mihoyo.mirai.web

import net.mamoe.mirai.console.plugins.PluginBase
import tech.mihoyo.mirai.web.http.ReportService
import tech.mihoyo.mirai.web.websocket.WebSocketReverseClient

class HttpApiServices(override val console: PluginBase) : HttpApiService {
    private val services: List<HttpApiService> = listOf(
        ReportService(console)
    )

    override fun onLoad() {
        services.forEach {
            it.onLoad()
        }
    }

    override fun onEnable() {
        services.forEach {
            it.onEnable()
        }
    }

    override fun onDisable() {
        services.forEach {
            it.onDisable()
        }
    }
}