/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package tech.mihoyo.mirai.data.common

import kotlinx.serialization.*
import kotlinx.serialization.builtins.list
import kotlinx.serialization.builtins.serializer
import tech.mihoyo.mirai.coolq.api.http.util.PokeMap
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.MiraiExperimentalAPI
import net.mamoe.mirai.utils.currentTimeMillis
import tech.mihoyo.mirai.util.toCQString
import java.net.URL

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
@SerialName("text")
data class CQPlainDTO(val data: CQPlainData) : MessageDTO()

@Serializable
data class CQPlainData(val text: String)


@Serializable
@SerialName("at")
data class CQAtDTO(val data: CQAtData) : MessageDTO()

@Serializable
data class CQAtData(val qq: Long)


@Serializable
@SerialName("face")
data class CQFaceDTO(val data: CQFaceData) : MessageDTO()

@Serializable
data class CQFaceData(val id: Int = -1)

@Serializable
@SerialName("image")
data class CQImageDTO(val data: CQImageData) : MessageDTO()

@Serializable
data class CQImageData(
    val file: String? = null,
    val url: String? = null
)

@Serializable
@SerialName("shake")
data class CQPokeMessageDTO(val data: CQPokeData) : MessageDTO()

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
data class XmlDTO(val xml: String) : MessageDTO()

@Serializable
@SerialName("App")
data class AppDTO(val content: String) : MessageDTO()


@Serializable
@SerialName("Json")
data class JsonDTO(val json: String) : MessageDTO()

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

        @OptIn(UnstableDefault::class)
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

        @OptIn(UnstableDefault::class)
        override fun serialize(encoder: Encoder, value: WrappedCQMessageChainList) {
            return MessageDTO.serializer().list.serialize(encoder, value.value)
        }
    }
}

@Serializable
sealed class MessageDTO : DTO

/*
    Extend function
 */
@MiraiExperimentalAPI
suspend fun MessageEvent.toDTO(isRawMessage: Boolean = false): CQEventDTO {
    val rawMessage = WrappedCQMessageChainString("")
    message.forEach { rawMessage.value += it.toCQString() }
    return when (this) {
        is GroupMessageEvent -> CQGroupMessagePacketDTO(
            self_id = bot.id,
            sub_type = "normal",
            message_id = message.id,
            group_id = group.id,
            user_id = sender.id,
            anonymous = null,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = CQMemberDTO(sender),
            time = currentTimeMillis
        )
        is FriendMessageEvent -> CQPrivateMessagePacketDTO(
            self_id = bot.id,
            sub_type = "friend",
            message_id = message.id,
            user_id = sender.id,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = CQQQDTO(sender),
            time = currentTimeMillis
        )
        is TempMessageEvent -> CQPrivateMessagePacketDTO(
            self_id = bot.id,
            sub_type = "group", // QQ don't have discuss anymore
            message_id = message.id,
            user_id = sender.id,
            message = if (isRawMessage) rawMessage else message.toMessageChainDTO { it != UnknownMessageDTO },
            raw_message = rawMessage.value,
            font = 0,
            sender = CQQQDTO(sender),
            time = currentTimeMillis
        )
        else -> CQIgnoreEventDTO(sender.id)
    }
}

suspend inline fun MessageChain.toMessageChainDTO(filter: (MessageDTO) -> Boolean): WrappedCQMessageChainList {
    return WrappedCQMessageChainList(mutableListOf<MessageDTO>().apply {
        forEachContent { content -> content.toDTO().takeIf { filter(it) }?.let(::add) }
    })
}

suspend fun Message.toDTO() = when (this) {
    is At -> CQAtDTO(CQAtData(target))
    is AtAll -> CQAtDTO(CQAtData(0L))
    is Face -> CQFaceDTO(CQFaceData(id))
    is PlainText -> CQPlainDTO(CQPlainData(content))
    is Image -> {
        CQImageDTO(CQImageData(imageId, queryUrl()))
    }
    is ServiceMessage -> XmlDTO(content)
    is LightApp -> AppDTO(content)
//    is FlashImage -> FlashImageDTO(image.imageId, image.queryUrl())
//    is QuoteReply -> QuoteDTO(source.id, source.fromId, source.targetId,
//        groupId = when {
//            source is OfflineMessageSource && (source as OfflineMessageSource).kind == OfflineMessageSource.Kind.GROUP ||
//                    source is OnlineMessageSource && (source as OnlineMessageSource).subject is Group -> source.targetId
//            else -> 0L
//        },
//        // 避免套娃
//        origin = source.originalMessage.toMessageChainDTO { it != UnknownMessageDTO && it !is QuoteDTO })
    is PokeMessage -> CQPokeMessageDTO(CQPokeData(PokeMap[type]))
    else -> UnknownMessageDTO
}

suspend fun MessageDTO.toMessage(contact: Contact) = when (this) {
    is CQAtDTO -> At((contact as Group)[data.qq])
    is AtAllDTO -> AtAll
    is CQFaceDTO -> when {
        data.id >= 0 -> Face(data.id)
        else -> Face(Face.unknown)
    }
    is CQPlainDTO -> PlainText(data.text)
    is CQImageDTO -> when {
        !data.file.isNullOrBlank() -> Image(data.file)
        !data.url.isNullOrBlank() -> contact.uploadImage(URL(data.url))
        else -> null
    }
    is XmlDTO -> ServiceMessage(60, xml)
    is JsonDTO -> ServiceMessage(1, json)
    is AppDTO -> LightApp(content)
    is CQPokeMessageDTO -> PokeMap[data.name]
    // ignore
    is UnknownMessageDTO
    -> null
}