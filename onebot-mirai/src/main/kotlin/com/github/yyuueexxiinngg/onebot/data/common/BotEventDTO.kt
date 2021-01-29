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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.utils.MiraiExperimentalApi

@Serializable
sealed class BotEventDTO : EventDTO()

@Serializable
sealed class CQBotEventDTO : CQEventDTO()

@OptIn(MiraiExperimentalApi::class)
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
                    time = currentTimeSeconds()
                )
                is MemberJoinEvent.Invite -> CQMemberJoinEventDTO(
                    self_id = bot.id,
                    sub_type = "invite",
                    group_id = group.id,
                    operator_id = 0L, // Not available in Mirai
                    user_id = member.id,
                    time = currentTimeSeconds()
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
                    time = currentTimeSeconds()
                )
                is MemberLeaveEvent.Kick -> CQMemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "kick",
                    group_id = group.id,
                    operator_id = operator?.id ?: bot.id,
                    user_id = member.id,
                    time = currentTimeSeconds()
                )
                else -> CQIgnoreEventDTO(bot.id)
            }
        }
        is BotJoinGroupEvent.Active -> CQMemberJoinEventDTO(
            self_id = bot.id,
            sub_type = "approve",
            group_id = group.id,
            operator_id = 0L, // Not available in Mirai
            user_id = bot.id,
            time = currentTimeSeconds()
        )
        is BotJoinGroupEvent.Invite -> CQMemberJoinEventDTO(
            self_id = bot.id,
            sub_type = "invite",
            group_id = group.id,
            operator_id = 0L, // Not available in Mirai
            user_id = bot.id,
            time = currentTimeSeconds()
        )

        is BotLeaveEvent -> {
            when (this) {
                is BotLeaveEvent.Kick -> CQMemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "kick_me",
                    group_id = group.id,
                    operator_id = 0L, // Retrieve operator is currently not supported
                    user_id = bot.id,
                    time = currentTimeSeconds()
                )
                is BotLeaveEvent.Active -> CQMemberLeaveEventDTO(
                    self_id = bot.id,
                    sub_type = "kick_me",
                    group_id = group.id,
                    operator_id = 0L, // Retrieve operator is currently not supported
                    user_id = bot.id,
                    time = currentTimeSeconds()
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
                    time = currentTimeSeconds()
                )
                else -> CQGroupAdministratorChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "set",
                    group_id = group.id,
                    user_id = member.id,
                    time = currentTimeSeconds()
                )
            }
        is MemberMuteEvent -> CQGroupMuteChangeEventDTO(
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
                CQGroupMuteChangeEventDTO(
                    self_id = bot.id,
                    sub_type = "ban",
                    group_id = group.id,
                    operator_id = operator?.id ?: bot.id,
                    user_id = 0L,
                    duration = 0,
                    time = currentTimeSeconds()
                )
            } else {
                CQGroupMuteChangeEventDTO(
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
        is BotMuteEvent -> CQGroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "ban",
            group_id = group.id,
            operator_id = operator.id,
            user_id = bot.id,
            duration = durationSeconds,
            time = currentTimeSeconds()
        )
        is MemberUnmuteEvent -> CQGroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "lift_ban",
            group_id = group.id,
            operator_id = operator?.id ?: bot.id,
            user_id = member.id,
            duration = 0,
            time = currentTimeSeconds()
        )
        is BotUnmuteEvent -> CQGroupMuteChangeEventDTO(
            self_id = bot.id,
            sub_type = "lift_ban",
            group_id = group.id,
            operator_id = operator.id,
            user_id = bot.id,
            duration = 0,
            time = currentTimeSeconds()
        )
        is FriendAddEvent -> CQFriendAddEventDTO(
            self_id = bot.id,
            user_id = friend.id,
            time = currentTimeSeconds()
        )
        is NewFriendRequestEvent -> CQFriendRequestEventDTO(
            self_id = bot.id,
            user_id = fromId,
            comment = message,
            flag = eventId.toString(),
            time = currentTimeSeconds()
        )
        is MemberJoinRequestEvent -> CQGroupMemberAddRequestEventDTO(
            self_id = bot.id,
            sub_type = "add",
            group_id = groupId,
            user_id = fromId,
            comment = message,
            flag = eventId.toString(),
            time = currentTimeSeconds()
        )
        is BotInvitedJoinGroupRequestEvent -> CQGroupMemberAddRequestEventDTO(
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
                is Group -> CQGroupMemberNudgedEventDTO(
                    self_id = bot.id,
                    group_id = subject.id,
                    user_id = from.id,
                    target_id = target.id,
                    time = currentTimeSeconds()
                )
                else -> {
                    // OneBot not yet provides private nudged event standard.
                    logger.info("私聊被戳事件已被插件忽略: $this")
                    CQIgnoreEventDTO(bot.id)
                }
            }
        }
        is MessageRecallEvent -> {
            when (this) {
                is MessageRecallEvent.GroupRecall -> {
                    CQGroupMessageRecallEventDTO(
                        self_id = bot.id,
                        group_id = group.id,
                        user_id = authorId,
                        operator_id = operator?.id ?: bot.id,
                        message_id = messageInternalIds.toCQMessageId(bot.id, group.id),
                        time = currentTimeSeconds()
                    )
                }
                is MessageRecallEvent.FriendRecall -> {
                    CQFriendMessageRecallEventDTO(
                        self_id = bot.id,
                        user_id = operatorId,
                        message_id = messageInternalIds.toCQMessageId(bot.id, operatorId),
                        time = currentTimeSeconds()
                    )
                }
                else -> {
                    logger.debug("发生讨论组消息撤回事件, 已被插件忽略: $this")
                    CQIgnoreEventDTO(bot.id)
                }
            }
        }
        is MemberHonorChangeEvent -> {
            CQMemberHonorChangeEventDTO(
                self_id = bot.id,
                user_id = user.id,
                group_id = group.id,
                honor_type = honorType.name.toLowerCase(),
                time = currentTimeSeconds()
            )
        }
        else -> {
            logger.debug("发生了被插件忽略的事件: $this")
            CQIgnoreEventDTO(bot.id)
        }
    }
}


