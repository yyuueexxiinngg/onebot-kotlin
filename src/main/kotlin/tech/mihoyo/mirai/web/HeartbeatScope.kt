package tech.mihoyo.mirai.web

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import tech.mihoyo.mirai.util.logger
import kotlin.coroutines.CoroutineContext

class HeartbeatScope(coroutineContext: CoroutineContext) : CoroutineScope {
    override val coroutineContext: CoroutineContext = coroutineContext + CoroutineExceptionHandler { _, throwable ->
        logger.error("Exception in Heartbeat", throwable)
    } + SupervisorJob()
}