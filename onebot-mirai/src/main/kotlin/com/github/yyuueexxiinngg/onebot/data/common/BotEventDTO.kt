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
import com.github.yyuueexxiinngg.onebot.util.toMessageId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.MiraiExperimentalApi

@Serializable
sealed class BotEventDTO : EventDTO()

@OptIn(MiraiExperimentalApi::class)
suspend fun BotEvent.toDTO(isRawMessage: Boolean = false): EventDTO {
    return when (this) {
        is MessageEvent -> this.toDTO(isRawMessage)
        is MemberJoinEvent -> {
            when (this) {
                is MemberJoinEvent.Active -> MemberJoinEventDTO(
                    self_id = bot.id,
                    sub_type = "approve",
                    group_id = group.id,
                    operator_id = 0L, // Not available in Mirai
                    user_id = member.id,
                    time = currentTimeSeconds()
                )
                is MemberJoinEvent.Invite -> MemberJoinEventDTO(
                    self_id = bot.id,
                    sub_type = "invite",
                    group_id = group.id,
                    operator_id = 0L, // Not available in Mirai
                    user_id = member.id,
                    time = currentTimeSeconds()
                )
                else -> IgnoreEventDTO(bot.id)
            }
        }
        is MemberLeaveEvent -> {
            when (this) {
                is MemberLeaveEvent.Quit -> MemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "leave",
                    group_id = group.id,
                    operator_id = member.id,
                    user_id = member.id,
                    time = currentTimeSeconds()
                )
                is MemberLeaveEvent.Kick -> MemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "kick",
                    group_id = group.id,
                    operator_id = operator?.id ?: bot.id,
                    user_id = member.id,
                    time = currentTimeSeconds()
                )
                else -> IgnoreEventDTO(bot.id)
            }
        }
        is BotJoinGroupEvent.Active -> MemberJoinEventDTO(
            self_id = bot.id,
            sub_type = "approve",
            group_id = group.id,
            operator_id = 0L, // Not available in Mirai
            user_id = bot.id,
            time = currentTimeSeconds()
        )
        is BotJoinGroupEvent.Invite -> MemberJoinEventDTO(
            self_id = bot.id,
            sub_type = "invite",
            group_id = group.id,
            operator_id = 0L, // Not available in Mirai
            user_id = bot.id,
            time = currentTimeSeconds()
        )

        is BotLeaveEvent -> {
            when (this) {
                is BotLeaveEvent.Kick -> MemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "kick_me",
                    group_id = group.id,
                    operator_id = 0L, // Retrieve operator is currently not supported
                    user_id = bot.id,
                    time = currentTimeSeconds()
                )
                is BotLeaveEvent.Active -> MemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "kick_me",
                    group_id = group.id,
                    operator_id = 0L, // Retrieve operator is currently not supported
                    user_id = bot.id,
                    time = currentTimeSeconds()
                )
                else -> IgnoreEventDTO(bot.id)
            }
        }
        is MemberPermissionChangeEvent ->
            when (this.new) {
                MemberPermission.MEMBER -> GroupAdministratorChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "unset",
                    group_id = group.id,
                    user_id = member.id,
                    time = currentTimeSeconds()
                )
                else -> GroupAdministratorChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "set",
                    group_id = group.id,
                    user_id = member.id,
                    time = currentTimeSeconds()
                )
            }
        is MemberMuteEvent -> GroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "ban",
            group_id = group.id,
            operator_id = operator?.id ?: bot.id,
            user_id = member.id,
            duration = durationSeconds,
            time = currentTimeSeconds()
        )
        is GroupMuteAllEvent -> {
            if (new) {
                GroupMuteChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "ban",
                    group_id = group.id,
                    operator_id = operator?.id ?: bot.id,
                    user_id = 0L,
                    duration = 0,
                    time = currentTimeSeconds()
                )
            } else {
                GroupMuteChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "lift_ban",
                    group_id = group.id,
                    operator_id = operator?.id ?: bot.id,
                    user_id = 0L,
                    duration = 0,
                    time = currentTimeSeconds()
                )
            }
        }
        is BotMuteEvent -> GroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "ban",
            group_id = group.id,
            operator_id = operator.id,
            user_id = bot.id,
            duration = durationSeconds,
            time = currentTimeSeconds()
        )
        is MemberUnmuteEvent -> GroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "lift_ban",
            group_id = group.id,
            operator_id = operator?.id ?: bot.id,
            user_id = member.id,
            duration = 0,
            time = currentTimeSeconds()
        )
        is BotUnmuteEvent -> GroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "lift_ban",
            group_id = group.id,
            operator_id = operator.id,
            user_id = bot.id,
            duration = 0,
            time = currentTimeSeconds()
        )
        is FriendAddEvent -> FriendAddEventDTO(
            self_id = bot.id,
            user_id = friend.id,
            time = currentTimeSeconds()
        )
        is NewFriendRequestEvent -> FriendRequestEventDTO(
            self_id = bot.id,
            user_id = fromId,
            comment = message,
            flag = eventId.toString(),
            time = currentTimeSeconds()
        )
        is MemberJoinRequestEvent -> GroupMemberAddRequestEventDTO(
            self_id = bot.id,
            sub_type = "add",
            group_id = groupId,
            user_id = fromId,
            comment = message,
            flag = eventId.toString(),
            time = currentTimeSeconds()
        )
        is BotInvitedJoinGroupRequestEvent -> GroupMemberAddRequestEventDTO(
            self_id = bot.id,
            sub_type = "invite",
            group_id = groupId,
            user_id = invitorId,
            comment = "",
            flag = eventId.toString(),
            time = currentTimeSeconds()
        )
        is NudgeEvent -> {
            when (subject) {
                is Group -> GroupMemberNudgedEventDTO(
                    self_id = bot.id,
                    group_id = subject.id,
                    user_id = from.id,
                    target_id = target.id,
                    time = currentTimeSeconds()
                )
                else -> {
                    // OneBot not yet provides private nudged event standard.
                    logger.info("私聊被戳事件已被插件忽略: $this")
                    IgnoreEventDTO(bot.id)
                }
            }
        }
        is MessageRecallEvent -> {
            when (this) {
                is MessageRecallEvent.GroupRecall -> {
                    GroupMessageRecallEventDTO(
                        self_id = bot.id,
                        group_id = group.id,
                        user_id = authorId,
                        operator_id = operator?.id ?: bot.id,
                        message_id = messageInternalIds.toMessageId(bot.id, operator?.id ?: bot.id),
                        time = currentTimeSeconds()
                    )
                }
                is MessageRecallEvent.FriendRecall -> {
                    FriendMessageRecallEventDTO(
                        self_id = bot.id,
                        user_id = operatorId,
                        message_id = messageInternalIds.toMessageId(bot.id, operatorId),
                        time = currentTimeSeconds()
                    )
                }
                else -> {
                    logger.debug("发生讨论组消息撤回事件, 已被插件忽略: $this")
                    IgnoreEventDTO(bot.id)
                }
            }
        }
        is MemberHonorChangeEvent -> {
            MemberHonorChangeEventDTO(
                self_id = bot.id,
                user_id = user.id,
                group_id = group.id,
                honor_type = honorType.name.lowercase(),
                time = currentTimeSeconds()
            )
        }
        else -> {
            logger.debug("发生了被插件忽略的事件: $this")
            IgnoreEventDTO(bot.id)
        }
    }
}


