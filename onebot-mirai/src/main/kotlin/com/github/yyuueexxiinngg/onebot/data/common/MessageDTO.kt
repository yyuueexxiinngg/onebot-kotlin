/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package com.github.yyuueexxiinngg.onebot.data.common

import com.github.yyuueexxiinngg.onebot.logger
import com.github.yyuueexxiinngg.onebot.util.currentTimeSeconds
import com.github.yyuueexxiinngg.onebot.util.toCQMessageId
import com.github.yyuueexxiinngg.onebot.util.toCQString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.mamoe.mirai.contact.AnonymousMember
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl

/*
*   DTO data class
* */

// MessagePacket
@Serializable
@SerialName("GroupMessage")
data class CQGroupMessagePacketDTO(
    override var self_id: Long,
    val sub_type: String, // normal、anonymous、notice
    val message_id: Int,
    val group_id: Long,
    val user_id: Long,
    val anonymous: CQAnonymousMemberDTO?,
    var message: CQMessageChainOrStringDTO,  // Can be messageChainDTO or string depending on config
    val raw_message: String,
    val font: Int,
    val sender: CQMemberDTO,
    override var time: Long
) : CQEventDTO() {
    override var post_type: String = "message"
    val message_type: String = "group"
}

@Serializable
@SerialName("PrivateMessage")
data class CQPrivateMessagePacketDTO(
    override var self_id: Long,
    val sub_type: String, // friend、group、discuss、other
    val message_id: Int,
    val user_id: Long,
    val message: CQMessageChainOrStringDTO, // Can be messageChainDTO or string depending on config
    val raw_message: String,
    val font: Int,
    val sender: CQQQDTO,
    override var time: Long
) : CQEventDTO() {
    override var post_type: String = "message"
    val message_type: String = "private"
}

// Message DTO
@Serializable
@SerialName("Plain")
data class CQPlainDTO(val data: CQPlainData, val type: String = "text") : MessageDTO()

@Serializable
data class CQPlainData(val text: String)


@Serializable
@SerialName("At")
data class CQAtDTO(val data: CQAtData, val type: String = "at") : MessageDTO()

@Serializable
data class CQAtData(val qq: String)


@Serializable
@SerialName("Face")
data class CQFaceDTO(val data: CQFaceData, val type: String = "face") : MessageDTO()

@Serializable
data class CQFaceData(val id: String = "-1")

@Serializable
@SerialName("Image")
data class CQImageDTO(val data: CQImageData, val type: String = "image") : MessageDTO()

@Serializable
data class CQImageData(
    val file: String? = null,
    val url: String? = null,
    val type: String? = null
)

@Serializable
@SerialName("Poke")
data class CQPokeMessageDTO(val data: CQPokeData, val type: String = "poke") : MessageDTO()

@Serializable
data class CQPokeData(val name: String)

@Serializable
@SerialName("Unknown")
object UnknownMessageDTO : MessageDTO()

// Only used when deserialize
@Serializable
@SerialName("AtAll")
data class AtAllDTO(val target: Long = 0) : MessageDTO() // target为保留字段

@Serializable
@SerialName("Xml")
data class XmlDTO(val data: CQXmlData, val type: String = "xml") : MessageDTO()

@Serializable
data class CQXmlData(val data: String)

@Serializable
@SerialName("App")
data class AppDTO(val data: CQAppData, val type: String = "json") : MessageDTO()

@Serializable
data class CQAppData(val data: String)

@Serializable
@SerialName("Json")
data class JsonDTO(val data: CQJsonData, val type: String = "json") : MessageDTO()

@Serializable
data class CQJsonData(val data: String)

/*@Serializable
@SerialName("Source")
data class MessageSourceDTO(val id: Int, val time: Int) : MessageDTO()*/

/*@Serializable
@SerialName("Quote")
data class QuoteDTO(
    val id: Int,
    val senderId: Long,
    val targetId: Long,
    val groupId: Long,
    val origin: MessageChainDTO
) : MessageDTO()*/


/**
 * Hacky way to get message chain can be both String or List<MessageDTO>
 */
@Serializable(with = CQMessageChainOrStringDTO.Companion::class)
sealed class CQMessageChainOrStringDTO {
    companion object : KSerializer<CQMessageChainOrStringDTO> {
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): CQMessageChainOrStringDTO {
            error("Not implemented")
        }

        override fun serialize(encoder: Encoder, value: CQMessageChainOrStringDTO) {
            when (value) {
                is WrappedCQMessageChainString -> {
                    String.serializer().serialize(encoder, value.value)
                }
                is WrappedCQMessageChainList -> {
                    WrappedCQMessageChainList.serializer().serialize(encoder, value)
                }
            }
        }
    }
}

@Serializable(with = WrappedCQMessageChainString.Companion::class)
data class WrappedCQMessageChainString(
    var value: String
) : CQMessageChainOrStringDTO() {
    companion object : KSerializer<WrappedCQMessageChainString> {
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): WrappedCQMessageChainString {
            return WrappedCQMessageChainString(String.serializer().deserialize(decoder))
        }

        override fun serialize(encoder: Encoder, value: WrappedCQMessageChainString) {
            return String.serializer().serialize(encoder, value.value)
        }
    }
}

