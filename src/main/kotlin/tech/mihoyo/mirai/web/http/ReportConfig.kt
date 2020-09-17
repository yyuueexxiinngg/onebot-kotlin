package tech.mihoyo.mirai.web.http

import tech.mihoyo.mirai.Settings
import tech.mihoyo.mirai.util.ConfigSection

class ReportServiceConfig(serviceConfig: Settings) {
    /**
     * 上报消息格式
     */
    val postMessageFormat: String by lazy {serviceConfig.http.postMessageFormat}

    /**
     * 上报消息格式至URL
     */
    val postUrl: String by lazy {serviceConfig.http.postUrl}

    /**
     * 上报数据签名密钥
     */
    val secret: String by lazy {serviceConfig.http.secret}
}