package com.github.yyuueexxiinngg.onebot.util

import com.github.yyuueexxiinngg.onebot.PluginBase
import com.github.yyuueexxiinngg.onebot.PluginSettings
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageChain.Companion.serializeToJsonString
import net.mamoe.mirai.message.data.internalId
import java.nio.ByteBuffer
import java.util.zip.CRC32

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
        PluginBase.db?.put(
            messageId.toByteArray(),
            message.serializeToJsonString().toByteArray()
        )
    }
}

//    if (PluginSettings.db.enable) {
//        val messageId = message.internalId.toCQMessageId(bot.id, subject.id)
//        PluginBase.messageStore?.put(
//            messageId,
//            message.serializeToJsonString()
//        )
//        PluginBase.db?.commit()
//    }
