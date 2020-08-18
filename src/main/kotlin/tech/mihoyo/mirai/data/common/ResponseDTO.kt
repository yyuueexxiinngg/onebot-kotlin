package tech.mihoyo.mirai.data.common

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.data.GroupHonorListData

@Serializable
sealed class CQResponseDataDTO

@Serializable
open class CQResponseDTO(
    val status: String,
    val retcode: Int,
    val data: @Serializable(with = ResponseDataSerializer::class) Any?,
    var echo: @ContextualSerialization Any? = null
) {
    class CQGeneralSuccess : CQResponseDTO("ok", 0, null)
    class CQAsyncStarted : CQResponseDTO("async", 1, null)
    class CQMiraiFailure : CQResponseDTO("failed", 102, null)
    class CQPluginFailure : CQResponseDTO("failed", 103, null)
    class CQInvalidRequest : CQResponseDTO("failed", 100, null)
    class CQMessageResponse(message_id: Int) : CQResponseDTO("ok", 0, CQMessageData(message_id))
    class CQLoginInfo(user_id: Long, nickname: String) : CQResponseDTO("ok", 0, CQLoginInfoData(user_id, nickname))
    class CQFriendList(friendList: List<CQFriendData>) : CQResponseDTO("ok", 0, friendList)

    class CQGroupList(groupList: List<CQGroupData>?) : CQResponseDTO("ok", 0, groupList)
    class CQGroupInfo(group_id: Long, group_name: String, member_count: Int, max_member_count: Int) :
        CQResponseDTO("ok", 0, CQGroupInfoData(group_id, group_name, member_count, max_member_count))

    class CQMemberInfo(member: CQMemberInfoData) : CQResponseDTO("ok", 0, member)
    class CQMemberList(memberList: List<CQMemberInfoData>) : CQResponseDTO("ok", 0, memberList)
    class CQCanSendImage(data: CQCanSendImageData = CQCanSendImageData()) : CQResponseDTO("ok", 0, data)
    class CQCanSendRecord(data: CQCanSendRecordData = CQCanSendRecordData()) : CQResponseDTO("ok", 0, data)
    class CQPluginStatus(status: CQPluginStatusData) : CQResponseDTO("ok", 0, status)
    class CQVersionInfo(versionInfo: CQVersionInfoData) : CQResponseDTO("ok", 0, versionInfo)

    class CQHonorList(honorList: GroupHonorListData) : CQResponseDTO("ok", 0, honorList)

    object ResponseDataSerializer : KSerializer<Any?> {
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): Any? {
            error("Not implemented")
        }

        override fun serialize(encoder: Encoder, value: Any?) {
            return when (value) {
                is List<*> -> encoder.encodeSerializableValue(
                    ListSerializer(CQResponseDataDTO.serializer()),
                    value as List<CQResponseDataDTO>
                )
                is GroupHonorListData -> encoder.encodeSerializableValue(
                    GroupHonorListData.serializer(),
                    value
                )
                else -> encoder.encodeSerializableValue(
                    CQResponseDataDTO.serializer(),
                    value as CQResponseDataDTO
                )
            }
        }
    }
}

@Serializable
@SerialName("MessageData")
data class CQMessageData(val message_id: Int) : CQResponseDataDTO()

@Serializable
@SerialName("LoginInfoData")
data class CQLoginInfoData(val user_id: Long, val nickname: String) : CQResponseDataDTO()

@Serializable
@SerialName("FriendData")
data class CQFriendData(val user_id: Long, val nickname: String, val remark: String) : CQResponseDataDTO()

@Serializable
@SerialName("GroupData")
data class CQGroupData(val group_id: Long, val group_name: String) : CQResponseDataDTO()

@Serializable
@SerialName("GroupInfoData")
data class CQGroupInfoData(
    val group_id: Long,
    val group_name: String,
    val member_count: Int,
    val max_member_count: Int
) : CQResponseDataDTO()

@Serializable
@SerialName("MemberInfoData")
data class CQMemberInfoData(
    val group_id: Long,
    val user_id: Long,
    val nickname: String,
    val card: String,
    var sex: String = "unknown",
    var age: Int = 0,
    var join_time: Int = 0,
    var last_sent_time: Int = 0,
    var level: String = "unknown",
    val role: String,
    val unfriendly: Boolean = false,
    val title: String,
    val title_expire_time: Int = 0,
    val card_changeable: Boolean
) : CQResponseDataDTO() {
    constructor(member: Member) : this(
        member.group.id,
        member.id,
        member.nick,
        member.nameCard,
        "unknown",
        0,
        0,
        0,
        "unknown",
        if (member.permission == MemberPermission.ADMINISTRATOR) "admin" else member.permission.name.toLowerCase(),
        false,
        member.specialTitle,
        0,
        member.group.botPermission == MemberPermission.ADMINISTRATOR || member.group.botPermission == MemberPermission.OWNER
    )
}

@Serializable
@SerialName("CanSendImageData")
data class CQCanSendImageData(val yes: Boolean = true) : CQResponseDataDTO()

@Serializable
@SerialName("CanSendRecordData")
data class CQCanSendRecordData(val yes: Boolean = false) : CQResponseDataDTO()

@Serializable
@SerialName("PluginStatusData")
data class CQPluginStatusData(
    val app_initialized: Boolean = true,
    val app_enabled: Boolean = true,
    val plugins_good: Boolean = true,
    val app_good: Boolean = true,
    val online: Boolean = true,
    val good: Boolean = true
) : CQResponseDataDTO()

@Serializable
@SerialName("VersionInfoData")
data class CQVersionInfoData(
    val coolq_directory: String = "",
    val coolq_edition: String = "pro",
    val plugin_version: String,
    val plugin_build_number: String,
    val plugin_build_configuration: String = "debug"
) : CQResponseDataDTO()