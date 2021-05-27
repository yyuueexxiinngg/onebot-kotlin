package com.github.yyuueexxiinngg.onebot.util

import com.github.yyuueexxiinngg.onebot.PluginBase
import com.github.yyuueexxiinngg.onebot.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.message.data.Image
import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

class ImgUtils {
    enum class ImageState {
        RequireUpload,
        FileExist
    }

    companion object {
        private val imgStoreGroupPicUpClass =
            Class.forName("net.mamoe.mirai.internal.network.protocol.packet.chat.image.ImgStore\$GroupPicUp")
        private val imgStoreGroupPicUpClassConstructor = imgStoreGroupPicUpClass.getDeclaredConstructor()
        private val netWorkHandlerClass =
            Class.forName("net.mamoe.mirai.internal.network.handler.QQAndroidBotNetworkHandler")
        private val netWorkHandlerClassConstructor = netWorkHandlerClass.getDeclaredConstructor(
            Class.forName("kotlin.coroutines.CoroutineContext"),
            Class.forName("net.mamoe.mirai.internal.QQAndroidBot")
        )
        private val sendAndExpectMethod = netWorkHandlerClass.declaredMethods.find { it.name == "sendAndExpect" }
        private val groupPicUpInvokeMethod = imgStoreGroupPicUpClass.getDeclaredMethod(
            "invoke",
            Class.forName("net.mamoe.mirai.internal.network.QQAndroidClient"),
            Long::class.java,
            Long::class.java,
            ByteArray::class.java,
            Long::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            Long::class.java,
            String::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java
        )

        init {
            imgStoreGroupPicUpClassConstructor.isAccessible = true
            netWorkHandlerClassConstructor.isAccessible = true
            groupPicUpInvokeMethod.isAccessible = true
        }

        suspend fun tryGroupPicUp(bot: Bot, groupCode: Long, md5: String, size: Int): ImageState {
            val botClientFiled = bot.javaClass.superclass.getDeclaredField("client")
            botClientFiled.isAccessible = true
            val botClient = botClientFiled.get(bot)

            val network = bot::class.members.find { it.name == "_network" }

            val outGoingPacket = groupPicUpInvokeMethod.invoke(
                imgStoreGroupPicUpClassConstructor.newInstance(),
                botClient,
                bot.id,
                groupCode,
                hexStringToByteArray(md5),
                size.toLong(),
                0, 0, 2001, 0, getRandomString(16) + ".gif", 5, 9, 2, 1006, 0
            )
            val response = sendAndExpectMethod?.kotlinFunction?.callSuspend(
                network!!.call(bot),
                outGoingPacket,
                5000, 2
            )

            return if (response.toString().contains("FileExists")) {
                ImageState.FileExist
            } else {
                ImageState.RequireUpload
            }
        }

        fun md5ToImageId(md5: String, contact: Contact, imageType: String?): String {
            return when (contact) {
                is Group -> "{${md5.substring(0, 8)}-" +
                        "${md5.substring(8, 12)}-" +
                        "${md5.substring(12, 16)}-" +
                        "${md5.substring(16, 20)}-" +
                        "${md5.substring(20)}}.${imageType ?: "mirai"}"
                is Friend, is Member -> "/0-00-$md5"
                else -> ""
            }
        }

        private fun getRandomString(length: Int): String =
            getRandomString(length, *defaultRanges)

        private val defaultRanges: Array<CharRange> = arrayOf('a'..'z', 'A'..'Z', '0'..'9')

        private fun getRandomString(length: Int, vararg charRanges: CharRange): String =
            String(CharArray(length) { charRanges[Random.Default.nextInt(0..charRanges.lastIndex)].random() })

        private fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                        + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }
    }
}

data class CachedImage(
    val file: File,
    val fileName: String,
    val path: String,
    val md5: String,
    val size: Int,
    val url: String,
    val addTime: Long,
    val imageType: String?,
)

internal fun getImageType(image: Image): String {
    val parts = image.imageId.split(".", limit = 2)
    return if (parts.size == 2) {
        parts[1]
    } else {
        "unknown"
    }
}

