package tech.mihoyo.mirai

import io.ktor.util.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import net.mamoe.mirai.LowLevelAPI
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.console.plugins.withDefault
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Voice
import net.mamoe.mirai.utils.currentTimeMillis
import tech.mihoyo.mirai.SessionManager.allSession
import tech.mihoyo.mirai.util.HttpClient
import tech.mihoyo.mirai.util.toUHexString
import tech.mihoyo.mirai.web.HttpApiServices
import java.io.File

object PluginBase : PluginBase() {
    private val config = loadConfig("setting.yml")
    val debug by config.withDefault { false }
    var services: HttpApiServices = HttpApiServices(this)
    override fun onLoad() {
        services.onLoad()
    }

    @LowLevelAPI
    @KtorExperimentalAPI
    @ExperimentalCoroutinesApi
    override fun onEnable() {
        logger.info("Plugin loaded!")
        subscribeAlways<BotEvent> {
            when (this) {
                is BotOnlineEvent -> {
                    if (!allSession.containsKey(bot.id)) {
                        if (config.exist(bot.id.toString())) {
                            SessionManager.createBotSession(bot, config.getConfigSection(bot.id.toString()))
                        } else {
                            logger.debug("${bot.id}未对CQHTTPMirai进行配置")
                        }
                    }
                }
                is NewFriendRequestEvent -> {
                    allSession[bot.id]?.let {
                        (it as BotSession).cqApiImpl.cacheRequestQueue.add(this)
                    }
                }
                is MemberJoinRequestEvent -> {
                    allSession[bot.id]?.let {
                        (it as BotSession).cqApiImpl.cacheRequestQueue.add(this)
                    }
                }
                is MessageEvent -> {
                    allSession[bot.id]?.let { s ->
                        val session = s as BotSession
                        if (this is TempMessageEvent) {
                            session.cqApiImpl.cachedTempContact[this.sender.id] = this.group.id
                        }

                        if (session.shouldCacheImage) {
                            message.filterIsInstance<Image>().forEach { image ->
                                val delegate = image::class.members.find { it.name == "delegate" }?.call(image)
                                var imageMD5 = ""
                                var imageSize = 0
                                when (subject) {
                                    is Member, is Friend -> {
                                        imageMD5 =
                                            (delegate?.let { _delegate -> _delegate::class.members.find { it.name == "picMd5" } }
                                                ?.call(delegate) as ByteArray?)?.let { it.toUHexString("") } ?: ""
                                        val imageHeight =
                                            delegate?.let { _delegate -> _delegate::class.members.find { it.name == "picHeight" } }
                                                ?.call(delegate) as Int?
                                        val imageWidth =
                                            delegate?.let { _delegate -> _delegate::class.members.find { it.name == "picWidth" } }
                                                ?.call(delegate) as Int?

                                        if (imageHeight != null && imageWidth != null) {
                                            imageSize = imageHeight * imageWidth
                                        }
                                    }
                                    is Group -> {
                                        imageMD5 =
                                            (delegate?.let { _delegate -> _delegate::class.members.find { it.name == "md5" } }
                                                ?.call(delegate) as ByteArray?)?.let { it.toUHexString("") } ?: ""
                                        imageSize =
                                            (delegate?.let { _delegate -> _delegate::class.members.find { it.name == "size" } }
                                                ?.call(delegate) as Int?) ?: 0
                                    }
                                }

                                val cqImgContent = """
                                    [image]
                                    md5=$imageMD5
                                    size=$imageSize
                                    url=https://gchat.qpic.cn/gchatpic_new/0/0-00-$imageMD5/0?term=2
                                    addtime=$currentTimeMillis
                                """.trimIndent()

                                saveImageAsync("$imageMD5.cqimg", cqImgContent).start()
                            }
                        }

                        if (session.shouldCacheRecord) {
                            message.filterIsInstance<Voice>().forEach { voice ->
                                val voiceBytes = voice.url?.let { it -> HttpClient.getBytes(it) }
                                if (voiceBytes != null) {
                                    saveRecordAsync("${voice.md5.toUHexString("")}.cqrecord", voiceBytes).start()
                                }
                            }
                        }
                    }
                }
            }
        }
        services.onEnable()
    }

    override fun onDisable() {
        services.onDisable()
        allSession.forEach { (_, session) -> session.close() }
    }

    private val imageFold: File = File(dataFolder, "image").apply { mkdirs() }
    private val recordFold: File = File(dataFolder, "record").apply { mkdirs() }

    internal fun image(imageName: String) = File(imageFold, imageName)
    internal fun record(recordName: String) = File(recordFold, recordName)

    fun saveRecordAsync(name: String, data: ByteArray) =
        async {
            record(name).apply { writeBytes(data) }
        }

    fun saveImageAsync(name: String, data: ByteArray) =
        async {
            image(name).apply { writeBytes(data) }
        }

    fun saveImageAsync(name: String, data: String) =
        async {
            image(name).apply { writeText(data) }
        }
}