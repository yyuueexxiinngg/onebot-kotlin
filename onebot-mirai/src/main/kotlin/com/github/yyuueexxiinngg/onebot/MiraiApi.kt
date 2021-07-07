package com.github.yyuueexxiinngg.onebot

import com.github.yyuueexxiinngg.onebot.PluginBase.saveImage
import com.github.yyuueexxiinngg.onebot.data.common.*
import com.github.yyuueexxiinngg.onebot.util.*
import com.github.yyuueexxiinngg.onebot.web.queue.CacheRequestQueue
import com.github.yyuueexxiinngg.onebot.web.queue.CacheSourceQueue
import com.google.gson.Gson
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.LowLevelApi
import net.mamoe.mirai.Mirai
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.announcement.OfflineAnnouncement
import net.mamoe.mirai.contact.getMemberOrFail
import net.mamoe.mirai.data.GroupHonorListData
import net.mamoe.mirai.data.GroupHonorType
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.MessageChain.Companion.deserializeJsonToMessageChain
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.MessageSourceKind
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.kind
import net.mamoe.mirai.message.data.source
import net.mamoe.mirai.utils.MiraiExperimentalApi


@OptIn(LowLevelApi::class, MiraiExperimentalApi::class)
suspend fun callMiraiApi(action: String?, params: ApiParams, mirai: MiraiApi): ResponseDTO {
    var responseDTO: ResponseDTO = ResponseDTO.PluginFailure()
    try {
        when (action) {
            "send_msg" -> responseDTO = mirai.sendMessage(params)
            "send_private_msg" -> responseDTO = mirai.sendPrivateMessage(params)
            "send_group_msg" -> responseDTO = mirai.sendGroupMessage(params)
            "send_discuss_msg" -> responseDTO = mirai.sendDiscussMessage(params)
            "delete_msg" -> responseDTO = mirai.deleteMessage(params)
            "send_like" -> responseDTO = mirai.sendLike(params)
            "set_group_kick" -> responseDTO = mirai.setGroupKick(params)
            "set_group_ban" -> responseDTO = mirai.setGroupBan(params)
            "set_group_anonymous_ban" -> responseDTO = mirai.setGroupAnonymousBan(params)
            "set_group_whole_ban" -> responseDTO = mirai.setWholeGroupBan(params)
            "set_group_admin" -> responseDTO = mirai.setGroupAdmin(params)
            "set_group_anonymous" -> responseDTO = mirai.setGroupAnonymous(params)
            "set_group_card" -> responseDTO = mirai.setGroupCard(params)
            "set_group_leave" -> responseDTO = mirai.setGroupLeave(params)
            "set_group_special_title" -> responseDTO = mirai.setGroupSpecialTitle(params)
            "set_discuss_leave" -> responseDTO = mirai.setDiscussLeave(params)
            "set_friend_add_request" -> responseDTO = mirai.setFriendAddRequest(params)
            "set_group_add_request" -> responseDTO = mirai.setGroupAddRequest(params)
            "get_login_info" -> responseDTO = mirai.getLoginInfo(params)
            "get_stranger_info" -> responseDTO = mirai.getStrangerInfo(params)
            "get_friend_list" -> responseDTO = mirai.getFriendList(params)
            "get_group_list" -> responseDTO = mirai.getGroupList(params)
            "get_group_info" -> responseDTO = mirai.getGroupInfo(params)
            "get_group_member_info" -> responseDTO = mirai.getGroupMemberInfo(params)
            "get_group_member_list" -> responseDTO = mirai.getGroupMemberList(params)
            "get_cookies" -> responseDTO = mirai.getCookies(params)
            "get_csrf_token" -> responseDTO = mirai.getCSRFToken(params)
            "get_credentials" -> responseDTO = mirai.getCredentials(params)
            "get_record" -> responseDTO = mirai.getRecord(params)
            "get_image" -> responseDTO = mirai.getImage(params)
            "can_send_image" -> responseDTO = mirai.canSendImage(params)
            "can_send_record" -> responseDTO = mirai.canSendRecord(params)
            "get_status" -> responseDTO = mirai.getStatus(params)
            "get_version_info" -> responseDTO = mirai.getVersionInfo(params)
            "set_restart_plugin" -> responseDTO = mirai.setRestartPlugin(params)
            "clean_data_dir" -> responseDTO = mirai.cleanDataDir(params)
            "clean_plugin_log" -> responseDTO = mirai.cleanPluginLog(params)
            ".handle_quick_operation" -> responseDTO = mirai.handleQuickOperation(params)

            "set_group_name" -> responseDTO = mirai.setGroupName(params)
            "get_group_honor_info" -> responseDTO = mirai.getGroupHonorInfo(params)
            "get_msg" -> responseDTO = mirai.getMessage(params)

            "_set_group_announcement" -> responseDTO = mirai.setGroupAnnouncement(params)
            else -> {
                logger.error("未知OneBot API: $action")
            }
        }
    } catch (e: IllegalArgumentException) {
        responseDTO = ResponseDTO.InvalidRequest()
    } catch (e: PermissionDeniedException) {
        logger.debug("机器人无操作权限, 调用的API: /$action")
        responseDTO = ResponseDTO.MiraiFailure()
    } catch (e: Exception) {
        logger.error(e)
        responseDTO = ResponseDTO.PluginFailure()
    }

    return responseDTO
}

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
class MiraiApi(val bot: Bot) {
    // Store temp contact information for sending messages
    // QQ : GroupId
    val cachedTempContact: MutableMap<Long, Long> = mutableMapOf()
    val cacheRequestQueue = CacheRequestQueue()
    val cachedSourceQueue = CacheSourceQueue()

