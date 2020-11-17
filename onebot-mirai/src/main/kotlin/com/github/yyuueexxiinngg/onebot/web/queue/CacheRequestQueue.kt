package com.github.yyuueexxiinngg.onebot.web.queue

import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent

class CacheRequestQueue : LinkedHashMap<Long, BotEvent>() {

    var cacheSize = 512

    override fun get(key: Long): BotEvent = super.get(key) ?: throw NoSuchElementException()

    override fun put(key: Long, value: BotEvent): BotEvent? = super.put(key, value).also {
        if (size > cacheSize) {
            remove(this.entries.first().key)
        }
    }

    fun add(source: NewFriendRequestEvent) {
        put(source.eventId, source)
    }

    fun add(source: MemberJoinRequestEvent) {
        put(source.eventId, source)
    }

    fun add(source: BotInvitedJoinGroupRequestEvent) {
        put(source.eventId, source)
    }
}