package tech.mihoyo.mirai.web.http

import net.mamoe.mirai.console.plugins.ConfigSection

class HttpApiServerServiceConfig(serviceConfig: ConfigSection) {
    val enable: Boolean by serviceConfig.withDefault { false }

    val host: String by serviceConfig.withDefault { "0.0.0.0" }

    val port: Int by serviceConfig.withDefault { 5700 }

    val accessToken: String? by serviceConfig.withDefault { "" }
}