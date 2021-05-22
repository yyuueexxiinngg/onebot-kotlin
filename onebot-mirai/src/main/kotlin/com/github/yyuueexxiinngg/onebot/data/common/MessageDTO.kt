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
import com.github.yyuueexxiinngg.onebot.util.toCQString
import com.github.yyuueexxiinngg.onebot.util.toMessageId
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
import net.mamoe.mirai.utils.MiraiInternalApi

/*
*   DTO data class
* */

// MessagePacket
@Serializable
@SerialName("GroupMessage")
data class GroupMessagePacketDTO(
    override var self_id: Long,
    val sub_type: String, // normal、anonymous、notice
    val message_id: Int,
    val group_id: Long,
    val user_id: Long,
    val anonymous: AnonymousMemberDTO?,
    var message: MessageChainOrStringDTO,  // Can be messageChainDTO or string depending on config
    val raw_message: String,
    val font: Int,
    val sender: MemberDTO,
    override var time: Long
) : EventDTO() {
    override var post_type: String = "message"
    val message_type: String = "group"
}

@Serializable
@SerialName("PrivateMessage")
data class PrivateMessagePacketDTO(
    override var self_id: Long,
    val sub_type: String, // friend、group、discuss、other
    val message_id: Int,
    val user_id: Long,
    val message: MessageChainOrStringDTO, // Can be messageChainDTO or string depending on config
    val raw_message: String,
    val font: Int,
    val sender: QQDTO,
    override var time: Long
) : EventDTO() {
    override var post_type: String = "message"
    val message_type: String = "private"
}

// Message DTO
@Serializable
@SerialName("Plain")
data class PlainDTO(val data: PlainData, val type: String = "text") : MessageDTO()

@Serializable
data class PlainData(val text: String)


@Serializable
@SerialName("At")
data class AtDTO(val data: AtData, val type: String = "at") : MessageDTO()

@Serializable
data class AtData(val qq: String)


@Serializable
@SerialName("Face")
data class FaceDTO(val data: FaceData, val type: String = "face") : MessageDTO()

@Serializable
data class FaceData(val id: String = "-1")

@Serializable
@SerialName("Image")
data class ImageDTO(val data: ImageData, val type: String = "image") : MessageDTO()

@Serializable
data class ImageData(
    val file: String? = null,
    val url: String? = null,
    val type: String? = null
)

@Serializable
@SerialName("Poke")
data class PokeMessageDTO(val data: PokeData, val type: String = "poke") : MessageDTO()

@Serializable
data class PokeData(val name: String)

@Serializable
@SerialName("Unknown")
object UnknownMessageDTO : MessageDTO()

// Only used when deserialize
@Serializable
@SerialName("AtAll")
data class AtAllDTO(val target: Long = 0) : MessageDTO() // target为保留字段

@Serializable
@SerialName("Xml")
data class XmlDTO(val data: XmlData, val type: String = "xml") : MessageDTO()

@Serializable
data class XmlData(val data: String)

@Serializable
@SerialName("App")
data class AppDTO(val data: AppData, val type: String = "json") : MessageDTO()

@Serializable
data class AppData(val data: String)

@Serializable
@SerialName("Json")
data class JsonDTO(val data: JsonData, val type: String = "json") : MessageDTO()

@Serializable
data class JsonData(val data: String)

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
@Serializable(with = MessageChainOrStringDTO.Companion::class)
sealed class MessageChainOrStringDTO {
    companion object : KSerializer<MessageChainOrStringDTO> {
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): MessageChainOrStringDTO {
            error("Not implemented")
        }

        override fun serialize(encoder: Encoder, value: MessageChainOrStringDTO) {
            when (value) {
                is WrappedMessageChainString -> {
                    String.serializer().serialize(encoder, value.value)
                }
                is WrappedMessageChainList -> {
                    WrappedMessageChainList.serializer().serialize(encoder, value)
                }
            }
        }
    }
}

