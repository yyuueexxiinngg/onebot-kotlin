/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package com.github.yyuueexxiinngg.onebot.data.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.contact.*

@Serializable
abstract class ContactDTO : DTO {
    abstract val id: Long
}

@Serializable
sealed class CQContactDTO : DTO {
    abstract val user_id: Long
    abstract val nickname: String
    abstract val sex: String?
    abstract val age: Int?
}

@Serializable
data class QQDTO(
    override val id: Long,
    val nickname: String,
    val remark: String
) : ContactDTO() {
    // TODO: queryProfile.nickname & queryRemark.value not support now
    constructor(qq: Friend) : this(qq.id, qq.nick, "")
}


@Serializable
data class MemberDTO(
    override val id: Long,
    val memberName: String,
    val permission: MemberPermission,
    val group: GroupDTO
) : ContactDTO() {
    constructor(member: Member) : this(
        member.id, member.nameCardOrNick, member.permission,
        GroupDTO(member.group)
    )
}

@Serializable
@SerialName("Member")
data class CQMemberDTO(
    override val user_id: Long,
    override val nickname: String,
    val card: String? = null,
    override val sex: String? = null,
    override val age: Int? = null,
    val area: String? = null,
    val level: String? = null,
    val role: String? = null,
    val title: String? = null
) : CQContactDTO() {
    constructor(member: Member) : this(
        member.id,
        member.nameCardOrNick,
        member.nameCardOrNick,
        "unknown",
        0,
        "unknown",
        "unknown",
        if (member.permission == MemberPermission.ADMINISTRATOR) "admin" else member.permission.name.toLowerCase(),
        "unknown"
    )
}

@Serializable
@SerialName("QQ")
data class CQQQDTO(
    override val user_id: Long,
    override val nickname: String,
    override val sex: String? = null,
    override val age: Int? = null
) : CQContactDTO() {
    constructor(contact: User) : this(
        contact.id,
        contact.nameCardOrNick,
        "unknown",
        0
    )
}

@Serializable
data class CQAnonymousMemberDTO(
    val id: Long,
    val name: String,
    val flag: String
) {
    constructor(member: AnonymousMember) : this(
        member.id,
        member.nameCard,
        member.anonymousId + "&${member.nameCard}" // Need member nick to mute
    )
}

@Serializable
data class GroupDTO(
    override val id: Long,
    val name: String,
    val permission: MemberPermission
) : ContactDTO() {
    constructor(group: Group) : this(group.id, group.name, group.botPermission)
}
