package tech.mihoyo.mirai.web.websocket

import tech.mihoyo.mirai.WsConfig

class WebSocketServerServiceConfig(serviceConfig: WsConfig) {
    val enable: Boolean by lazy {serviceConfig.enable}

    val postMessageFormat: String by lazy {serviceConfig.postMessageFormat}

    val wsHost: String by lazy {serviceConfig.wsHost}

    val wsPort: Int by lazy {serviceConfig.wsPort}

    val accessToken: String? by lazy {serviceConfig.accessToken}
}