    suspend fun sendMessage(params: ApiParams): ResponseDTO {
        if (params.contains("message_type")) {
            when (params["message_type"].string) {
                "private" -> return sendPrivateMessage(params)
                "group" -> return sendGroupMessage(params)
            }
        } else {
            when {
                params["group_id"] != null -> return sendGroupMessage(params)
                params["discuss_id"] != null -> return sendGroupMessage(params)
                params["user_id"] != null -> return sendPrivateMessage(params)
            }
        }
        return ResponseDTO.InvalidRequest()
    }

    @Suppress("DuplicatedCode")
    suspend fun sendGroupMessage(params: ApiParams): ResponseDTO {
        val targetGroupId = params["group_id"].long
        val raw = params["auto_escape"].booleanOrNull ?: false
        val messages = params["message"]

        val group = bot.getGroupOrFail(targetGroupId)
        messageToMiraiMessageChains(bot, group, messages, raw)?.let {
            return if (it.content.isNotEmpty()) {
                val receipt = group.sendMessage(it)
                cachedSourceQueue.add(receipt.source)
                ResponseDTO.MessageResponse(receipt.source.internalIds.toMessageId(bot.id, receipt.source.fromId))
            } else {
                ResponseDTO.MessageResponse(-1)
            }
        }
        return ResponseDTO.InvalidRequest()
    }

    @Suppress("DuplicatedCode")
    suspend fun sendPrivateMessage(params: ApiParams): ResponseDTO {
        val targetQQId = params["user_id"].long
        val raw = params["auto_escape"].booleanOrNull ?: false
        val messages = params["message"]

        val contact = try {
            bot.getFriendOrFail(targetQQId)
        } catch (e: NoSuchElementException) {
            val fromGroupId = cachedTempContact[targetQQId]
                ?: bot.groups.find { group -> group.members.contains(targetQQId) }?.id
            bot.getGroupOrFail(fromGroupId!!).getMemberOrFail(targetQQId)
        }

        messageToMiraiMessageChains(bot, contact, messages, raw)?.let {
            return if (it.content.isNotEmpty()) {
                val receipt = contact.sendMessage(it)
                cachedSourceQueue.add(receipt.source)
                ResponseDTO.MessageResponse(receipt.source.internalIds.toMessageId(bot.id, receipt.source.fromId))
            } else {
                ResponseDTO.MessageResponse(-1)
            }
        }
        return ResponseDTO.InvalidRequest()
    }

