package tech.mihoyo.mirai

import com.google.auto.service.AutoService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.LowLevelAPI
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPlugin
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Voice
import net.mamoe.mirai.utils.currentTimeMillis
import tech.mihoyo.mirai.SessionManager.allSession
import tech.mihoyo.mirai.SessionManager.closeSession
import tech.mihoyo.mirai.util.HttpClient
import tech.mihoyo.mirai.util.HttpClient.Companion.initHTTPClientProxy
import tech.mihoyo.mirai.util.toUHexString
import java.io.File
import kotlin.reflect.jvm.isAccessible
import yyuueexxiinngg.cqhttp_mirai.BuildConfig

@AutoService(JvmPlugin::class)
object PluginBase : KotlinPlugin(
    JvmPluginDescription(
        "yyuueexxiinngg.cqhttp-mirai",
        "0.2.4-SNAPSHOT-1.0-M4-dev"
    ) {
        name("CQHTTP-Mirai")
    }
) {
    // private lateinit var config: Config
    var debug = false
    var initialSubscription: Listener<BotEvent>? = null
    var proxy = ""

    fun onLoad() {
        logger.info("Welcome to use CQHTTP-Mirai")
    }

    @OptIn(LowLevelAPI::class)
    @kotlin.ExperimentalUnsignedTypes
    override fun onEnable() {
        Settings.reload()

        //config = File("plugins/CQHTTPMirai/setting.yml").loadAsConfig()
        //debug = if (config.exist("debug")) config.getBoolean("debug") else false
        debug = Settings.debug
        //proxy = if (config.exist("proxy")) config.getString("proxy") else ""
        proxy = Settings.proxy
        logger.info("Plugin loaded! ${BuildConfig.VERSION}")
        logger.info("插件当前Commit 版本: ${BuildConfig.COMMIT_HASH}")
        if (debug) logger.debug("开发交流群: 1143274864")
        initHTTPClientProxy()
        Bot.forEachInstance {
            if (!allSession.containsKey(it.id)) {
                //if (config.exist(it.id.toString())) {
                if (it.id == Settings.user.toLong()) {
                    SessionManager.createBotSession(it, Settings)
                } else {
                    logger.debug("${it.id}未对CQHTTPMirai进行配置")
                }
            } else {
                logger.debug("${it.id}已存在")
            }
        }

        initialSubscription = subscribeAlways {
            when (this) {
                is BotOnlineEvent -> {
                    if (!allSession.containsKey(bot.id)) {
                        if (Settings.user.toLong() == bot.id) {
                            SessionManager.createBotSession(bot, Settings)
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
                is BotInvitedJoinGroupRequestEvent -> {
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
                                val voiceUrl = if (voice.url != null) voice.url else {
                                    val voiceUrlFiled = voice::class.members.find { it.name == "_url" }
                                    voiceUrlFiled?.isAccessible = true
                                    "http://grouptalk.c2c.qq.com${voiceUrlFiled?.call(voice)}"
                                }
                                val voiceBytes = voiceUrl?.let { it -> HttpClient.getBytes(it) }
                                if (voiceBytes != null) {
                                    saveRecordAsync("${voice.md5.toUHexString("")}.cqrecord", voiceBytes).start()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDisable() {
        initialSubscription?.complete()
        allSession.forEach { (sessionId, _) -> closeSession(sessionId) }
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

    suspend fun saveImage(name: String, data: ByteArray) = withContext(Dispatchers.IO) {
        image(name).apply { writeBytes(data) }
    }

    fun saveImageAsync(name: String, data: String) =
        async {
            image(name).apply { writeText(data) }
        }
}

// Plugin Config Object - 1.0-M4
object Settings : AutoSavePluginConfig() {
    val debug: Boolean by value(false)
    val proxy: String by value()
    val user: Int by value(111111111)
    val cacheImage: Boolean by value(false)
    val cacheRecord: Boolean by value(false)
    val heartbeat: HeartBeatConfig by value()
    val http: HttpConfig by value()
    val ws_reverse: WsReverseConfig by value()
    val ws: WsConfig by value()
}

@Serializable
data class HeartBeatConfig(
    val enable: Boolean = true,
    val interval: Int = 15000
)

@Serializable
data class HttpConfig(
    val enable: Boolean = false,
    val host: String = "0.0.0.0",
    val port: Int = 5700,
    val accessToken: String = "",
    val postUrl: String = "",
    val postMessageFormat: String = "string",
    val secret: String = ""
)

@Serializable
data class WsReverseConfig(
    val enable: Boolean = true,
    val postMessageFormat: String = "string",
    val reverseHost: String = "127.0.0.1",
    val reversePort: Int = 10676,
    val accessToken: String = "",
    val reversePath: String = "/ws",
    val reverseApiPath: String = "/api",
    val reverseEventPath: String = "/event",
    val useUniversal: Boolean = true,
    val useTLS: Boolean = false,
    val reconnectInterval: Int = 3000
)

@Serializable
data class WsConfig(
    val enable: Boolean = false,
    val postMessageFormat: String = "string",
    val accessToken: String = "",
    val wsHost: String = "0.0.0.0",
    val wsPort: Int = 8080
)