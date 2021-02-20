/*
 *
 * Part of codes was taken from Mirai Native
 *
 * Copyright (C) 2020 iTX Technologies
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author PeratX
 * @website https://github.com/iTXTech/mirai-native
 *
 */
package com.github.yyuueexxiinngg.onebot.util

import com.github.yyuueexxiinngg.onebot.PluginBase
import com.github.yyuueexxiinngg.onebot.PluginBase.saveImageAsync
import com.github.yyuueexxiinngg.onebot.PluginBase.saveRecordAsync
import com.github.yyuueexxiinngg.onebot.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.MiraiExperimentalApi
import net.mamoe.mirai.utils.MiraiInternalApi
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*
import kotlin.collections.HashMap

suspend fun cqMessageToMessageChains(
    bot: Bot,
    contact: Contact?,
    cqMessage: Any?,
    raw: Boolean = false
): MessageChain? {
    when (cqMessage) {
        is String -> {
            return if (raw) {
                PlainText(cqMessage).toMessageChain()
            } else {
                codeToChain(bot, cqMessage, contact)
            }
        }
        is JsonArray -> {
            var messageChain = buildMessageChain { }
            for (msg in cqMessage) {
                try {
                    val data = msg.jsonObject["data"]
                    when (msg.jsonObject["type"]?.jsonPrimitive?.content) {
                        "text" -> messageChain += PlainText(data!!.jsonObject["text"]!!.jsonPrimitive.content)
                        else -> messageChain += cqTextToMessageInternal(bot, contact, msg)
                    }
                } catch (e: NullPointerException) {
                    logger.warning("Got null when parsing CQ message array")
                    continue
                }
            }
            return messageChain
        }
        is JsonObject -> {
            return try {
                val data = cqMessage.jsonObject["data"]
                when (cqMessage.jsonObject["type"]?.jsonPrimitive?.content) {
                    "text" -> PlainText(data!!.jsonObject["text"]!!.jsonPrimitive.content).toMessageChain()
                    else -> cqTextToMessageInternal(bot, contact, cqMessage).toMessageChain()
                }
            } catch (e: NullPointerException) {
                logger.warning("Got null when parsing CQ message object")
                null
            }
        }
        is JsonPrimitive -> {
            return if (raw) {
                PlainText(cqMessage.content).toMessageChain()
            } else {
                codeToChain(bot, cqMessage.content, contact)
            }
        }
        else -> {
            logger.warning("Cannot determine type of " + cqMessage.toString())
            return null
        }
    }
}


private suspend fun cqTextToMessageInternal(bot: Bot, contact: Contact?, message: Any): Message {
    when (message) {
        is String -> {
            if (message.startsWith("[CQ:") && message.endsWith("]")) {
                val parts = message.substring(4, message.length - 1).split(delimiters = arrayOf(","), limit = 2)

                lateinit var args: HashMap<String, String>
                args = if (parts.size == 2) {
                    parts[1].toMap()
                } else {
                    HashMap()
                }
                return convertToMiraiMessage(bot, contact, parts[0], args)
            }
            return PlainText(message.unescape())
        }
        is JsonObject -> {
            val type = message.jsonObject["type"]!!.jsonPrimitive.content
            val data = message.jsonObject["data"] ?: return MSG_EMPTY
            val args = data.jsonObject.keys.map { it to data.jsonObject[it]!!.jsonPrimitive.content }.toMap()
            return convertToMiraiMessage(bot, contact, type, args)
        }
        else -> return MSG_EMPTY
    }
}