    suspend fun deleteMessage(params: ApiParams): ResponseDTO {
        val messageId = params["message_id"].int
        cachedSourceQueue[messageId].recall()
        return ResponseDTO.GeneralSuccess()
    }

    suspend fun getMessage(params: ApiParams): ResponseDTO {
        val messageId = params["message_id"].int
        if (PluginSettings.db.enable) {
            PluginBase.db?.apply {
                val message = String(get(messageId.toByteArray())).deserializeJsonToMessageChain()
                val rawMessage = WrappedMessageChainString("")
                message.forEach { rawMessage.value += it.toCQString() }

                with(message) {
                    when (source.kind) {
                        MessageSourceKind.GROUP -> {
                            return ResponseDTO.GetMessageResponse(
                                GetMessageData(
                                    source.time.toLong(),
                                    "group",
                                    messageId,
                                    messageId,
                                    MemberDTO(source.fromId, "unknown"),
                                    message.toMessageChainDTO { it != UnknownMessageDTO }
                                )
                            )
                            /*val dto = CQGroupMessagePacketDTO(
                                bot.id,
                                if (source.fromId == 80000000L) "anonymous" else "normal",
                                messageId,
                                source.targetId,
                                source.fromId,
                                null,
                                message.toMessageChainDTO { it != UnknownMessageDTO },
                                rawMessage.value,
                                0,
                                CQMemberDTO(
                                    source.fromId,
                                    "unknown",
                                    "unknown",
                                    "unknown",
                                    0,
                                    "unknown",
                                    "unknown",
                                    "unknown",
                                    "unknown",
                                ),
                                source.time.toLong()
                            )*/
                        }
                        MessageSourceKind.FRIEND, MessageSourceKind.STRANGER, MessageSourceKind.TEMP -> {
                            return ResponseDTO.GetMessageResponse(
                                GetMessageData(
                                    source.time.toLong(),
                                    "private",
                                    messageId,
                                    messageId,
                                    QQDTO(source.fromId, "unknown"),
                                    message.toMessageChainDTO { it != UnknownMessageDTO }
                                )
                            )
                        }
                        else -> return ResponseDTO.PluginFailure("未知消息类型")
                    }
                }
            }
            return ResponseDTO.PluginFailure("数据库未正常初始化")
        } else {
            return ResponseDTO.InvalidRequest("请配置开启数据库")
        }
    }

    suspend fun setGroupKick(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val memberId = params["user_id"].long

        bot.getGroupOrFail(groupId).getMemberOrFail(memberId).kick("")
        return ResponseDTO.GeneralSuccess()
    }

    suspend fun setGroupBan(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val memberId = params["user_id"].long
        val duration = params["duration"].intOrNull ?: 30 * 60

        bot.getGroupOrFail(groupId).getMemberOrFail(memberId).mute(duration)
        return ResponseDTO.GeneralSuccess()
    }

    @OptIn(LowLevelApi::class, MiraiExperimentalApi::class)
    suspend fun setGroupAnonymousBan(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val flag = params["anonymous"]?.jsonObject?.get("flag").stringOrNull
            ?: params["anonymous_flag"].stringOrNull
            ?: params["flag"].string

        val duration = params["duration"].intOrNull ?: 30 * 60
        val splits = flag.split("&", limit = 2)
        Mirai.muteAnonymousMember(bot, splits[0], splits[1], groupId, duration)
        return ResponseDTO.GeneralSuccess()
    }

    fun setWholeGroupBan(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val enable = params["enable"].booleanOrNull ?: true

        bot.getGroupOrFail(groupId).settings.isMuteAll = enable
        return ResponseDTO.GeneralSuccess()
    }

    fun setGroupCard(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val memberId = params["user_id"].long
        val card = params["card"].stringOrNull ?: ""

        bot.getGroupOrFail(groupId).getMemberOrFail(memberId).nameCard = card
        return ResponseDTO.GeneralSuccess()
    }

