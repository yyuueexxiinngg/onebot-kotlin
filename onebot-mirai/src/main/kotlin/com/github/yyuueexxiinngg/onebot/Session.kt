package com.github.yyuueexxiinngg.onebot

import com.github.yyuueexxiinngg.onebot.data.common.CQIgnoreEventDTO
import com.github.yyuueexxiinngg.onebot.data.common.toCQDTO
import com.github.yyuueexxiinngg.onebot.util.EventFilter
import com.github.yyuueexxiinngg.onebot.util.toJson
import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import com.github.yyuueexxiinngg.onebot.web.http.HttpApiServer
import com.github.yyuueexxiinngg.onebot.web.http.ReportService
import com.github.yyuueexxiinngg.onebot.web.websocket.WebSocketReverseClient
import com.github.yyuueexxiinngg.onebot.web.websocket.WebSocketServer
import net.mamoe.mirai.event.events.BotEvent
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal typealias BotEventListener = suspend (String) -> Unit

internal object SessionManager {

    val allSession: MutableMap<Long, Session> = mutableMapOf()

    operator fun get(botId: Long) = allSession[botId]

    fun containSession(botId: Long): Boolean = allSession.containsKey(botId)

    fun closeSession(botId: Long) = allSession.remove(botId)?.also { it.close() }

    fun closeSession(session: Session) = closeSession(session.botId)

    fun createBotSession(bot: Bot, settings: PluginSettings.BotSettings): BotSession =
        BotSession(bot, settings, EmptyCoroutineContext).also { session -> allSession[bot.id] = session }
}

/**
 * @author NaturalHG
 * 这个用于管理不同Client与Mirai HTTP的会话
 *
 * [Session]均为内部操作用类
 * 需使用[SessionManager]
 */
abstract class Session internal constructor(
    coroutineContext: CoroutineContext,
    open val botId: Long
) : CoroutineScope {
    val supervisorJob = SupervisorJob(coroutineContext[Job])
    final override val coroutineContext: CoroutineContext = supervisorJob + coroutineContext

    internal open fun close() {
        supervisorJob.complete()
    }
}

class BotSession internal constructor(
    val bot: Bot,
    val settings: PluginSettings.BotSettings,
    coroutineContext: CoroutineContext
) :
    Session(coroutineContext, bot.id) {
    private val eventSubscriptionString = mutableListOf<BotEventListener>()
    private val eventSubscriptionArray = mutableListOf<BotEventListener>()
    private var hasStringFormatSubscription = false
    private var hasArrayFormatSubscription = false

    val cqApiImpl = MiraiApi(bot)
    private val httpApiServer = HttpApiServer(this)
    private val websocketClient = WebSocketReverseClient(this)
    private val websocketServer = WebSocketServer(this)
    private val httpReportService = ReportService(this)

    init {
        if (settings.cacheImage) logger.info("Bot: ${bot.id} 已开启接收图片缓存, 将会缓存收取到的所有图片")
        else logger.info("Bot: ${bot.id} 未开启接收图片缓存, 将不会缓存收取到的所有图片, 如需开启, 请在当前Bot配置中添加cacheImage=true")

        if (settings.cacheRecord) logger.info("Bot: ${bot.id} 已开启接收语音缓存, 将会缓存收取到的所有语音")
        else logger.info("Bot: ${bot.id} 未开启接收语音缓存, 将不会缓存收取到的所有语音, 如需开启, 请在当前Bot配置中添加cacheRecord=true")

        if (settings.heartbeat.enable) logger.info("Bot: ${bot.id} 已开启心跳机制, 设定的心跳发送频率为 ${settings.heartbeat.interval} 毫秒")
    }

    override fun close() {
        runBlocking {
            websocketClient.close()
            websocketServer.close()
            httpApiServer.close()
            httpReportService.close()
        }
        super.close()
    }

    suspend fun triggerEvent(event: BotEvent) {
        // Boolean checks should be faster then List size checks I suppose.
        if (this.hasStringFormatSubscription) {
            triggerEventInternal(event, true)
        }

        if (this.hasArrayFormatSubscription) {
            triggerEventInternal(event, false)
        }
    }

    private suspend fun triggerEventInternal(event: BotEvent, isStringFormat: Boolean = false) {
        event.toCQDTO(isRawMessage = isStringFormat).takeIf { it !is CQIgnoreEventDTO }?.let { dto ->
            val jsonToSend = dto.toJson()
            PluginBase.logger.debug("将发送事件: $jsonToSend")
            if (!EventFilter.eval(jsonToSend)) {
                PluginBase.logger.debug("事件被Event Filter命中, 取消发送")
            } else {
                if (isStringFormat) {
                    this.eventSubscriptionString.forEach { it(jsonToSend) }
                } else {
                    this.eventSubscriptionArray.forEach { it(jsonToSend) }
                }
            }
        }
    }

    fun subscribeEvent(listener: BotEventListener, isRawMessage: Boolean): BotEventListener =
        listener.also {
            if (isRawMessage) {
                eventSubscriptionString.add(it)
                hasStringFormatSubscription = true
            } else {
                eventSubscriptionArray.add(it)
                hasArrayFormatSubscription = true
            }
        }

    fun unsubscribeEvent(listener: BotEventListener, isRawMessage: Boolean) {
        if (isRawMessage) {
            eventSubscriptionString.remove(listener)
            if (eventSubscriptionString.isEmpty()) hasStringFormatSubscription = false
        } else {
            eventSubscriptionArray.remove(listener)
            if (eventSubscriptionArray.isEmpty()) hasArrayFormatSubscription = false
        }
    }
}