@Serializable
@SerialName("LifecycleMetaEvent")
data class LifecycleMetaEventDTO(
    override var self_id: Long,
    val sub_type: String, // enable、disable、connect
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "meta_event"
    val meta_event_type: String = "lifecycle"
}

@Serializable
@SerialName("HeartbeatMetaEvent")
data class HeartbeatMetaEventDTO(
    override var self_id: Long,
    override var time: Long,
    val status: PluginStatusData,
    val interval: Long,
) : BotEventDTO() {
    override var post_type: String = "meta_event"
    val meta_event_type: String = "heartbeat"
}

@Serializable
@SerialName("MemberJoinEvent")
data class MemberJoinEventDTO(
    override var self_id: Long,
    val sub_type: String, // approve、invite
    val group_id: Long,
    val operator_id: Long,
    val user_id: Long,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_increase"
}

@Serializable
@SerialName("MemberLeaveEvent")
data class MemberLeaveEventDTO(
    override var self_id: Long,
    val sub_type: String, // leave、kick、kick_me
    val group_id: Long,
    val operator_id: Long,
    val user_id: Long,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_decrease"
}


@Serializable
@SerialName("GroupAdministratorChangeEvent")
data class GroupAdministratorChangeEventDTO(
    override var self_id: Long,
    val sub_type: String, // set、unset
    val group_id: Long,
    val user_id: Long,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_admin"
}

@Serializable
@SerialName("GroupMuteChangeEvent")
data class GroupMuteChangeEventDTO(
    override var self_id: Long,
    val sub_type: String, // ban、lift_ban
    val group_id: Long,
    val operator_id: Long,
    val user_id: Long, // Mute all = 0F
    val duration: Int,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_ban"
}

@Serializable
@SerialName("FriendAddEvent")
data class FriendAddEventDTO(
    override var self_id: Long,
    val user_id: Long,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "friend_add"
}

@Serializable
@SerialName("FriendRequestEvent")
data class FriendRequestEventDTO(
    override var self_id: Long,
    val user_id: Long,
    val comment: String,
    val flag: String,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "request"
    val request_type: String = "friend"
}

@Serializable
@SerialName("GroupMemberAddRequestEvent")
data class GroupMemberAddRequestEventDTO(
    override var self_id: Long,
    val sub_type: String, // add、invite
    val group_id: Long,
    val user_id: Long,
    val comment: String,
    val flag: String,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "request"
    val request_type: String = "group"
}

@Serializable
@SerialName("GroupMemberNudgedEvent")
data class GroupMemberNudgedEventDTO(
    override var self_id: Long,
    val sub_type: String = "poke",
    val group_id: Long,
    val user_id: Long,
    val target_id: Long,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "notify"
}

@Serializable
@SerialName("GroupMessageRecallEvent")
data class GroupMessageRecallEventDTO(
    override var self_id: Long,
    val group_id: Long,
    val user_id: Long,
    val operator_id: Long,
    val message_id: Int,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_recall"
}

@Serializable
@SerialName("FriendMessageRecallEvent")
data class FriendMessageRecallEventDTO(
    override var self_id: Long,
    val user_id: Long,
    val message_id: Int,
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "friend_recall"
}

@Serializable
@SerialName("MemberHonorChangeEvent")
data class MemberHonorChangeEventDTO(
    override var self_id: Long,
    val sub_type: String = "honor",
    val user_id: Long,
    val group_id: Long,
    val honor_type: String = "talkative",
    override var time: Long
) : BotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "notify"
}
