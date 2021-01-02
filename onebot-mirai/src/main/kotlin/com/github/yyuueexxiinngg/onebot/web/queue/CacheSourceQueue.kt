/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package com.github.yyuueexxiinngg.onebot.web.queue

import com.github.yyuueexxiinngg.onebot.util.toCQMessageId
import net.mamoe.mirai.message.data.MessageSource

class CacheSourceQueue : LinkedHashMap<Int, MessageSource>() {

    var cacheSize = 1024

    override fun get(key: Int): MessageSource = super.get(key) ?: throw NoSuchElementException()

    override fun put(key: Int, value: MessageSource): MessageSource? = super.put(key, value).also {
        if (size > cacheSize) {
            remove(this.entries.first().key)
        }
    }

    fun add(source: MessageSource) {
        put(source.internalIds.toCQMessageId(source.botId, source.fromId), source)
    }
}