    suspend fun setGroupLeave(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val dismiss = params["is_dismiss"].booleanOrNull ?: false

        // Not supported
        if (dismiss) return ResponseDTO.MiraiFailure()

        bot.getGroupOrFail(groupId).quit()
        return ResponseDTO.GeneralSuccess()
    }

    fun setGroupSpecialTitle(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val memberId = params["user_id"].long
        val specialTitle = params["special_title"].stringOrNull ?: ""
        val duration = params["duration"].intOrNull ?: -1 // unused

        bot.getGroupOrFail(groupId).getMemberOrFail(memberId).specialTitle = specialTitle
        return ResponseDTO.GeneralSuccess()
    }

    suspend fun setFriendAddRequest(params: ApiParams): ResponseDTO {
        val flag = params["flag"].string
        val approve = params["approve"].booleanOrNull ?: true
        val remark = params["remark"].stringOrNull // unused

        val event = cacheRequestQueue[flag.toLong()]
        if (event is NewFriendRequestEvent)
            if (approve) event.accept() else event.reject()
        else return ResponseDTO.InvalidRequest()

        return ResponseDTO.GeneralSuccess()
    }

    suspend fun setGroupAddRequest(params: ApiParams): ResponseDTO {
        val flag = params["flag"].string
        val type = params["type"].stringOrNull // unused
        val subType = params["sub_type"].stringOrNull // unused
        val approve = params["approve"].booleanOrNull ?: true
        val reason = params["reason"].stringOrNull

        when (val event = cacheRequestQueue[flag.toLong()]) {
            is MemberJoinRequestEvent -> if (approve) event.accept() else event.reject(message = reason ?: "")
            is BotInvitedJoinGroupRequestEvent -> if (approve) event.accept() else event.ignore()
        }
        return ResponseDTO.GeneralSuccess()
    }

    fun getLoginInfo(params: ApiParams): ResponseDTO {
        return ResponseDTO.LoginInfo(bot.id, bot.nick)
    }

    suspend fun getStrangerInfo(params: ApiParams): ResponseDTO {
        val userId = params["user_id"].long

        val profile = Mirai.queryProfile(bot, userId)
        return ResponseDTO.StrangerInfo(
            StrangerInfoData(
                userId,
                profile.nickname,
                profile.sex.name.toLowerCase(),
                profile.age
            )
        )
    }

    fun getFriendList(params: ApiParams): ResponseDTO {
        val friendList = mutableListOf<FriendData>()
        bot.friends.forEach { friend ->
            friendList.add(FriendData(friend.id, friend.nick, friend.remark))
        }
        return ResponseDTO.FriendList(friendList)
    }

    fun getGroupList(params: ApiParams): ResponseDTO {
        val groupList = mutableListOf<GroupData>()
        bot.groups.forEach { group ->
            groupList.add(GroupData(group.id, group.name))
        }
        return ResponseDTO.GroupList(groupList)
    }

    /**
     * 获取群信息
     * 不支持获取群容量, 返回0
     */
    fun getGroupInfo(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val noCache = params["no_cache"].booleanOrNull ?: false // unused

        val group = bot.getGroupOrFail(groupId)
        return ResponseDTO.GroupInfo(group.id, group.name, group.members.size + 1, 0)
    }

    @MiraiExperimentalApi
    @LowLevelApi
    suspend fun getGroupMemberInfo(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val memberId = params["user_id"].long
        val noCache = params["no_cache"].booleanOrNull ?: false

        val group = bot.getGroupOrFail(groupId)
        return if (noCache) {
            val groupUin = Mirai.calculateGroupUinByGroupCode(groupId)
            val members = Mirai.getRawGroupMemberList(bot, groupUin, groupId, group.owner.id)
            val member = members.find { m -> m.uin == memberId }
            member?.let {
                ResponseDTO.MemberInfo(
                    MemberInfoData(
                        group.id,
                        it.uin,
                        nickname = it.nameCard,
                        card = it.nameCard,
                        role = if (it.permission == MemberPermission.ADMINISTRATOR) "admin" else it.permission.name.toLowerCase(),
                        title = it.specialTitle,
                        card_changeable = group.botPermission == MemberPermission.OWNER
                    )
                )
            } ?: ResponseDTO.MiraiFailure()
        } else {
            val member = bot.getGroupOrFail(groupId).getMemberOrFail(memberId)
            ResponseDTO.MemberInfo(MemberInfoData(member))
        }
    }

