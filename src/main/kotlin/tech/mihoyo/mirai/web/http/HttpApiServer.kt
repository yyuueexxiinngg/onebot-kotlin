package tech.mihoyo.mirai.web.http

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tech.mihoyo.mirai.BotSession
import tech.mihoyo.mirai.util.logger

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
class HttpApiServer(
    val session: BotSession
) {
    lateinit var server: ApplicationEngine
    private var serviceConfig = HttpApiServerServiceConfig(session.config.getConfigSection("http"))

    init {
        logger.info("Bot: ${session.bot.id} HTTP API服务端是否配置开启: ${serviceConfig.enable}")
        if (serviceConfig.enable) {
            try {
                server = embeddedServer(CIO, environment = applicationEngineEnvironment {
                    this.module { cqHttpApiServer(session, serviceConfig) }
                    connector {
                        this.host = serviceConfig.host
                        this.port = serviceConfig.port
                    }
                })
                server.start(false)
            } catch (e: Exception) {
                logger.error("Bot:${session.bot.id} HTTP API服务端模块启用失败")
            }
        }
    }

    fun close() {
        server.stop(5000, 5000)
    }

}