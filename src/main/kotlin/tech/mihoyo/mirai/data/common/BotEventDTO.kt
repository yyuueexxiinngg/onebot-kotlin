/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package tech.mihoyo.mirai.data.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.*
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.currentTimeMillis

@Serializable
sealed class BotEventDTO : EventDTO()

@Serializable
sealed class CQBotEventDTO : CQEventDTO()

suspend fun BotEvent.toCQDTO(isRawMessage: Boolean = false): CQEventDTO {
    return when (this) {
        is MessageEvent -> toDTO(isRawMessage)
        is MemberJoinEvent -> {
            when (this) {
                is MemberJoinEvent.Active -> CQMemberJoinEventDTO(
                    self_id = bot.id,
                    sub_type = "approve",
                    group_id = group.id,
                    operator_id = 0L, // Not available in Mirai
                    user_id = member.id,
                    time = currentTimeMillis
                )
                is MemberJoinEvent.Invite -> CQMemberJoinEventDTO(
                    self_id = bot.id,
                    sub_type = "invite",
                    group_id = group.id,
                    operator_id = 0L, // Not available in Mirai
                    user_id = member.id,
                    time = currentTimeMillis
                )
                else -> CQIgnoreEventDTO(bot.id)
            }
        }
        is MemberLeaveEvent -> {
            when (this) {
                is MemberLeaveEvent.Quit -> CQMemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "leave",
                    group_id = group.id,
                    operator_id = member.id,
                    user_id = member.id,
                    time = currentTimeMillis
                )
                is MemberLeaveEvent.Kick -> CQMemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "kick",
                    group_id = group.id,
                    operator_id = operator?.id ?: bot.id,
                    user_id = member.id,
                    time = currentTimeMillis
                )
                else -> CQIgnoreEventDTO(bot.id)
            }
        }
        is BotLeaveEvent -> {
            when (this) {
                is BotLeaveEvent.Kick -> CQMemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "kick_me",
                    group_id = group.id,
                    operator_id = 0L, // Retrieve operator is currently not supported
                    user_id = bot.id,
                    time = currentTimeMillis
                )
                else -> CQIgnoreEventDTO(bot.id)
            }
        }
        is MemberPermissionChangeEvent ->
            when (this.new) {
                MemberPermission.MEMBER -> CQGroupAdministratorChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "unset",
                    group_id = group.id,
                    user_id = member.id,
                    time = currentTimeMillis
                )
                else -> CQGroupAdministratorChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "set",
                    group_id = group.id,
                    user_id = member.id,
                    time = currentTimeMillis
                )
            }
        is MemberMuteEvent -> CQGroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "ban",
            group_id = group.id,
            operator_id = operator?.id ?: bot.id,
            user_id = member.id,
            duration = durationSeconds,
            time = currentTimeMillis
        )
        is GroupMuteAllEvent -> {
            if (new) {
                CQGroupMuteChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "ban",
                    group_id = group.id,
                    operator_id = operator?.id ?: bot.id,
                    user_id = 0L,
                    duration = 0,
                    time = currentTimeMillis
                )
            } else {
                CQGroupMuteChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "lift_ban",
                    group_id = group.id,
                    operator_id = operator?.id ?: bot.id,
                    user_id = 0L,
                    duration = 0,
                    time = currentTimeMillis
                )
            }
        }
        is BotMuteEvent -> CQGroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "ban",
            group_id = group.id,
            operator_id = operator.id,
            user_id = bot.id,
            duration = durationSeconds,
            time = currentTimeMillis
        )
        is MemberUnmuteEvent -> CQGroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "lift_ban",
            group_id = group.id,
            operator_id = operator?.id ?: bot.id,
            user_id = member.id,
            duration = 0,
            time = currentTimeMillis
        )
        is BotUnmuteEvent -> CQGroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "lift_ban",
            group_id = group.id,
            operator_id = operator.id,
            user_id = bot.id,
            duration = 0,
            time = currentTimeMillis
        )
        is FriendAddEvent -> CQFriendAddEventDTO(
            self_id = bot.id,
            user_id = friend.id,
            time = currentTimeMillis
        )
        is NewFriendRequestEvent -> CQFriendRequestEventDTO(
            self_id = bot.id,
            user_id = fromId,
            comment = message,
            flag = eventId.toString(),
            time = currentTimeMillis
        )
        is MemberJoinRequestEvent -> CQGroupMemberAddRequestEventDTO(
            self_id = bot.id,
            sub_type = "add", // Invite not support by Mirai
            group_id = groupId,
            user_id = fromId,
            comment = message,
            flag = eventId.toString(),
            time = currentTimeMillis
        )
        else -> CQIgnoreEventDTO(bot.id)
    }
}


@Serializable
@SerialName("CQMetaEvent")
data class CQMetaEventDTO(
    override var self_id: Long,
    val sub_type: String, // enable、disable、connect
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "meta_event"
    val meta_event_type: String = "lifecycle"
}

@Serializable
@SerialName("CQMemberJoinEvent")
data class CQMemberJoinEventDTO(
    override var self_id: Long,
    val sub_type: String, // approve、invite
    val group_id: Long,
    val operator_id: Long,
    val user_id: Long,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_increase"
}

@Serializable
@SerialName("CQMemberLeaveEvent")
data class CQMemberLeaveEventDTO(
    override var self_id: Long,
    val sub_type: String, // leave、kick、kick_me
    val group_id: Long,
    val operator_id: Long,
    val user_id: Long,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_decrease"
}


@Serializable
@SerialName("CQGroupAdministratorChangeEvent")
data class CQGroupAdministratorChangeEventDTO(
    override var self_id: Long,
    val sub_type: String, // set、unset
    val group_id: Long,
    val user_id: Long,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_admin"
}

@Serializable
@SerialName("CQGroupMuteChangeEvent")
data class CQGroupMuteChangeEventDTO(
    override var self_id: Long,
    val sub_type: String, // ban、lift_ban
    val group_id: Long,
    val operator_id: Long,
    val user_id: Long, // Mute all = 0F
    val duration: Int,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_ban"
}

@Serializable
@SerialName("CQFriendAddEvent")
data class CQFriendAddEventDTO(
    override var self_id: Long,
    val user_id: Long,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "friend_add"
}

@Serializable
@SerialName("CQFriendRequestEvent")
data class CQFriendRequestEventDTO(
    override var self_id: Long,
    val user_id: Long,
    val comment: String,
    val flag: String,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val request_type: String = "friend"
}

@Serializable
@SerialName("CQGroupMemberAddRequestEvent")
data class CQGroupMemberAddRequestEventDTO(
    override var self_id: Long,
    val sub_type: String, // add、invite
    val group_id: Long,
    val user_id: Long,
    val comment: String,
    val flag: String,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val request_type: String = "group"
}