@OptIn(MiraiExperimentalApi::class)
private suspend fun convertToMiraiMessage(
    bot: Bot,
    contact: Contact?,
    type: String,
    args: Map<String, String>
): Message {
    when (type) {
        "at" -> {
            if (args["qq"] == "all") {
                return AtAll
            } else {
                val group = bot.getGroup(contact!!.id) ?: return MSG_EMPTY
                val member = group[args["qq"]!!.toLong()] ?: return MSG_EMPTY
                return At(member)
            }
        }
        "face" -> {
            return Face(args["id"]!!.toInt())
        }
        "emoji" -> {
            return PlainText(String(Character.toChars(args["id"]!!.toInt())))
        }
        "image" -> {
            return tryResolveMedia("image", contact, args)
        }
        "share" -> {
            return RichMessageHelper.share(
                args["url"]!!,
                args["title"],
                args["content"],
                args["image"]
            )
        }
        "record" -> {
            return tryResolveMedia("record", contact, args)
        }
        "contact" -> {
            return if (args["type"] == "qq") {
                RichMessageHelper.contactQQ(bot, args["id"]!!.toLong())
            } else {
                RichMessageHelper.contactGroup(bot, args["id"]!!.toLong())
            }
        }
        "music" -> {
            when (args["type"]) {
                "qq" -> return QQMusic.send(args["id"]!!)
                "163" -> return NeteaseMusic.send(args["id"]!!)
                else -> throw IllegalArgumentException("Custom music share not supported anymore")
            }
        }
        "shake" -> {
            return PokeMessage.ChuoYiChuo
        }
        "poke" -> {
            PokeMessage.values.forEach {
                if (it.pokeType == args["type"]!!.toInt() && it.id == args["id"]!!.toInt()) {
                    return it
                }
            }
            return MSG_EMPTY
        }
        // Could be changed at anytime.
        "nudge" -> {
            val target = args["qq"] ?: error("Nudge target `qq` must not ne null.")
            if (contact is Group) {
                contact.members[target.toLong()]?.nudge()?.sendTo(contact)
            } else {
                contact?.let { bot.friends[target.toLong()]?.nudge()?.sendTo(it) }
            }
            return MSG_EMPTY
        }
        "xml" -> {
            return xmlMessage(args["data"]!!)
        }
        "json" -> {
            return if (args["data"]!!.contains("\"app\":")) {
                LightApp(args["data"]!!)
            } else {
                jsonMessage(args["data"]!!)
            }
        }
        else -> {
            logger.debug("不支持的 CQ码：${type}")
        }
    }
    return MSG_EMPTY
}


private val MSG_EMPTY = PlainText("")

private fun String.escape(): String {
    return replace("&", "&amp;")
        .replace("[", "&#91;")
        .replace("]", "&#93;")
        .replace(",", "&#44;")
}

private fun String.unescape(): String {
    return replace("&amp;", "&")
        .replace("&#91;", "[")
        .replace("&#93;", "]")
        .replace("&#44;", ",")
}

private fun String.toMap(): HashMap<String, String> {
    val map = HashMap<String, String>()
    split(",").forEach {
        val parts = it.split(delimiters = *arrayOf("="), limit = 2)
        map[parts[0].trim()] = parts[1].unescape()
    }
    return map
}

@OptIn(MiraiExperimentalApi::class, MiraiInternalApi::class)
suspend fun Message.toCQString(): String {
    return when (this) {
        is PlainText -> content.escape()
        is At -> "[CQ:at,qq=$target]"
        is Face -> "[CQ:face,id=$id]"
        is VipFace -> "[CQ:vipface,id=${kind.id},name=${kind.name},count=${count}]"
        is PokeMessage -> "[CQ:poke,id=${id},type=${pokeType},name=${name}]"
        is AtAll -> "[CQ:at,qq=all]"
        is Image -> "[CQ:image,file=${md5.toUHexString("")},url=${queryUrl().escape()}]"
        is FlashImage -> "[CQ:image,file=${image.md5.toUHexString("")},url=${image.queryUrl().escape()},type=flash]"
        is ServiceMessage -> with(content) {
            when {
                contains("xml version") -> "[CQ:xml,data=${content.escape()}]"
                else -> "[CQ:json,data=${content.escape()}]"
            }
        }
        is LightApp -> "[CQ:json,data=${content.escape()}]"
        is MessageSource -> ""
        is QuoteReply -> ""
        is Voice -> "[CQ:record,url=${url?.escape()},file=${md5.toUHexString("")}]"
        else -> "此处消息的转义尚未被插件支持"
    }
}

suspend fun codeToChain(bot: Bot, message: String, contact: Contact?): MessageChain {
    return buildMessageChain {
        if (message.contains("[CQ:")) {
            var interpreting = false
            val sb = StringBuilder()
            var index = 0
            message.forEach { c: Char ->
                if (c == '[') {
                    if (interpreting) {
                        logger.error("CQ消息解析失败：$message，索引：$index")
                        return@forEach
                    } else {
                        interpreting = true
                        if (sb.isNotEmpty()) {
                            val lastMsg = sb.toString()
                            sb.delete(0, sb.length)
                            +cqTextToMessageInternal(bot, contact, lastMsg)
                        }
                        sb.append(c)
                    }
                } else if (c == ']') {
                    if (!interpreting) {
                        logger.error("CQ消息解析失败：$message，索引：$index")
                        return@forEach
                    } else {
                        interpreting = false
                        sb.append(c)
                        if (sb.isNotEmpty()) {
                            val lastMsg = sb.toString()
                            sb.delete(0, sb.length)
                            +cqTextToMessageInternal(bot, contact, lastMsg)
                        }
                    }
                } else {
                    sb.append(c)
                }
                index++
            }
            if (sb.isNotEmpty()) {
                +cqTextToMessageInternal(bot, contact, sb.toString())
            }
        } else {
            +PlainText(message.unescape())
        }
    }
}

