package com.github.yyuueexxiinngg.onebot.web.http

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import com.github.yyuueexxiinngg.onebot.BotSession
import com.github.yyuueexxiinngg.onebot.logger
import io.ktor.util.*

@OptIn(KtorExperimentalAPI::class)
class HttpApiServer(
    private val session: BotSession
) {
    lateinit var server: ApplicationEngine

    init {
        val settings = session.settings.http
        logger.info("Bot: ${session.bot.id} HTTP API服务端是否配置开启: ${settings.enable}")
        if (settings.enable) {
            try {
                server = embeddedServer(CIO, environment = applicationEngineEnvironment {
                    this.module { oneBotApiServer(session, settings) }
                    connector {
                        this.host = settings.host
                        this.port = settings.port
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