package tech.mihoyo.mirai.web.http

import tech.mihoyo.mirai.Settings

class HttpApiServerServiceConfig(serviceConfig: Settings) {
    val enable: Boolean by lazy { serviceConfig.http.enable }
    val host: String by lazy {serviceConfig.http.host }
    val port: Int by lazy {serviceConfig.http.port}
    val accessToken: String? by lazy {serviceConfig.http.accessToken}
}