    fun getGroupMemberList(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val groupMemberListData = mutableListOf<MemberInfoData>()

        var isBotIncluded = false
        val group = bot.getGroupOrFail(groupId)
        val members = group.members
        members.forEach { member ->
            run {
                if (member.id == bot.id) isBotIncluded = true
                groupMemberListData.add(MemberInfoData(member))
            }
        }
        if (!isBotIncluded) groupMemberListData.add(MemberInfoData(group.botAsMember))
        return ResponseDTO.MemberList(groupMemberListData)
    }

    // https://github.com/richardchien/coolq-http-api/blob/master/src/cqhttp/plugins/web/http.cpp#L375
    suspend fun handleQuickOperation(params: ApiParams): ResponseDTO {
        try {
            val context = params["context"]?.jsonObject
            val operation = params["operation"]?.jsonObject
            val postType = context?.get("post_type")?.jsonPrimitive?.content

            if (postType == "message") {
                val messageType = context["message_type"]?.jsonPrimitive?.content

                val replyElement = operation?.get("reply")
                if (replyElement != null) {
                    val nextCallParams = context.toMutableMap()
                    if (messageType == "group" && operation["at_sender"]?.jsonPrimitive?.booleanOrNull == true) {
                        context["user_id"]?.jsonPrimitive?.apply {
                            when (replyElement) {
                                is JsonArray -> {
                                    val replyMessageChain = replyElement.jsonArray.toMutableList()
                                    replyMessageChain.add(
                                        0, JsonObject(
                                            mapOf(
                                                "type" to JsonPrimitive("at"),
                                                "data" to JsonObject(mapOf("qq" to JsonPrimitive(this.long)))
                                            )
                                        )
                                    )
                                    nextCallParams["message"] = JsonArray(replyMessageChain)
                                }
                                is JsonObject -> {
                                    val replyMessageChain = mutableListOf(replyElement.jsonObject)
                                    replyMessageChain.add(
                                        0, JsonObject(
                                            mapOf(
                                                "type" to JsonPrimitive("at"),
                                                "data" to JsonObject(mapOf("qq" to JsonPrimitive(this.long)))
                                            )
                                        )
                                    )
                                    nextCallParams["message"] = JsonArray(replyMessageChain)
                                }
                                else -> {
                                    val textToReply = "[CQ:at,qq=$this] ${replyElement.jsonPrimitive.content}"
                                    nextCallParams["message"] = JsonPrimitive(textToReply)
                                }
                            }
                        }
                    } else {
                        nextCallParams["message"] = replyElement
                    }
                    return sendMessage(nextCallParams)
                }

                if (messageType == "group") {
                    // TODO: 备忘, 暂未支持
                    val isAnonymous = false
                    if (operation?.get("delete")?.jsonPrimitive?.booleanOrNull == true) {
                        return deleteMessage(context)
                    }
                    if (operation?.get("kick")?.jsonPrimitive?.booleanOrNull == true) {
                        return setGroupKick(context)
                    }
                    if (operation?.get("ban")?.jsonPrimitive?.booleanOrNull == true) {
                        @Suppress("ConstantConditionIf")
                        (return if (isAnonymous) {
                            setGroupAnonymousBan(context)
                        } else {
                            setGroupBan(context)
                        })
                    }
                }
            } else if (postType == "request") {
                val requestType = context["request_type"]?.jsonPrimitive?.content
                val approveOpt = operation?.get("approve")?.jsonPrimitive?.booleanOrNull ?: false
                val nextCallParams = context.toMutableMap()
                nextCallParams["approve"] = JsonPrimitive(approveOpt)
                nextCallParams["remark"] = JsonPrimitive(operation?.get("remark")?.jsonPrimitive?.contentOrNull)
                nextCallParams["reason"] = JsonPrimitive(operation?.get("reason")?.jsonPrimitive?.contentOrNull)
                if (requestType == "friend") {
                    return setFriendAddRequest(nextCallParams)
                } else if (requestType == "group") {
                    return setGroupAddRequest(nextCallParams)
                }
            }
            return ResponseDTO.InvalidRequest()
        } catch (e: Exception) {
            return ResponseDTO.PluginFailure()
        }
    }

