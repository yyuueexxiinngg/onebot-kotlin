package tech.mihoyo.mirai.util

import net.mamoe.mirai.utils.MiraiLoggerPlatformBase
import tech.mihoyo.mirai.PluginBase

class Logger(override val identity: String?) : MiraiLoggerPlatformBase() {
    private val consoleLogger = PluginBase.logger
    override fun verbose0(message: String?) {
        consoleLogger.verbose(message)
    }

    override fun verbose0(message: String?, e: Throwable?) {
        consoleLogger.verbose(message, e)
    }

    override fun debug0(message: String?) {
        if (PluginBase.debug) consoleLogger.debug(message)
    }

    override fun debug0(message: String?, e: Throwable?) {
        if (PluginBase.debug) consoleLogger.debug(message, e)
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

val logger = Logger("CQHTTPAPI")