fun getDataFile(type: String, name: String): File? {
    arrayOf(
        File(PluginBase.dataFolder, type).absolutePath + File.separatorChar,
        "data" + File.separatorChar + type + File.separatorChar,
        System.getProperty("java.library.path")
            .substringBefore(";") + File.separatorChar + "data" + File.separatorChar + type + File.separatorChar,
        ""
    ).forEach {
        val f = File(it + name).absoluteFile
        if (f.exists()) {
            return f
        }
    }
    return null
}

suspend fun tryResolveMedia(type: String, contact: Contact?, args: Map<String, String>): Message {
    var media: Message? = null
    var mediaBytes: ByteArray? = null
    var mediaUrl: String? = null

    withContext(Dispatchers.IO) {
        if (args.containsKey("file")) {
            with(args["file"]!!) {
                when {
                    startsWith("base64://") -> {
                        mediaBytes = Base64.getDecoder().decode(args["file"]!!.replace("base64://", ""))
                    }
                    startsWith("http") -> {
                        mediaUrl = args["file"]
                    }
                    else -> {
                        val filePath = args["file"]!!
                        if (filePath.startsWith("file:///")) {
                            var fileUri = URL(args["file"]).toURI()
                            if (fileUri.authority != null && fileUri.authority.isNotEmpty()) {
                                fileUri = URL("file://" + args["file"]!!.substring("file:".length)).toURI()
                            }
                            val file = File(fileUri).absoluteFile
                            if (file.exists() && file.canRead()) {
                                mediaBytes = file.readBytes()
                            }
                        } else {
                            if (type == "image") {
                                media = tryResolveCachedImage(filePath, contact)
                            } else if (type == "record") {
                                media = tryResolveCachedRecord(filePath, contact)
                            }
                            if (media == null) {
                                val file = getDataFile(type, filePath)
                                if (file != null && file.canRead()) {
                                    mediaBytes = file.readBytes()
                                }
                            }
                        }
                        if (mediaBytes == null) {
                            if (args.containsKey("url")) {
                                mediaUrl = args["url"]!!
                            }
                        }
                    }
                }
            }
        } else if (args.containsKey("url")) {
            mediaUrl = args["url"]!!
        }

        if (mediaBytes == null && mediaUrl != null) {
            var useCache = true
            if (args.containsKey("cache")) {
                try {
                    useCache = args["cache"]?.toIntOrNull() != 0
                } catch (e: Exception) {
                    logger.debug(e.message)
                }
            }

            val timeoutSecond = (if (args.containsKey("timeout")) args["timeout"]?.toIntOrNull() else null) ?: 0
            val useProxy = (if (args.containsKey("proxy")) args["proxy"]?.toIntOrNull() == 1 else null) ?: false
            val urlHash = md5(mediaUrl!!).toUHexString("")

            when (type) {
                "image" -> {
                    if (useCache) {
                        media = tryResolveCachedImage(urlHash, contact)
                    }

                    if (media == null || !useCache) {
                        mediaBytes = HttpClient.getBytes(mediaUrl!!, timeoutSecond * 1000L, useProxy)

                        media = mediaBytes?.let {
                            contact!!.uploadImage(it.toExternalResource())
                        }

                        if (useCache) {
                            val imageMD5 = mediaBytes?.let { md5(it) }?.toUHexString("")
                            if (imageMD5 != null) {
                                val cqImgContent = """
                                    [image]
                                    md5=$imageMD5
                                    size=${mediaBytes?.size ?: 0}
                                    url=https://gchat.qpic.cn/gchatpic_new/${contact!!.bot.id}/0-00-$imageMD5/0?term=2
                                    addtime=${currentTimeMillis()}
                                    """.trimIndent()
                                logger.info("此链接图片将缓存为$urlHash.cqimg")
                                saveImageAsync("$urlHash.cqimg", cqImgContent).start()
                            }
                        }
                    }
                }
                "record" -> {
                    if (useCache) {
                        media = tryResolveCachedRecord(urlHash, contact)
                    }
                    if (media == null || !useCache) {
                        mediaBytes = HttpClient.getBytes(mediaUrl!!, timeoutSecond * 1000L, useProxy)
                        media = mediaBytes?.let { mBytes ->
                            contact?.let { (it as Group).uploadVoice(mBytes.toExternalResource()) }
                        }

                        if (useCache && mediaBytes != null) {
                            saveRecordAsync("$urlHash.cqrecord", mediaBytes!!).start()
                        }
                    }
                }
            }
        }
    }

    when (type) {
        "image" -> {
            val flash = args.containsKey("type") && args["type"] == "flash"
            if (media == null && mediaBytes != null) {
                val bis = ByteArrayInputStream(mediaBytes)
                media = withContext(Dispatchers.IO) { contact!!.uploadImage(bis.toExternalResource()) }
            }

            return if (flash) {
                (media as Image).flash()
            } else {
                media as Image
            }
        }
        "record" -> {
            if (media == null && mediaBytes != null) {
                media =
                    withContext(Dispatchers.IO) { (contact!! as Group).uploadVoice(mediaBytes!!.toExternalResource()) }
            }
            return media as Voice
        }
    }
    return PlainText("插件无法获取到媒体" + if (mediaUrl != null) ", 媒体链接: $mediaUrl" else "")
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
                            }
                        }
                    }
                }

                "image" -> {
                    val bytes = cacheFile.readBytes()
                    md5 = bytes.copyOf(16).toUHexString("")
                    size = bytes.copyOfRange(16, 20).toUnsignedInt().toInt()
                    url = "https://gchat.qpic.cn/gchatpic_new/0/0-00-$md5/0?term=2"
                }
            }

            if (md5 != "" && size != 0) {
                return@withContext CachedImage(cacheFile, name, cacheFile.absolutePath, md5, size, url, addTime)
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

suspend fun getCachedRecordFile(name: String): File? = withContext(Dispatchers.IO) {
    if (name.endsWith(".cqrecord")) getDataFile("record", name)
    else getDataFile("record", "$name.cqrecord")
}

suspend fun tryResolveCachedRecord(name: String, contact: Contact?): Voice? {
    val cacheFile = getCachedRecordFile(name)
    if (cacheFile != null) {
        if (cacheFile.canRead()) {
            logger.info("此语音已缓存, 如需删除缓存请至 ${cacheFile.absolutePath}")
            return contact?.let { (it as Group).uploadVoice(cacheFile.toExternalResource()) }
        } else {
            logger.error("Record $name cache file cannot read.")
        }
    } else {
        logger.error("Record $name cache file cannot read.")
    }
    return null
}

suspend fun tryResolveCachedImage(name: String, contact: Contact?): Image? {
    var image: Image? = null
    val cachedImage = getCachedImageFile(name)

    if (cachedImage != null) {
        if (contact != null) {
            // If add time till now more than one day, check if the image exists
            if (cachedImage.addTime - currentTimeMillis() >= 1000 * 60 * 60 * 24) {
                if (ImgUtil.tryGroupPicUp(
                        contact.bot,
                        contact.id,
                        cachedImage.md5,
                        cachedImage.size
                    ) != ImgUtil.ImageState.FileExist
                ) {
                    cachedImage.file.delete()
                } else { // If file exists
                    image = Image(ImgUtil.md5ToImageId(cachedImage.md5, contact))
                    val cqImgContent = """
                                                [image]
                                                md5=${cachedImage.md5}
                                                size=${cachedImage.size}
                                                url=https://gchat.qpic.cn/gchatpic_new/${contact.bot.id}/0-00-${cachedImage.md5}/0?term=2
                                                addtime=${currentTimeMillis()}
                                            """.trimIndent()
                    saveImageAsync("$name.cqimg", cqImgContent).start() // Update cache file
                }
            } else { // If time < one day
                image = Image(ImgUtil.md5ToImageId(cachedImage.md5, contact))
            }
        }
    }
    return image
}

fun md5(data: ByteArray): ByteArray {
    return MessageDigest.getInstance("MD5").digest(data)
}

fun md5(str: String): ByteArray = md5(str.toByteArray())

@OptIn(ExperimentalUnsignedTypes::class)
internal fun ByteArray.toUHexString(
    separator: String = " ",
    offset: Int = 0,
    length: Int = this.size - offset
): String {
    if (length == 0) {
        return ""
    }
    val lastIndex = offset + length
    return buildString(length * 2) {
        this@toUHexString.forEachIndexed { index, it ->
            if (index in offset until lastIndex) {
                var ret = it.toUByte().toString(16).toUpperCase()
                if (ret.length == 1) ret = "0$ret"
                append(ret)
                if (index < lastIndex - 1) append(separator)
            }
        }
    }
}

private fun ByteArray.toUnsignedInt(): Long {
    val buffer: ByteBuffer = ByteBuffer.allocate(8).put(byteArrayOf(0, 0, 0, 0)).put(this)
    (buffer as Buffer).position(0)
    return buffer.long
}

data class CachedImage(
    val file: File,
    val fileName: String,
    val path: String,
    val md5: String,
    val size: Int,
    val url: String,
    val addTime: Long,
)