    suspend fun getRecord(params: ApiParams): ResponseDTO {
        val file = params["file"].string
        val outFormat = params["out_format"].stringOrNull // Currently not supported

        val cachedFile = getCachedRecordFile(file)
        cachedFile?.let {
            val fileType = with(it.readBytes().copyOfRange(0, 10).toUHexString("")) {
                when {
                    startsWith("2321414D52") -> "amr"
                    startsWith("02232153494C4B5F5633") -> "silk"
                    else -> "unknown"
                }
            }
            return ResponseDTO.RecordInfo(
                RecordInfoData(
                    it.absolutePath,
                    it.nameWithoutExtension,
                    it.nameWithoutExtension,
                    fileType
                )
            )
        } ?: return ResponseDTO.PluginFailure()
    }

    suspend fun getImage(params: ApiParams): ResponseDTO {
        val file = params["file"].string

        val image = getCachedImageFile(file)
        image?.let { cachedImageMeta ->
            var cachedFile = getDataFile("image", cachedImageMeta.fileName)
            if (cachedFile == null) {
                HttpClient.getBytes(cachedImageMeta.url)?.let { saveImage(cachedImageMeta.fileName, it) }
            }
            cachedFile = getDataFile("image", cachedImageMeta.fileName)

            cachedFile?.let {
                val fileType = getImageType(it.readBytes())
                return ResponseDTO.ImageInfo(
                    ImageInfoData(
                        it.absolutePath,
                        cachedImageMeta.fileName,
                        cachedImageMeta.md5,
                        cachedImageMeta.size,
                        cachedImageMeta.url,
                        cachedImageMeta.addTime,
                        fileType
                    )
                )
            }
        } ?: return ResponseDTO.PluginFailure()
    }

    fun canSendImage(params: ApiParams): ResponseDTO {
        return ResponseDTO.CanSendImage()
    }

    fun canSendRecord(params: ApiParams): ResponseDTO {
        return ResponseDTO.CanSendRecord()
    }

    fun getStatus(params: ApiParams): ResponseDTO {
        return ResponseDTO.PluginStatus(PluginStatusData(online = bot.isActive, good = bot.isActive))
    }

    fun getVersionInfo(params: ApiParams): ResponseDTO {
        return ResponseDTO.VersionInfo(
            VersionInfoData(
                coolq_directory = PluginBase.dataFolder.absolutePath
            )
        )
    }

    fun setRestartPlugin(params: ApiParams): ResponseDTO {
        val delay = params["delay"]?.jsonPrimitive?.int ?: 0 // unused
        return ResponseDTO.GeneralSuccess()
    }

    fun cleanDataDir(params: ApiParams): ResponseDTO {
        return ResponseDTO.GeneralSuccess()
    }

    fun cleanPluginLog(params: ApiParams): ResponseDTO {
        return ResponseDTO.GeneralSuccess()
    }

    ////////////////
    ////  v11  ////
    //////////////