@Serializable
@SerialName("CQLifecycleMetaEvent")
data class CQLifecycleMetaEventDTO(
    override var self_id: Long,
    val sub_type: String, // enable、disable、connect
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "meta_event"
    val meta_event_type: String = "lifecycle"
}

@Serializable
@SerialName("CQHeartbeatMetaEvent")
data class CQHeartbeatMetaEventDTO(
    override var self_id: Long,
    override var time: Long,
    val status: CQPluginStatusData,
    val interval: Long,
) : CQBotEventDTO() {
    override var post_type: String = "meta_event"
    val meta_event_type: String = "heartbeat"
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
    override var post_type: String = "request"
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
    override var post_type: String = "request"
    val request_type: String = "group"
}

@Serializable
@SerialName("CQGroupMemberNudgedEvent")
data class CQGroupMemberNudgedEventDTO(
    override var self_id: Long,
    val sub_type: String = "poke",
    val group_id: Long,
    val user_id: Long,
    val target_id: Long,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "notify"
}

@Serializable
@SerialName("CQGroupMessageRecallEvent")
data class CQGroupMessageRecallEventDTO(
    override var self_id: Long,
    val group_id: Long,
    val user_id: Long,
    val operator_id: Long,
    val message_id: Int,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "group_recall"
}

@Serializable
@SerialName("CQFriendMessageRecallEvent")
data class CQFriendMessageRecallEventDTO(
    override var self_id: Long,
    val user_id: Long,
    val message_id: Int,
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "friend_recall"
}

@Serializable
@SerialName("CQMemberHonorChangeEvent")
data class CQMemberHonorChangeEventDTO(
    override var self_id: Long,
    val sub_type: String = "honor",
    val user_id: Long,
    val group_id: Long,
    val honor_type: String = "talkative",
    override var time: Long
) : CQBotEventDTO() {
    override var post_type: String = "notice"
    val notice_type: String = "notify"
}
