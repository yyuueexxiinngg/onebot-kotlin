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
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.getMemberOrFail
import net.mamoe.mirai.data.GroupAnnouncement
import net.mamoe.mirai.data.GroupAnnouncementMsg
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
suspend fun callMiraiApi(action: String?, params: Map<String, JsonElement>, mirai: MiraiApi): ResponseDTO {
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

    suspend fun sendMessage(params: Map<String, JsonElement>): ResponseDTO {
        if (params.contains("message_type")) {
            when (params["message_type"]?.jsonPrimitive?.content) {
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
    suspend fun sendGroupMessage(params: Map<String, JsonElement>): ResponseDTO {
        val targetGroupId = params["group_id"]!!.jsonPrimitive.long
        val raw = params["auto_escape"]?.jsonPrimitive?.booleanOrNull ?: false
        val messages = params["message"]

        val group = bot.getGroupOrFail(targetGroupId)
        messageToMiraiMessageChains(bot, group, messages, raw)?.let {
            return if (it.content.isNotEmpty()) {
                val receipt = group.sendMessage(it)
                cachedSourceQueue.add(receipt.source)
                ResponseDTO.MessageResponse(receipt.source.internalIds.toMessageId(bot.id, group.id))
            } else {
                ResponseDTO.MessageResponse(-1)
            }
        }
        return ResponseDTO.InvalidRequest()
    }

    @Suppress("DuplicatedCode")
    suspend fun sendPrivateMessage(params: Map<String, JsonElement>): ResponseDTO {
        val targetQQId = params["user_id"]!!.jsonPrimitive.long
        val raw = params["auto_escape"]?.jsonPrimitive?.booleanOrNull ?: false
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
                ResponseDTO.MessageResponse(receipt.source.internalIds.toMessageId(bot.id, contact.id))
            } else {
                ResponseDTO.MessageResponse(-1)
            }
        }
        return ResponseDTO.InvalidRequest()
    }

    suspend fun deleteMessage(params: Map<String, JsonElement>): ResponseDTO {
        val messageId = params["message_id"]?.jsonPrimitive?.intOrNull
        messageId?.let {
            cachedSourceQueue[it].recall()
            return ResponseDTO.GeneralSuccess()
        }
        return ResponseDTO.InvalidRequest()
    }

    suspend fun getMessage(params: Map<String, JsonElement>): ResponseDTO {
        val messageId = params["message_id"]?.jsonPrimitive?.intOrNull
        if (PluginSettings.db.enable) {
            if (messageId != null) {
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
                        }
                    }
                }
            }
            return ResponseDTO.InvalidRequest()
        } else {
            return ResponseDTO.InvalidRequest("请配置开启数据库")
        }
    }

    suspend fun setGroupKick(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        return if (groupId != null && memberId != null) {
            bot.getGroupOrFail(groupId).getMemberOrFail(memberId).kick("")
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    suspend fun setGroupBan(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val duration = params["duration"]?.jsonPrimitive?.int ?: 30 * 60
        return if (groupId != null && memberId != null) {
            bot.getGroupOrFail(groupId).getMemberOrFail(memberId).mute(duration)
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    @OptIn(LowLevelApi::class)
    suspend fun setGroupAnonymousBan(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val flag = params["anonymous"]?.jsonObject?.get("flag")?.jsonPrimitive?.content
            ?: params["anonymous_flag"]?.jsonPrimitive?.content
            ?: params["flag"]?.jsonPrimitive?.content

        val duration = params["duration"]?.jsonPrimitive?.int ?: 30 * 60
        return if (groupId != null && flag != null) {
            val splits = flag.split("&", limit = 2)
            Mirai.muteAnonymousMember(bot, splits[0], splits[1], groupId, duration)
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun setWholeGroupBan(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val enable = params["enable"]?.jsonPrimitive?.booleanOrNull ?: true
        return if (groupId != null) {
            bot.getGroupOrFail(groupId).settings.isMuteAll = enable
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun setGroupCard(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val card = params["card"]?.jsonPrimitive?.contentOrNull ?: ""
        val enable = params["enable"]?.jsonPrimitive?.booleanOrNull ?: true
        return if (groupId != null && memberId != null) {
            bot.getGroupOrFail(groupId).getMemberOrFail(memberId).nameCard = card
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    suspend fun setGroupLeave(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val dismiss = params["enable"]?.jsonPrimitive?.booleanOrNull ?: false
        return if (groupId != null) {
            // Not supported
            if (dismiss) return ResponseDTO.MiraiFailure()

            bot.getGroupOrFail(groupId).quit()
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun setGroupSpecialTitle(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val specialTitle = params["special_title"]?.jsonPrimitive?.contentOrNull ?: ""
        val duration = params["duration"]?.jsonPrimitive?.int ?: -1  // Not supported
        return if (groupId != null && memberId != null) {
            bot.getGroupOrFail(groupId).getMemberOrFail(memberId).specialTitle = specialTitle
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    suspend fun setFriendAddRequest(params: Map<String, JsonElement>): ResponseDTO {
        val flag = params["flag"]?.jsonPrimitive?.contentOrNull
        val approve = params["approve"]?.jsonPrimitive?.booleanOrNull ?: true
        val remark = params["remark"]?.jsonPrimitive?.contentOrNull
        return if (flag != null) {
            val event = cacheRequestQueue[flag.toLongOrNull()]
            if (event is NewFriendRequestEvent)
                if (approve) event.accept() else event.reject()
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    suspend fun setGroupAddRequest(params: Map<String, JsonElement>): ResponseDTO {
        val flag = params["flag"]?.jsonPrimitive?.contentOrNull
        val type = params["type"]?.jsonPrimitive?.contentOrNull
        val subType = params["sub_type"]?.jsonPrimitive?.contentOrNull
        val approve = params["approve"]?.jsonPrimitive?.booleanOrNull ?: true
        val reason = params["reason"]?.jsonPrimitive?.contentOrNull

        return if (flag != null) {
            when (val event = cacheRequestQueue[flag.toLongOrNull()]) {
                is MemberJoinRequestEvent -> if (approve) event.accept() else event.reject(message = reason ?: "")
                is BotInvitedJoinGroupRequestEvent -> if (approve) event.accept() else event.ignore()
            }
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun getLoginInfo(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.LoginInfo(bot.id, bot.nick)
    }

    @OptIn(ConsoleExperimentalApi::class, MiraiExperimentalApi::class)
    suspend fun getStrangerInfo(params: Map<String, JsonElement>): ResponseDTO {
        val userId = params["user_id"]?.jsonPrimitive?.long
        return if (userId != null) {
            val profile = Mirai.queryProfile(bot, userId)
            ResponseDTO.StrangerInfo(
                StrangerInfoData(
                    userId,
                    profile.nickname,
                    profile.sex.name.toLowerCase(),
                    profile.age
                )
            )
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun getFriendList(params: Map<String, JsonElement>): ResponseDTO {
        val friendList = mutableListOf<FriendData>()
        bot.friends.forEach { friend ->
            friendList.add(FriendData(friend.id, friend.nick, friend.remark))
        }
        return ResponseDTO.FriendList(friendList)
    }

    fun getGroupList(params: Map<String, JsonElement>): ResponseDTO {
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
    fun getGroupInfo(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val noCache = params["no_cache"]?.jsonPrimitive?.booleanOrNull ?: false

        return if (groupId != null) {
            val group = bot.getGroupOrFail(groupId)
            ResponseDTO.GroupInfo(group.id, group.name, group.members.size + 1, 0)
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    @MiraiExperimentalApi
    @LowLevelApi
    suspend fun getGroupMemberInfo(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val noCache = params["no_cache"]?.jsonPrimitive?.booleanOrNull ?: false

        return if (groupId != null && memberId != null) {
            val group = bot.getGroupOrFail(groupId)
            if (noCache) {
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
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun getGroupMemberList(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val groupMemberListData = mutableListOf<MemberInfoData>()
        return if (groupId != null) {
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
            ResponseDTO.MemberList(groupMemberListData)
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    // https://github.com/richardchien/coolq-http-api/blob/master/src/cqhttp/plugins/web/http.cpp#L375
    suspend fun handleQuickOperation(params: Map<String, JsonElement>): ResponseDTO {
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

    suspend fun getRecord(params: Map<String, JsonElement>): ResponseDTO {
        val file = params["file"]?.jsonPrimitive?.contentOrNull
        val outFormat = params["out_format"]?.jsonPrimitive?.contentOrNull // Currently not supported
        return if (file != null) {
            val cachedFile = getCachedRecordFile(file)
            cachedFile?.let {
                val fileType = with(it.readBytes().copyOfRange(0, 10).toUHexString("")) {
                    when {
                        startsWith("2321414D52") -> "amr"
                        startsWith("02232153494C4B5F5633") -> "silk"
                        else -> "unknown"
                    }
                }
                ResponseDTO.RecordInfo(
                    RecordInfoData(
                        it.absolutePath,
                        it.nameWithoutExtension,
                        it.nameWithoutExtension,
                        fileType
                    )
                )
            } ?: ResponseDTO.PluginFailure()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    suspend fun getImage(params: Map<String, JsonElement>): ResponseDTO {
        val file = params["file"]?.jsonPrimitive?.contentOrNull
        return if (file != null) {
            val image = getCachedImageFile(file)
            image?.let { cachedImageMeta ->
                var cachedFile = getDataFile("image", cachedImageMeta.fileName)
                if (cachedFile == null) {
                    HttpClient.getBytes(cachedImageMeta.url)?.let { saveImage(cachedImageMeta.fileName, it) }
                }
                cachedFile = getDataFile("image", cachedImageMeta.fileName)

                cachedFile?.let {
                    val fileType = with(it.readBytes().copyOfRange(0, 8).toUHexString("")) {
                        when {
                            startsWith("FFD8") -> "jpg"
                            startsWith("89504E47") -> "png"
                            startsWith("47494638") -> "gif"
                            startsWith("424D") -> "bmp"
                            else -> "unknown"
                        }
                    }
                    ResponseDTO.ImageInfo(
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
            } ?: ResponseDTO.PluginFailure()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun canSendImage(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.CanSendImage()
    }

    fun canSendRecord(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.CanSendRecord()
    }

    fun getStatus(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.PluginStatus(PluginStatusData(online = bot.isActive, good = bot.isActive))
    }

    fun getVersionInfo(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.VersionInfo(
            VersionInfoData(
                coolq_directory = PluginBase.dataFolder.absolutePath
            )
        )
    }

    fun setRestartPlugin(params: Map<String, JsonElement>): ResponseDTO {
        val delay = params["delay"]?.jsonPrimitive?.int ?: 0
        return ResponseDTO.GeneralSuccess()
    }

    fun cleanDataDir(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.GeneralSuccess()
    }

    fun cleanPluginLog(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.GeneralSuccess()
    }

    ////////////////
    ////  v11  ////
    //////////////

    fun setGroupName(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val name = params["group_name"]?.jsonPrimitive?.content

        return if (groupId != null && name != null && name != "") {
            bot.getGroupOrFail(groupId).name = name
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    @MiraiExperimentalApi
    @LowLevelApi
    @Suppress("DuplicatedCode")
    suspend fun getGroupHonorInfo(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.longOrNull
        val type = params["type"]?.jsonPrimitive?.contentOrNull

        return if (groupId != null && type != null) {
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

            finalData?.let { ResponseDTO.HonorInfo(it) } ?: ResponseDTO.MiraiFailure()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    /////////////////
    //// hidden ////
    ///////////////

    @MiraiExperimentalApi
    @LowLevelApi
    suspend fun setGroupAnnouncement(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val content = params["content"]?.jsonPrimitive?.content

        return if (groupId != null && content != null && content != "") {
            Mirai.sendGroupAnnouncement(bot, groupId, GroupAnnouncement(msg = GroupAnnouncementMsg(text = content)))
            ResponseDTO.GeneralSuccess()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    ////////////////////////////////
    //// currently unsupported ////
    //////////////////////////////

    fun getCookies(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun getCSRFToken(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun getCredentials(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun sendDiscussMessage(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun setGroupAnonymous(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val enable = params["enable"]?.jsonPrimitive?.long ?: true
        return if (groupId != null) {
            // Not supported
            // bot.getGroupOrFail(groupId).settings.isAnonymousChatEnabled = enable
            ResponseDTO.MiraiFailure()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun setGroupAdmin(params: Map<String, JsonElement>): ResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val enable = params["enable"]?.jsonPrimitive?.long ?: true
        return if (groupId != null && memberId != null) {
            // Not supported
            // bot.getGroupOrFail(groupId).getMemberOrFail(memberId).permission = if (enable) MemberPermission.ADMINISTRATOR else MemberPermission.MEMBER
            ResponseDTO.MiraiFailure()
        } else {
            ResponseDTO.InvalidRequest()
        }
    }

    fun setDiscussLeave(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }

    fun sendLike(params: Map<String, JsonElement>): ResponseDTO {
        return ResponseDTO.MiraiFailure()
    }


}