    fun setGroupName(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val name = params["group_name"].string

        return if (name != "") {
            bot.getGroupOrFail(groupId).name = name
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    @MiraiExperimentalApi
    @LowLevelApi
    @Suppress("DuplicatedCode")
    suspend fun getGroupHonorInfo(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val type = params["type"].string

        var finalData: GroupHonorInfoData? = null

        if (type == "talkative" || type == "all") {
            val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.TALKATIVE)
            val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
            val cqData = jsonData?.let { Gson().fromJson(it, GroupHonorInfoData::class.java) }
            finalData = cqData?.let { cqData }
        }

        if (type == "performer" || type == "all") {
            val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.PERFORMER)
            val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
            val cqData = jsonData?.let { Gson().fromJson(it, GroupHonorInfoData::class.java) }
            finalData = finalData?.apply { performerList = cqData?.actorList } ?: cqData?.let { cqData }
        }

        if (type == "legend" || type == "all") {
            val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.LEGEND)
            val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
            val cqData = jsonData?.let { Gson().fromJson(it, GroupHonorInfoData::class.java) }
            finalData = finalData?.apply { legendList = cqData?.legendList } ?: cqData?.let { cqData }
        }

        if (type == "strong_newbie" || type == "all") {
            val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.STRONG_NEWBIE)
            val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
            val cqData = jsonData?.let { Gson().fromJson(it, GroupHonorInfoData::class.java) }
            finalData = finalData?.apply { strongNewbieList = cqData?.strongNewbieList } ?: cqData?.let { cqData }
        }

        if (type == "emotion" || type == "all") {
            val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.EMOTION)
            val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
            val cqData = jsonData?.let { Gson().fromJson(it, GroupHonorInfoData::class.java) }
            finalData = finalData?.apply { emotionList = cqData?.emotionList } ?: cqData?.let { cqData }
        }

        if (type == "active") {
            val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.ACTIVE)
            val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
            val cqData = jsonData?.let { Gson().fromJson(it, GroupHonorInfoData::class.java) }
            finalData = finalData?.apply {
                activeObj = cqData?.activeObj
                showActiveObj = cqData?.showActiveObj
            } ?: cqData?.let { cqData }
        }

        if (type == "exclusive") {
            val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.EXCLUSIVE)
            val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
            val cqData = jsonData?.let { Gson().fromJson(it, GroupHonorInfoData::class.java) }
            finalData = finalData?.apply { exclusiveList = cqData?.exclusiveList } ?: cqData?.let { cqData }
        }

        if (type == "manage") {
            val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.MANAGE)
            val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
            val cqData = jsonData?.let { Gson().fromJson(it, GroupHonorInfoData::class.java) }
            finalData = finalData?.apply { manageList = cqData?.manageList } ?: cqData?.let { cqData }
        }

        finalData?.let { return ResponseDTO.HonorInfo(it) } ?: return ResponseDTO.MiraiFailure()
    }

    /////////////////
    //// hidden ////
    ///////////////

    @MiraiExperimentalApi
    @LowLevelApi
    suspend fun setGroupAnnouncement(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val content = params["content"].string

        return if (groupId != null && content != null && content != "") {
            bot.getGroupOrFail(groupId).announcements.publish(OfflineAnnouncement(content))
            CQResponseDTO.CQGeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    ////////////////////////////////
    //// currently unsupported ////
    //////////////////////////////

    fun getCookies(params: ApiParams): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun getCSRFToken(params: ApiParams): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun getCredentials(params: ApiParams): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun sendDiscussMessage(params: ApiParams): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun setGroupAnonymous(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val enable = params["enable"]?.jsonPrimitive?.long ?: true

        // Not supported
        // bot.getGroupOrFail(groupId).settings.isAnonymousChatEnabled = enable
        return ResponseDTO.MiraiFailure()
    }

    fun setGroupAdmin(params: ApiParams): ResponseDTO {
        val groupId = params["group_id"].long
        val memberId = params["user_id"].long
        val enable = params["enable"]?.jsonPrimitive?.long ?: true

        // Not supported
        // bot.getGroupOrFail(groupId).getMemberOrFail(memberId).permission = if (enable) MemberPermission.ADMINISTRATOR else MemberPermission.MEMBER
        return ResponseDTO.MiraiFailure()
    }

    fun setDiscussLeave(params: ApiParams): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun sendLike(params: ApiParams): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }


}