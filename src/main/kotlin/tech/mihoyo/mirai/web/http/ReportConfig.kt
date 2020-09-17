package tech.mihoyo.mirai.web.http

import tech.mihoyo.mirai.util.ConfigSection

class ReportServiceConfig(serviceConfig: ConfigSection) {
    /**
     * 上报消息格式
     */
    val postMessageFormat: String by serviceConfig.withDefault { "string" }

    /**
     * 上报消息格式至URL
     */
    val postUrl: String? by serviceConfig.withDefault { "" }

    /**
     * 上报数据签名密钥
     */
    val secret: String by serviceConfig.withDefault { "" }
}