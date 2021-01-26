package com.github.yyuueexxiinngg.onebot.util

import com.github.yyuueexxiinngg.onebot.PluginBase
import com.github.yyuueexxiinngg.onebot.PluginSettings
import com.github.yyuueexxiinngg.onebot.logger
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.events.UserMessageEvent
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.message.data.internalId
import java.nio.ByteBuffer
import java.util.zip.CRC32

/*@Serializable
data class PersistentMessage(
    val message_id: Int,
    val internal_ids: List<Int>,
    val bot_id: Long,
    val sender_id: Long,
    val sender_name: String,
    val is_group: Boolean,
    val group_id: Long?,
    val group_name: String?,
    val time: Long,
    val message: String
)*/

fun IntArray.toCQMessageId(botId: Long, contactId: Long): Int {
    val crc = CRC32()
    val messageId = "$botId$$contactId$${joinToString("-")}"
    crc.update(messageId.toByteArray())
    return crc.value.toInt()
}

fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(4).putInt(this).array()

fun MessageEvent.saveMessageToDB() {
    if (PluginSettings.db.enable) {
        val messageId = message.internalId.toCQMessageId(bot.id, subject.id)
        when (this) {
            is GroupMessageEvent -> {
                logger.debug(
                    message.serializeToJsonString()
                )
                PluginBase.db?.put(
                    messageId.toByteArray(),
                    message.serializeToJsonString().toByteArray()
/*                    Json.encodeToString(
                        PersistentMessage(
                            message_id = messageId,
                            internal_ids = message.internalId.toList(),
                            bot_id = bot.id,
                            is_group = true,
                            sender_id = sender.id,
                            sender_name = sender.nameCardOrNick,
                            group_id = group.id,
                            group_name = group.name,
                            time = message.time.toLong(),
                            message = message.serializeToJsonString()
                        )
                    ).toByteArray()*/
                )
                PluginBase.db?.apply {
                    logger.debug(String(get(messageId.toByteArray())))
                }
            }
            is UserMessageEvent -> {
                PluginBase.db?.put(
                    messageId.toByteArray(),
                    message.serializeToJsonString().toByteArray()
                )
            }
        }
    }
}