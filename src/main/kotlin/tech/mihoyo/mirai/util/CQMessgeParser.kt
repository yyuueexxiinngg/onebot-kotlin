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
package tech.mihoyo.mirai.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.content
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.uploadImage
import net.mamoe.mirai.utils.MiraiExperimentalAPI
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.util.*
import kotlin.collections.HashMap


suspend fun cqMessageToMessageChains(
    bot: Bot,
    contact: Contact?,
    cqMessage: Any?,
    raw: Boolean = false
): MessageChain? {
    return when (cqMessage) {
        is String -> {
            return if (raw) {
                PlainText(cqMessage).asMessageChain()
            } else {
                codeToChain(bot, cqMessage, contact)
            }
        }
        is JsonArray -> {
            var messageChain = buildMessageChain { }
            for (msg in cqMessage) {
                try {
                    val data = msg.jsonObject["data"]
                    when (msg.jsonObject["type"]?.content) {
                        "text" -> messageChain += PlainText(data!!.jsonObject["text"]!!.content)
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
                when (cqMessage.jsonObject["type"]?.content) {
                    "text" -> PlainText(data!!.jsonObject["text"]!!.content).asMessageChain()
                    else -> cqTextToMessageInternal(bot, contact, cqMessage).asMessageChain()
                }
            } catch (e: NullPointerException) {
                logger.warning("Got null when parsing CQ message object")
                null
            }
        }
        is JsonPrimitive -> {
            return if (raw) {
                PlainText(cqMessage.content).asMessageChain()
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
    return when (message) {
        is String -> {
            if (message.startsWith("[CQ:") && message.endsWith("]")) {
                val parts = message.substring(4, message.length - 1).split(delimiters = *arrayOf(","), limit = 2)

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
            val type = message.jsonObject["type"]!!.content
            val data = message.jsonObject["data"] ?: return MSG_EMPTY
            val args = data.jsonObject.keys.map { it to data.jsonObject[it]!!.content }.toMap()
            return convertToMiraiMessage(bot, contact, type, args)
        }
        else -> MSG_EMPTY
    }
}

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
                val group = bot.getGroupOrNull(contact!!.id) ?: return MSG_EMPTY
                val member = group.getOrNull(args["qq"]!!.toLong()) ?: return MSG_EMPTY
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
            var image: Image? = null
            if (args.containsKey("file")) {
                with(args["file"]!!) {
                    when {
                        startsWith("base64://") -> {
                            val imageBytes = Base64.getDecoder().decode(args["file"]!!.replace("base64://", ""))
                            val bis = ByteArrayInputStream(imageBytes)
                            image = withContext(Dispatchers.IO) { contact!!.uploadImage(bis) }
                        }
                        startsWith("http") -> {
                            image = try {
                                withContext(Dispatchers.IO) { contact!!.uploadImage(URL(args["file"]!!)) }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        else -> {
                            var fileIdOrPath = args["file"]!!
                            if (fileIdOrPath.startsWith("file:///")) {
                                fileIdOrPath = fileIdOrPath.replace("file:///", "")
                                val file = File(fileIdOrPath).absoluteFile
                                if (file.exists()) {
                                    image = contact!!.uploadImage(file)
                                }
                            } else {
                                if (fileIdOrPath.endsWith(".mnimg")) {
                                    image = Image(fileIdOrPath.replace(".mnimg", ""))
                                }
                                val file = getDataFile("image", fileIdOrPath)
                                if (file != null) {
                                    image = contact!!.uploadImage(file)
                                }
                            }
                            if (image == null) {
                                if (args.containsKey("url")) {
                                    image = withContext(Dispatchers.IO) { contact!!.uploadImage(URL(args["url"]!!)) }
                                }
                            }
                        }

                    }
                }
            } else if (args.containsKey("url")) {
                image = withContext(Dispatchers.IO) { contact!!.uploadImage(URL(args["url"])) }
            }
            if (image != null) {
                if (args["type"] == "flash") {
                    return image!!.flash()
                }
                return image as Image
            } else {
                val imageUrl = when {
                    args.containsKey("file") && args["file"]!!.startsWith("http") -> args["file"]
                    args.containsKey("url") -> args["url"]
                    else -> null
                }
                return PlainText("插件无法获取到图片" + if (imageUrl != null) ", 原图链接: $imageUrl" else "")
            }
        }
        "share" -> {
            return RichMessageHelper.share(
                args["url"]!!,
                args["title"],
                args["content"],
                args["image"]
            )
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
                "custom" -> return Music.custom(
                    args["url"]!!,
                    args["audio"]!!,
                    args["title"]!!,
                    args["content"],
                    args["image"]
                )
            }
        }
        "shake" -> {
            return PokeMessage.Poke
        }
        "poke" -> {
            PokeMessage.values.forEach {
                if (it.type == args["type"]!!.toInt() && it.id == args["id"]!!.toInt()) {
                    return it
                }
            }
            return MSG_EMPTY
        }
        "xml" -> {
            return XmlMessage(args["data"]!!)
        }
        "json" -> {
            return JsonMessage(args["data"]!!)
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
        map[parts[0]] = parts[1].unescape()
    }
    return map
}

@MiraiExperimentalAPI
suspend fun Message.toCQString(): String {
    return when (this) {
        is PlainText -> content.escape()
        is At -> "[CQ:at,qq=$target]"
        is Face -> "[CQ:face,id=$id]"
        is VipFace -> "[CQ:vipface,id=${kind.id},name=${kind.name},count=${count}]"
        is PokeMessage -> "[CQ:poke,id=${id},type=${type},name=${name}]"
        is AtAll -> "[CQ:at,qq=all]"
        is Image -> "[CQ:image,file=$imageId,url=${queryUrl()}]"  // 无需转义 https://github.com/richardchien/coolq-http-api/blob/30bfedec692cd1c383ad7561af80c4744343861f/src/cqhttp/plugins/message_enhancer/message_enhancer.cpp#L249
        is RichMessage -> "[CQ:rich,data=${content.escape()}]"
        is MessageSource -> ""
        is QuoteReply -> ""
        is Voice -> "[CQ:voice,url=${url},md5=${md5},file=${fileName}]"
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