@Serializable(with = WrappedCQMessageChainList.Companion::class)
data class WrappedCQMessageChainList(
    var value: List<MessageDTO>
) : CQMessageChainOrStringDTO() {
    companion object : KSerializer<WrappedCQMessageChainList> {
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): WrappedCQMessageChainList {
            error("Not implemented")
        }

        override fun serialize(encoder: Encoder, value: WrappedCQMessageChainList) {
            return ListSerializer(MessageDTO.serializer()).serialize(encoder, value.value)
        }
    }
}

@Serializable
sealed class MessageDTO : DTO

/*
    Extend function
 */
suspend fun MessageEvent.toDTO(isRawMessage: Boolean = false): CQEventDTO {
    val rawMessage = WrappedCQMessageChainString("")
    message.forEach { rawMessage.value += it.toCQString() }
    return when (this) {
        is GroupMessageEvent -> CQGroupMessagePacketDTO(
            self_id = bot.id,
            sub_type = if (sender is AnonymousMember) "anonymous" else "normal",
            message_id = message.internalId.toCQMessageId(bot.id, group.id),
            group_id = group.id,
            user_id = sender.id,
            anonymous = if (sender is AnonymousMember) CQAnonymousMemberDTO(sender as AnonymousMember) else null,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = CQMemberDTO(sender),
            time = currentTimeSeconds()
        )
        is FriendMessageEvent -> CQPrivateMessagePacketDTO(
            self_id = bot.id,
            sub_type = "friend",
            message_id = message.internalId.toCQMessageId(bot.id, sender.id),
            user_id = sender.id,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = CQQQDTO(sender),
            time = currentTimeSeconds()
        )
        is GroupTempMessageEvent -> CQPrivateMessagePacketDTO(
            self_id = bot.id,
            sub_type = "group",
            message_id = message.internalId.toCQMessageId(bot.id, sender.id),
            user_id = sender.id,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = CQQQDTO(sender),
            time = currentTimeSeconds()
        )
        else -> CQIgnoreEventDTO(sender.id)
    }
}

suspend inline fun MessageChain.toMessageChainDTO(filter: (MessageDTO) -> Boolean): WrappedCQMessageChainList {
    return WrappedCQMessageChainList(mutableListOf<MessageDTO>().apply {
        contentsSequence().forEach { content -> content.toDTO().takeIf { filter(it) }?.let(::add) }
    })
}

suspend fun Message.toDTO() = when (this) {
    is At -> CQAtDTO(CQAtData(target.toString()))
    is AtAll -> CQAtDTO(CQAtData("0"))
    is Face -> CQFaceDTO(CQFaceData(id.toString()))
    is PlainText -> CQPlainDTO(CQPlainData(content))
    is Image -> CQImageDTO(CQImageData(imageId, queryUrl()))
    is FlashImage -> CQImageDTO(CQImageData(image.imageId, image.queryUrl(), "flash"))
    is ServiceMessage ->
        with(content) {
            when {
                contains("xml version") -> XmlDTO(CQXmlData(content))
                else -> JsonDTO(CQJsonData(content))
            }
        }
    is LightApp -> AppDTO(CQAppData(content))
//    is FlashImage -> FlashImageDTO(image.imageId, image.queryUrl())
//    is QuoteReply -> QuoteDTO(source.id, source.fromId, source.targetId,
//        groupId = when {
//            source is OfflineMessageSource && (source as OfflineMessageSource).kind == OfflineMessageSource.Kind.GROUP ||
//                    source is OnlineMessageSource && (source as OnlineMessageSource).subject is Group -> source.targetId
//            else -> 0L
//        },
//        // 避免套娃
//        origin = source.originalMessage.toMessageChainDTO { it != UnknownMessageDTO && it !is QuoteDTO })
    is PokeMessage -> CQPokeMessageDTO(CQPokeData(name))
    else -> {
        logger.debug("收到未支持消息: $this")
        UnknownMessageDTO
    }
}
/*
@OptIn(MiraiInternalApi::class, MiraiExperimentalApi::class)
suspend fun MessageDTO.toMessage(contact: Contact) = when (this) {
    is CQAtDTO -> (contact as Group)[data.qq]?.let { At(it) }
    is AtAllDTO -> AtAll
    is CQFaceDTO -> when {
        data.id >= 0 -> Face(data.id)
        else -> Face(255)
    }
    is CQPlainDTO -> PlainText(data.text)
    is CQImageDTO -> when {
        !data.file.isNullOrBlank() -> Image(data.file)
        !data.url.isNullOrBlank() -> withContext(Dispatchers.IO) { URL(data.url).openStream().uploadAsImage(contact) }
        else -> null
    }
    is XmlDTO -> SimpleServiceMessage(60, data.data)
    is JsonDTO -> SimpleServiceMessage(1, data.data)
    is AppDTO -> LightApp(data.data)
    is CQPokeMessageDTO -> PokeMap[data.name]
    // ignore
    is UnknownMessageDTO
    -> null
}*/
