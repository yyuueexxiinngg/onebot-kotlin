package com.github.yyuueexxiinngg.onebot.util

import net.mamoe.mirai.utils.MiraiLoggerPlatformBase
import com.github.yyuueexxiinngg.onebot.PluginBase
import com.github.yyuueexxiinngg.onebot.PluginSettings

class Logger(override val identity: String?) : MiraiLoggerPlatformBase() {
    private val consoleLogger = PluginBase.logger
    override fun verbose0(message: String?) {
        consoleLogger.verbose(message)
    }

    override fun verbose0(message: String?, e: Throwable?) {
        consoleLogger.verbose(message, e)
    }

    override fun debug0(message: String?) {
        if (PluginSettings.debug) consoleLogger.debug(message)
    }

    override fun debug0(message: String?, e: Throwable?) {
        if (PluginSettings.debug) consoleLogger.debug(message, e)
    }

    override fun info0(message: String?) {
        consoleLogger.info(message)
    }

    override fun info0(message: String?, e: Throwable?) {
        consoleLogger.info(message, e)
    }

    override fun warning0(message: String?) {
        consoleLogger.warning(message)
    }

    override fun warning0(message: String?, e: Throwable?) {
        consoleLogger.warning(message, e)
    }

    override fun error0(message: String?) {
        consoleLogger.error(message)
    }

    override fun error0(message: String?, e: Throwable?) {
        consoleLogger.error(message, e)
    }
}

val logger = Logger("ONEBOTAPI")