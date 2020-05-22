package tech.mihoyo.mirai.web.http

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugins.Config

class ReportServiceConfig(config: Config, bot: Bot) {
    /**
     * 当前Bot的总配置
     */
    @Suppress("UNCHECKED_CAST")
    private val  botConfig = config[bot.id.toString()] as? Map<String, Any> ?: emptyMap()

    /**
     * Bot对应HTTP服务配置
     */
    @Suppress("UNCHECKED_CAST")
    private val httpServiceConfig = botConfig["http"] as? Map<String, Any> ?: emptyMap()

    /**
     * 上报消息格式
     */
    val postMessageFormat: String by httpServiceConfig.withDefault { "string" }

    /**
     * 上报消息格式至URL
     */
    val postUrl: String by httpServiceConfig.withDefault { "" }

    /**
     * 上报数据签名密钥
     */
    val secret: String by httpServiceConfig.withDefault { "" }
}