@Serializable(with = WrappedMessageChainString.Companion::class)
data class WrappedMessageChainString(
    var value: String
) : MessageChainOrStringDTO() {
    companion object : KSerializer<WrappedMessageChainString> {
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): WrappedMessageChainString {
            return WrappedMessageChainString(String.serializer().deserialize(decoder))
        }

        override fun serialize(encoder: Encoder, value: WrappedMessageChainString) {
            return String.serializer().serialize(encoder, value.value)
        }
    }
}

@Serializable(with = WrappedMessageChainList.Companion::class)
data class WrappedMessageChainList(
    var value: List<MessageDTO>
) : MessageChainOrStringDTO() {
    companion object : KSerializer<WrappedMessageChainList> {
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): WrappedMessageChainList {
            error("Not implemented")
        }

        override fun serialize(encoder: Encoder, value: WrappedMessageChainList) {
            return ListSerializer(MessageDTO.serializer()).serialize(encoder, value.value)
        }
    }
}

@Serializable
sealed class MessageDTO : DTO

/*
    Extend function
 */
suspend fun MessageEvent.toDTO(isRawMessage: Boolean = false): EventDTO {
    val rawMessage = WrappedMessageChainString("")
    message.forEach { rawMessage.value += it.toCQString() }
    return when (this) {
        is GroupMessageEvent -> GroupMessagePacketDTO(
            self_id = bot.id,
            sub_type = if (sender is AnonymousMember) "anonymous" else "normal",
            message_id = message.internalId.toMessageId(bot.id, group.id),
            group_id = group.id,
            user_id = sender.id,
            anonymous = if (sender is AnonymousMember) AnonymousMemberDTO(sender as AnonymousMember) else null,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = MemberDTO(sender),
            time = currentTimeSeconds()
        )
        is FriendMessageEvent -> PrivateMessagePacketDTO(
            self_id = bot.id,
            sub_type = "friend",
            message_id = message.internalId.toMessageId(bot.id, sender.id),
            user_id = sender.id,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = QQDTO(sender),
            time = currentTimeSeconds()
        )
        is GroupTempMessageEvent -> PrivateMessagePacketDTO(
            self_id = bot.id,
            sub_type = "group",
            message_id = message.internalId.toMessageId(bot.id, sender.id),
            user_id = sender.id,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = QQDTO(sender),
            time = currentTimeSeconds()
        )
        else -> IgnoreEventDTO(sender.id)
    }
}

suspend inline fun MessageChain.toMessageChainDTO(filter: (MessageDTO) -> Boolean): WrappedMessageChainList {
    return WrappedMessageChainList(mutableListOf<MessageDTO>().apply {
        contentsSequence().forEach { content -> content.toDTO().takeIf { filter(it) }?.let(::add) }
    })
}

@OptIn(MiraiInternalApi::class)
suspend fun Message.toDTO() = when (this) {
    is At -> AtDTO(AtData(target.toString()))
    is AtAll -> AtDTO(AtData("0"))
    is Face -> FaceDTO(FaceData(id.toString()))
    is PlainText -> PlainDTO(PlainData(content))
    is Image -> ImageDTO(ImageData(imageId, queryUrl()))
    is FlashImage -> ImageDTO(ImageData(image.imageId, image.queryUrl(), "flash"))
    is ServiceMessage ->
        with(content) {
            when {
                contains("xml version") -> XmlDTO(XmlData(content))
                else -> JsonDTO(JsonData(content))
            }
        }
    is LightApp -> AppDTO(AppData(content))
//    is FlashImage -> FlashImageDTO(image.imageId, image.queryUrl())
//    is QuoteReply -> QuoteDTO(source.id, source.fromId, source.targetId,
//        groupId = when {
//            source is OfflineMessageSource && (source as OfflineMessageSource).kind == OfflineMessageSource.Kind.GROUP ||
//                    source is OnlineMessageSource && (source as OnlineMessageSource).subject is Group -> source.targetId
//            else -> 0L
//        },
//        // 避免套娃
//        origin = source.originalMessage.toMessageChainDTO { it != UnknownMessageDTO && it !is QuoteDTO })
    is PokeMessage -> PokeMessageDTO(PokeData(name))
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