fun constructCacheImageMeta(md5: String, size: Int?, url: String?, imageType: String?): String {
    return """
                [image]
                md5=${md5}
                size=${size ?: 0}
                url=${url ?: "https://c2cpicdw.qpic.cn/offpic_new/0/0-00-${md5}/0?term=2"}
                addtime=${currentTimeMillis()}
                type=${imageType ?: "unknown"}
            """.trimIndent()
}

internal fun getImageType(bytes: ByteArray): String {
    return with(bytes.copyOfRange(0, 8).toUHexString("")) {
        when {
            startsWith("FFD8") -> "jpg"
            startsWith("89504E47") -> "png"
            startsWith("47494638") -> "gif"
            startsWith("424D") -> "bmp"
            startsWith("52494646") -> "webp"
            else -> "unknown"
        }
    }
}

suspend fun tryResolveCachedImage(name: String, contact: Contact?): Image? {
    var image: Image? = null
    val cachedImage = getCachedImageFile(name)

    if (cachedImage != null) {
        if (contact != null) {
            // If add time till now more than one day, check if the image exists
            if (cachedImage.addTime - currentTimeMillis() >= 1000 * 60 * 60 * 24) {
                if (ImgUtils.tryGroupPicUp(
                        contact.bot,
                        contact.id,
                        cachedImage.md5,
                        cachedImage.size
                    ) != ImgUtils.ImageState.FileExist
                ) {
                    cachedImage.file.delete()
                } else { // If file exists
                    image = Image(ImgUtils.md5ToImageId(cachedImage.md5, contact, cachedImage.imageType))
                    val imgContent = constructCacheImageMeta(
                        cachedImage.md5,
                        cachedImage.size,
                        cachedImage.url,
                        cachedImage.imageType
                    )
                    PluginBase.saveImageAsync("$name.cqimg", imgContent).start() // Update cache file
                }
            } else { // If time < one day
                image = Image(ImgUtils.md5ToImageId(cachedImage.md5, contact, cachedImage.imageType))
            }
        }
    }
    return image
}

suspend fun getCachedImageFile(name: String): CachedImage? = withContext(Dispatchers.IO) {
    val cacheFile =
        with(name) {
            when {
                endsWith(".cqimg") -> getDataFile("image", name)
                endsWith(".image") -> getDataFile("image", name.toLowerCase())
                else -> getDataFile("image", "$name.cqimg") ?: getDataFile("image", "${name.toLowerCase()}.image")
            }
        }

    if (cacheFile != null) {
        if (cacheFile.canRead()) {
            logger.info("此链接图片已缓存, 如需删除缓存请至 ${cacheFile.absolutePath}")
            var md5 = ""
            var size = 0
            var url = ""
            var addTime = 0L
            var imageType: String? = null

            when (cacheFile.extension) {
                "cqimg" -> {
                    val cacheMediaContent = cacheFile.readLines()
                    cacheMediaContent.forEach {
                        val parts = it.trim().split("=", limit = 2)
                        if (parts.size == 2) {
                            when (parts[0]) {
                                "md5" -> md5 = parts[1]
                                "size" -> size = parts[1].toIntOrNull() ?: 0
                                "url" -> url = parts[1]
                                "addtime" -> addTime = parts[1].toLongOrNull() ?: 0L
                                "type" -> imageType = parts[1]
                            }
                        }
                    }
                }

                "image" -> {
                    val bytes = cacheFile.readBytes()
                    md5 = bytes.copyOf(16).toUHexString("")
                    size = bytes.copyOfRange(16, 20).toUnsignedInt().toInt()
                    url = "https://c2cpicdw.qpic.cn/offpic_new//0/0-00-$md5/0?term=2"
                }
            }

            if (md5 != "" && size != 0) {
                return@withContext CachedImage(
                    cacheFile,
                    name,
                    cacheFile.absolutePath,
                    md5,
                    size,
                    url,
                    addTime,
                    imageType
                )
            } else { // If cache file corrupted
                cacheFile.delete()
            }
        } else {
            logger.error("Image $name cache file cannot read.")
        }
    } else {
        logger.error("Image $name cache file cannot be found.")
    }
    null
}

private fun ByteArray.toUnsignedInt(): Long {
    val buffer: ByteBuffer = ByteBuffer.allocate(8).put(byteArrayOf(0, 0, 0, 0)).put(this)
    (buffer as Buffer).position(0)
    return buffer.long
}