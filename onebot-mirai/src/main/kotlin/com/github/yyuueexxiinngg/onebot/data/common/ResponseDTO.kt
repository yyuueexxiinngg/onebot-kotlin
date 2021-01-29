package com.github.yyuueexxiinngg.onebot.data.common

import com.github.yyuueexxiinngg.onebot.BuildConfig
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.MemberPermission

@Serializable
sealed class CQResponseDataDTO

@Serializable
open class CQResponseDTO(
    val status: String,
    val retcode: Int,
    @Serializable(with = ResponseDataSerializer::class) val data: Any?,
    var echo: JsonElement? = null
) {
    class CQGeneralSuccess : CQResponseDTO("ok", 0, null)
    class CQAsyncStarted : CQResponseDTO("async", 1, null)
    class CQMiraiFailure(message: String? = null) : CQResponseDTO("failed", 102, ResponseMessageData(message))
    class CQPluginFailure(message: String? = null) : CQResponseDTO("failed", 103, ResponseMessageData(message))
    class CQInvalidRequest(message: String? = "参数错误") : CQResponseDTO("failed", 100, ResponseMessageData(message))
    class CQMessageResponse(message_id: Int) : CQResponseDTO("ok", 0, CQMessageData(message_id))
    class CQGetMessageResponse(result: CQGetMessageData) : CQResponseDTO("ok", 0, result)
    class CQLoginInfo(user_id: Long, nickname: String) : CQResponseDTO("ok", 0, CQLoginInfoData(user_id, nickname))
    class CQFriendList(friendList: List<CQFriendData>) : CQResponseDTO("ok", 0, friendList)
    class CQStrangerInfo(info: CQStrangerInfoData) : CQResponseDTO("ok", 0, info)

    class CQGroupList(groupList: List<CQGroupData>?) : CQResponseDTO("ok", 0, groupList)
    class CQGroupInfo(group_id: Long, group_name: String, member_count: Int, max_member_count: Int) :
        CQResponseDTO("ok", 0, CQGroupInfoData(group_id, group_name, member_count, max_member_count))

    class CQImageInfo(image: CQImageInfoData) : CQResponseDTO("ok", 0, image)
    class CQRecordInfo(record: CQRecordInfoData) : CQResponseDTO("ok", 0, record)
    class CQMemberInfo(member: CQMemberInfoData) : CQResponseDTO("ok", 0, member)
    class CQMemberList(memberList: List<CQMemberInfoData>) : CQResponseDTO("ok", 0, memberList)
    class CQCanSendImage(data: CQCanSendImageData = CQCanSendImageData()) : CQResponseDTO("ok", 0, data)
    class CQCanSendRecord(data: CQCanSendRecordData = CQCanSendRecordData()) : CQResponseDTO("ok", 0, data)
    class CQPluginStatus(status: CQPluginStatusData) : CQResponseDTO("ok", 0, status)
    class CQVersionInfo(versionInfo: CQVersionInfoData) : CQResponseDTO("ok", 0, versionInfo)

    class CQHonorInfo(honorInfo: CQGroupHonorInfoData) : CQResponseDTO("ok", 0, honorInfo)

    object ResponseDataSerializer : KSerializer<Any?> {
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor

        override fun deserialize(decoder: Decoder): Any? {
            error("Not implemented")
        }

        @Suppress("UNCHECKED_CAST")
        override fun serialize(encoder: Encoder, value: Any?) {
            return when (value) {
                is List<*> -> encoder.encodeSerializableValue(
                    ListSerializer(CQResponseDataDTO.serializer()),
                    value as List<CQResponseDataDTO>
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
@SerialName("ResponseMessageData")
data class ResponseMessageData(val message: String?) : CQResponseDataDTO()

@Serializable
@SerialName("MessageData")
data class CQMessageData(val message_id: Int) : CQResponseDataDTO()

@Serializable
@SerialName("GetMessageData")
data class CQGetMessageData(
    val time: Long,
    val message_type: String,
    val message_id: Int,
    val real_id: Int,
    val sender: CQContactDTO,
    val message: CQMessageChainOrStringDTO
) : CQResponseDataDTO()

@Serializable
@SerialName("LoginInfoData")
data class CQLoginInfoData(val user_id: Long, val nickname: String) : CQResponseDataDTO()

@Serializable
@SerialName("StrangerInfoData")
data class CQStrangerInfoData(
    val user_id: Long,
    val nickname: String,
    val sex: String = "unknown",
    val age: Int = 0
) : CQResponseDataDTO()

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
@SerialName("ImageInfoData")
data class CQImageInfoData(
    val file: String,
    @SerialName("filename") val fileName: String,
    val md5: String,
    val size: Int,
    val url: String,
    @SerialName("add_time") val addTime: Long,
    @SerialName("file_type") val fileType: String,
) : CQResponseDataDTO()

@Serializable
@SerialName("RecordInfoData")
data class CQRecordInfoData(
    val file: String,
    @SerialName("filename") val fileName: String,
    val md5: String,
    @SerialName("file_type") val fileType: String,
) : CQResponseDataDTO()

@Serializable
@SerialName("CanSendImageData")
data class CQCanSendImageData(val yes: Boolean = true) : CQResponseDataDTO()

@Serializable
@SerialName("CanSendRecordData")
data class CQCanSendRecordData(val yes: Boolean = true) : CQResponseDataDTO()

@Serializable
@SerialName("PluginStatusData")
data class CQPluginStatusData(
    val app_initialized: Boolean = true,
    val app_enabled: Boolean = true,
    val plugins_good: PluginsGoodData = PluginsGoodData(),
    val app_good: Boolean = true,
    val online: Boolean = true,
    val good: Boolean = true
) : CQResponseDataDTO()

@Serializable
@SerialName("PluginsGoodData")
data class PluginsGoodData(
    val asyncActions: Boolean = true,
    val backwardCompatibility: Boolean = true,
    val defaultConfigGenerator: Boolean = true,
    val eventDataPatcher: Boolean = true,
    val eventFilter: Boolean = true,
    val experimentalActions: Boolean = true,
    val extensionLoader: Boolean = true,
    val heartbeatGenerator: Boolean = true,
    val http: Boolean = true,
    val iniConfigLoader: Boolean = true,
    val jsonConfigLoader: Boolean = true,
    val loggers: Boolean = true,
    val messageEnhancer: Boolean = true,
    val postMessageFormatter: Boolean = true,
    val rateLimitedActions: Boolean = true,
    val restarter: Boolean = true,
    val updater: Boolean = true,
    val websocket: Boolean = true,
    val websocketReverse: Boolean = true,
    val workerPoolResizer: Boolean = true,
) : CQResponseDataDTO()

@Serializable
@SerialName("VersionInfoData")
data class CQVersionInfoData(
    val coolq_directory: String = "",
    val coolq_edition: String = "pro",
    val plugin_version: String = "4.15.0",
    val plugin_build_number: Int = 99,
    val plugin_build_configuration: String = "release",
    val app_name: String = "onebot-mirai",
    val app_version: String = BuildConfig.VERSION,
    val app_build_version: String = BuildConfig.COMMIT_HASH,
    val protocol_version: String = "v10",
) : CQResponseDataDTO()

@SerialName("HonorInfoData")
@Serializable
data class CQGroupHonorInfoData(
    @SerialName("accept_languages") val acceptLanguages: List<Language?>? = null,
    @SerialName("group_id") @SerializedName("gc")
    val groupId: String?,
    val type: Int?,
    @SerialName("user_id")
    val uin: String?,
    @SerialName("talkative_list")
    val talkativeList: List<Talkative?>? = null,
    @SerialName("current_talkative")
    val currentTalkative: CurrentTalkative? = null,
    @SerialName("actor_list")
    val actorList: List<Actor?>? = null,
    @SerialName("legend_list")
    var legendList: List<Actor?>? = null,
    @SerialName("performer_list")
    var performerList: List<Actor?>? = null,
    @SerialName("newbie_list")
    val newbieList: List<Actor?>? = null,
    @SerialName("strong_newbie_list") @SerializedName("strongnewbieList")
    var strongNewbieList: List<Actor?>? = null,
    @SerialName("emotion_list")
    var emotionList: List<Actor?>? = null,
    @SerialName("level_name") @SerializedName("levelname")
    val levelName: LevelName? = null,
    @SerialName("manage_list")
    var manageList: List<Tag?>? = null,
    @SerialName("exclusive_list")
    var exclusiveList: List<Tag?>? = null,
    @SerialName("active_obj")
    var activeObj: Map<String, List<Tag?>?>? = null, // Key为活跃等级名, 如`冒泡`
    @SerialName("show_active_obj")
    var showActiveObj: Map<String, Boolean?>? = null,
    @SerialName("my_title")
    val myTitle: String?,
    @SerialName("my_index")
    val myIndex: Int? = 0,
    @SerialName("my_avatar")
    val myAvatar: String?,
    @SerialName("has_server_error")
    val hasServerError: Boolean?,
    @SerialName("hw_excellent_list")
    val hwExcellentList: List<Actor?>? = null
) : CQResponseDataDTO() {
    @Serializable
    data class Language(
        @SerialName("code")
        val code: String? = null,
        @SerialName("script")
        val script: String? = null,
        @SerialName("region")
        val region: String? = null,
        @SerialName("quality")
        val quality: Double? = null
    )

    @Serializable
    data class Actor(
        @SerialName("user_id")
        val uin: Long? = 0,
        @SerialName("avatar")
        val avatar: String? = null,
        @SerialName("nickname")
        val name: String? = null,
        @SerialName("description")
        val desc: String? = null,
        @SerialName("btn_text")
        val btnText: String? = null,
        @SerialName("text")
        val text: String? = null,
        @SerialName("icon")
        val icon: Int?
    )

    @Serializable
    data class Talkative(
        @SerialName("user_id")
        val uin: Long? = 0,
        @SerialName("avatar")
        val avatar: String? = null,
        @SerialName("nickname")
        val name: String? = null,
        @SerialName("description")
        val desc: String? = null,
        @SerialName("btn_text")
        val btnText: String? = null,
        @SerialName("text")
        val text: String? = null
    )

    @Serializable
    data class CurrentTalkative(
        @SerialName("user_id")
        val uin: Long? = 0,
        @SerialName("day_count") @SerializedName("day_count")
        val dayCount: Int? = null,
        @SerialName("avatar")
        val avatar: String? = null,
        @SerialName("avatar_size") @SerializedName("avatar_size")
        val avatarSize: Int? = null,
        @SerialName("nickname")
        val name: String? = null
    )

    @Serializable
    data class LevelName(
        @SerialName("lvln1") @SerializedName("lvln1")
        val lv1: String? = null,
        @SerialName("lvln2") @SerializedName("lvln2")
        val lv2: String? = null,
        @SerialName("lvln3") @SerializedName("lvln3")
        val lv3: String? = null,
        @SerialName("lvln4") @SerializedName("lvln4")
        val lv4: String? = null,
        @SerialName("lvln5") @SerializedName("lvln5")
        val lv5: String? = null,
        @SerialName("lvln6") @SerializedName("lvln6")
        val lv6: String? = null
    )

    @Serializable
    data class Tag(
        @SerialName("user_id")
        val uin: Long? = 0,
        @SerialName("avatar")
        val avatar: String? = null,
        @SerialName("nickname")
        val name: String? = null,
        @SerialName("btn_text")
        val btnText: String? = null,
        @SerialName("text")
        val text: String? = null,
        @SerialName("tag")
        val tag: String? = null,  // 头衔
        @SerialName("tag_color")
        val tagColor: String? = null
    )
}