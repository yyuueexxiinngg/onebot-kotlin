package com.github.yyuueexxiinngg.onebot.util

import com.github.yyuueexxiinngg.onebot.PluginBase
import com.github.yyuueexxiinngg.onebot.logger
import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.message.data.Image
import java.io.File
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.*

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

fun md5ToImageId(md5: String, contact: Contact): String {
    return when (contact) {
        is Group -> "{${md5.substring(0, 8)}-" +
                "${md5.substring(8, 12)}-" +
                "${md5.substring(12, 16)}-" +
                "${md5.substring(16, 20)}-" +
                "${md5.substring(20)}}.mirai"
        is User -> "/0-00-$md5"
        else -> ""
    }
}

suspend fun tryResolveCachedImage(name: String, contact: Contact?): Image? {
    var image: Image? = null
    val cachedImage = getCachedImageFile(name)


    if (cachedImage != null) {
        // If add time till now more than one day, check if the image exists
        if (contact != null) {
            if (currentTimeMillis() - cachedImage.addTime >= 1000 * 60 * 60 * 24) {
                runCatching {
                    HttpClient {}.head<ByteArray>(cachedImage.url)
                }.onFailure {
                    //Not existed, delete file and return null
                    logger.error("Failed to fetch cache image", it)
                    cachedImage.file.delete()
                    return null
                }.onSuccess {
                    //Existed and update cache file
                    val imgContent = constructCacheImageMeta(
                        cachedImage.md5,
                        cachedImage.size,
                        cachedImage.url,
                        cachedImage.imageType
                    )
                    PluginBase.saveImageAsync("$name.cqimg", imgContent).start()
                }
            }
            //Only use id when existing
            image = Image.fromId(md5ToImageId(cachedImage.md5, contact))
        }
    }
    return image
}

suspend fun getCachedImageFile(name: String): CachedImage? = withContext(Dispatchers.IO) {
    val cacheFile =
        with(name) {
            when {
                endsWith(".cqimg") -> getDataFile("image", name)
                endsWith(".image") -> getDataFile("image", name.lowercase(Locale.getDefault()))
                else -> getDataFile("image", "$name.cqimg") ?: getDataFile(
                    "image",
                    "${name.lowercase(Locale.getDefault())}.image"
                )
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
        logger.info("Image $name cache file cannot be found.")
    }
    null
}

private fun ByteArray.toUnsignedInt(): Long {
    val buffer: ByteBuffer = ByteBuffer.allocate(8).put(byteArrayOf(0, 0, 0, 0)).put(this)
    (buffer as Buffer).position(0)
    return buffer.long
}