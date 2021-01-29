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
suspend fun callMiraiApi(action: String?, params: Map<String, JsonElement>, mirai: MiraiApi): CQResponseDTO {
    var responseDTO: CQResponseDTO = CQResponseDTO.CQPluginFailure()
    try {
        when (action) {
            "send_msg" -> responseDTO = mirai.cqSendMessage(params)
            "send_private_msg" -> responseDTO = mirai.cqSendPrivateMessage(params)
            "send_group_msg" -> responseDTO = mirai.cqSendGroupMessage(params)
            "send_discuss_msg" -> responseDTO = mirai.cqSendDiscussMessage(params)
            "delete_msg" -> responseDTO = mirai.cqDeleteMessage(params)
            "send_like" -> responseDTO = mirai.cqSendLike(params)
            "set_group_kick" -> responseDTO = mirai.cqSetGroupKick(params)
            "set_group_ban" -> responseDTO = mirai.cqSetGroupBan(params)
            "set_group_anonymous_ban" -> responseDTO = mirai.cqSetGroupAnonymousBan(params)
            "set_group_whole_ban" -> responseDTO = mirai.cqSetWholeGroupBan(params)
            "set_group_admin" -> responseDTO = mirai.cqSetGroupAdmin(params)
            "set_group_anonymous" -> responseDTO = mirai.cqSetGroupAnonymous(params)
            "set_group_card" -> responseDTO = mirai.cqSetGroupCard(params)
            "set_group_leave" -> responseDTO = mirai.cqSetGroupLeave(params)
            "set_group_special_title" -> responseDTO = mirai.cqSetGroupSpecialTitle(params)
            "set_discuss_leave" -> responseDTO = mirai.cqSetDiscussLeave(params)
            "set_friend_add_request" -> responseDTO = mirai.cqSetFriendAddRequest(params)
            "set_group_add_request" -> responseDTO = mirai.cqSetGroupAddRequest(params)
            "get_login_info" -> responseDTO = mirai.cqGetLoginInfo(params)
            "get_stranger_info" -> responseDTO = mirai.cqGetStrangerInfo(params)
            "get_friend_list" -> responseDTO = mirai.cqGetFriendList(params)
            "get_group_list" -> responseDTO = mirai.cqGetGroupList(params)
            "get_group_info" -> responseDTO = mirai.cqGetGroupInfo(params)
            "get_group_member_info" -> responseDTO = mirai.cqGetGroupMemberInfo(params)
            "get_group_member_list" -> responseDTO = mirai.cqGetGroupMemberList(params)
            "get_cookies" -> responseDTO = mirai.cqGetCookies(params)
            "get_csrf_token" -> responseDTO = mirai.cqGetCSRFToken(params)
            "get_credentials" -> responseDTO = mirai.cqGetCredentials(params)
            "get_record" -> responseDTO = mirai.cqGetRecord(params)
            "get_image" -> responseDTO = mirai.cqGetImage(params)
            "can_send_image" -> responseDTO = mirai.cqCanSendImage(params)
            "can_send_record" -> responseDTO = mirai.cqCanSendRecord(params)
            "get_status" -> responseDTO = mirai.cqGetStatus(params)
            "get_version_info" -> responseDTO = mirai.cqGetVersionInfo(params)
            "set_restart_plugin" -> responseDTO = mirai.cqSetRestartPlugin(params)
            "clean_data_dir" -> responseDTO = mirai.cqCleanDataDir(params)
            "clean_plugin_log" -> responseDTO = mirai.cqCleanPluginLog(params)
            ".handle_quick_operation" -> responseDTO = mirai.cqHandleQuickOperation(params)

            "set_group_name" -> responseDTO = mirai.cqSetGroupName(params)
            "get_group_honor_info" -> responseDTO = mirai.cqGetGroupHonorInfo(params)
            "get_msg" -> responseDTO = mirai.cqGetMessage(params)

            "_set_group_announcement" -> responseDTO = mirai.cqSetGroupAnnouncement(params)
            else -> {
                logger.error("未知OneBot API: $action")
            }
        }
    } catch (e: PermissionDeniedException) {
        logger.debug("机器人无操作权限, 调用的API: /$action")
        responseDTO = CQResponseDTO.CQMiraiFailure()
    } catch (e: Exception) {
        logger.error(e)
        responseDTO = CQResponseDTO.CQPluginFailure()
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

    suspend fun cqSendMessage(params: Map<String, JsonElement>): CQResponseDTO {
        if (params.contains("message_type")) {
            when (params["message_type"]?.jsonPrimitive?.content) {
                "private" -> return cqSendPrivateMessage(params)
                "group" -> return cqSendGroupMessage(params)
            }
        } else {
            when {
                params["group_id"] != null -> return cqSendGroupMessage(params)
                params["discuss_id"] != null -> return cqSendGroupMessage(params)
                params["user_id"] != null -> return cqSendPrivateMessage(params)
            }
        }
        return CQResponseDTO.CQInvalidRequest()
    }

    @Suppress("DuplicatedCode")
    suspend fun cqSendGroupMessage(params: Map<String, JsonElement>): CQResponseDTO {
        val targetGroupId = params["group_id"]!!.jsonPrimitive.long
        val raw = params["auto_escape"]?.jsonPrimitive?.booleanOrNull ?: false
        val messages = params["message"]

        val group = bot.getGroupOrFail(targetGroupId)
        cqMessageToMessageChains(bot, group, messages, raw)?.let {
            return if (it.content.isNotEmpty()) {
                val receipt = group.sendMessage(it)
                cachedSourceQueue.add(receipt.source)
                CQResponseDTO.CQMessageResponse(receipt.source.internalIds.toCQMessageId(bot.id, group.id))
            } else {
                CQResponseDTO.CQMessageResponse(-1)
            }
        }
        return CQResponseDTO.CQInvalidRequest()
    }

    @Suppress("DuplicatedCode")
    suspend fun cqSendPrivateMessage(params: Map<String, JsonElement>): CQResponseDTO {
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

        cqMessageToMessageChains(bot, contact, messages, raw)?.let {
            return if (it.content.isNotEmpty()) {
                val receipt = contact.sendMessage(it)
                cachedSourceQueue.add(receipt.source)
                CQResponseDTO.CQMessageResponse(receipt.source.internalIds.toCQMessageId(bot.id, contact.id))
            } else {
                CQResponseDTO.CQMessageResponse(-1)
            }
        }
        return CQResponseDTO.CQInvalidRequest()
    }

    suspend fun cqDeleteMessage(params: Map<String, JsonElement>): CQResponseDTO {
        val messageId = params["message_id"]?.jsonPrimitive?.intOrNull
        messageId?.let {
            cachedSourceQueue[it].recall()
            return CQResponseDTO.CQGeneralSuccess()
        }
        return CQResponseDTO.CQInvalidRequest()
    }

    suspend fun cqGetMessage(params: Map<String, JsonElement>): CQResponseDTO {
        val messageId = params["message_id"]?.jsonPrimitive?.intOrNull
        if (PluginSettings.db.enable) {
            if (messageId != null) {
                PluginBase.db?.apply {
                    val message = String(get(messageId.toByteArray())).deserializeJsonToMessageChain()
                    val rawMessage = WrappedCQMessageChainString("")
                    message.forEach { rawMessage.value += it.toCQString() }

                    with(message) {
                        when (source.kind) {
                            MessageSourceKind.GROUP -> {
                                return CQResponseDTO.CQGetMessageResponse(
                                    CQGetMessageData(
                                        source.time.toLong(),
                                        "group",
                                        messageId,
                                        messageId,
                                        CQMemberDTO(source.fromId, "unknown"),
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
                                return CQResponseDTO.CQGetMessageResponse(
                                    CQGetMessageData(
                                        source.time.toLong(),
                                        "private",
                                        messageId,
                                        messageId,
                                        CQQQDTO(source.fromId, "unknown"),
                                        message.toMessageChainDTO { it != UnknownMessageDTO }
                                    )
                                )
                            }
                        }
                    }
                }
            }
            return CQResponseDTO.CQInvalidRequest()
        } else {
            return CQResponseDTO.CQInvalidRequest("请配置开启数据库")
        }
    }

    suspend fun cqSetGroupKick(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        return if (groupId != null && memberId != null) {
            bot.getGroupOrFail(groupId).getMemberOrFail(memberId).kick("")
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    suspend fun cqSetGroupBan(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val duration = params["duration"]?.jsonPrimitive?.int ?: 30 * 60
        return if (groupId != null && memberId != null) {
            bot.getGroupOrFail(groupId).getMemberOrFail(memberId).mute(duration)
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    @OptIn(LowLevelApi::class)
    suspend fun cqSetGroupAnonymousBan(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val flag = params["anonymous"]?.jsonObject?.get("flag")?.jsonPrimitive?.content
            ?: params["anonymous_flag"]?.jsonPrimitive?.content
            ?: params["flag"]?.jsonPrimitive?.content

        val duration = params["duration"]?.jsonPrimitive?.int ?: 30 * 60
        return if (groupId != null && flag != null) {
            val splits = flag.split("&", limit = 2)
            Mirai.muteAnonymousMember(bot, splits[0], splits[1], groupId, duration)
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqSetWholeGroupBan(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val enable = params["enable"]?.jsonPrimitive?.booleanOrNull ?: true
        return if (groupId != null) {
            bot.getGroupOrFail(groupId).settings.isMuteAll = enable
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqSetGroupCard(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val card = params["card"]?.jsonPrimitive?.contentOrNull ?: ""
        val enable = params["enable"]?.jsonPrimitive?.booleanOrNull ?: true
        return if (groupId != null && memberId != null) {
            bot.getGroupOrFail(groupId).getMemberOrFail(memberId).nameCard = card
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    suspend fun cqSetGroupLeave(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val dismiss = params["enable"]?.jsonPrimitive?.booleanOrNull ?: false
        return if (groupId != null) {
            // Not supported
            if (dismiss) return CQResponseDTO.CQMiraiFailure()

            bot.getGroupOrFail(groupId).quit()
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqSetGroupSpecialTitle(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val specialTitle = params["special_title"]?.jsonPrimitive?.contentOrNull ?: ""
        val duration = params["duration"]?.jsonPrimitive?.int ?: -1  // Not supported
        return if (groupId != null && memberId != null) {
            bot.getGroupOrFail(groupId).getMemberOrFail(memberId).specialTitle = specialTitle
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    suspend fun cqSetFriendAddRequest(params: Map<String, JsonElement>): CQResponseDTO {
        val flag = params["flag"]?.jsonPrimitive?.contentOrNull
        val approve = params["approve"]?.jsonPrimitive?.booleanOrNull ?: true
        val remark = params["remark"]?.jsonPrimitive?.contentOrNull
        return if (flag != null) {
            val event = cacheRequestQueue[flag.toLongOrNull()]
            if (event is NewFriendRequestEvent)
                if (approve) event.accept() else event.reject()
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    suspend fun cqSetGroupAddRequest(params: Map<String, JsonElement>): CQResponseDTO {
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
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqGetLoginInfo(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQLoginInfo(bot.id, bot.nick)
    }

    @OptIn(ConsoleExperimentalApi::class, MiraiExperimentalApi::class)
    suspend fun cqGetStrangerInfo(params: Map<String, JsonElement>): CQResponseDTO {
        val userId = params["user_id"]?.jsonPrimitive?.long
        return if (userId != null) {
            val profile = Mirai.queryProfile(bot, userId)
            CQResponseDTO.CQStrangerInfo(
                CQStrangerInfoData(
                    userId,
                    profile.nickname,
                    profile.sex.name.toLowerCase(),
                    profile.age
                )
            )
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqGetFriendList(params: Map<String, JsonElement>): CQResponseDTO {
        val cqFriendList = mutableListOf<CQFriendData>()
        bot.friends.forEach { friend ->
            cqFriendList.add(CQFriendData(friend.id, friend.nick, friend.remark))
        }
        return CQResponseDTO.CQFriendList(cqFriendList)
    }

    fun cqGetGroupList(params: Map<String, JsonElement>): CQResponseDTO {
        val cqGroupList = mutableListOf<CQGroupData>()
        bot.groups.forEach { group ->
            cqGroupList.add(CQGroupData(group.id, group.name))
        }
        return CQResponseDTO.CQGroupList(cqGroupList)
    }

    /**
     * 获取群信息
     * 不支持获取群容量, 返回0
     */
    fun cqGetGroupInfo(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val noCache = params["no_cache"]?.jsonPrimitive?.booleanOrNull ?: false

        return if (groupId != null) {
            val group = bot.getGroupOrFail(groupId)
            CQResponseDTO.CQGroupInfo(group.id, group.name, group.members.size + 1, 0)
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    @MiraiExperimentalApi
    @LowLevelApi
    suspend fun cqGetGroupMemberInfo(params: Map<String, JsonElement>): CQResponseDTO {
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
                    CQResponseDTO.CQMemberInfo(
                        CQMemberInfoData(
                            group.id,
                            it.uin,
                            nickname = it.nameCard,
                            card = it.nameCard,
                            role = if (it.permission == MemberPermission.ADMINISTRATOR) "admin" else it.permission.name.toLowerCase(),
                            title = it.specialTitle,
                            card_changeable = group.botPermission == MemberPermission.OWNER
                        )
                    )
                } ?: CQResponseDTO.CQMiraiFailure()
            } else {
                val member = bot.getGroupOrFail(groupId).getMemberOrFail(memberId)
                CQResponseDTO.CQMemberInfo(CQMemberInfoData(member))
            }
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqGetGroupMemberList(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val cqGroupMemberListData = mutableListOf<CQMemberInfoData>()
        return if (groupId != null) {
            var isBotIncluded = false
            val group = bot.getGroupOrFail(groupId)
            val members = group.members
            members.forEach { member ->
                run {
                    if (member.id == bot.id) isBotIncluded = true
                    cqGroupMemberListData.add(CQMemberInfoData(member))
                }
            }
            if (!isBotIncluded) cqGroupMemberListData.add(CQMemberInfoData(group.botAsMember))
            CQResponseDTO.CQMemberList(cqGroupMemberListData)
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    // https://github.com/richardchien/coolq-http-api/blob/master/src/cqhttp/plugins/web/http.cpp#L375
    suspend fun cqHandleQuickOperation(params: Map<String, JsonElement>): CQResponseDTO {
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
                    return cqSendMessage(nextCallParams)
                }

                if (messageType == "group") {
                    // TODO: 备忘, 暂未支持
                    val isAnonymous = false
                    if (operation?.get("delete")?.jsonPrimitive?.booleanOrNull == true) {
                        return cqDeleteMessage(context)
                    }
                    if (operation?.get("kick")?.jsonPrimitive?.booleanOrNull == true) {
                        return cqSetGroupKick(context)
                    }
                    if (operation?.get("ban")?.jsonPrimitive?.booleanOrNull == true) {
                        @Suppress("ConstantConditionIf")
                        (return if (isAnonymous) {
                            cqSetGroupAnonymousBan(context)
                        } else {
                            cqSetGroupBan(context)
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
                    return cqSetFriendAddRequest(nextCallParams)
                } else if (requestType == "group") {
                    return cqSetGroupAddRequest(nextCallParams)
                }
            }
            return CQResponseDTO.CQInvalidRequest()
        } catch (e: Exception) {
            return CQResponseDTO.CQPluginFailure()
        }
    }

    suspend fun cqGetRecord(params: Map<String, JsonElement>): CQResponseDTO {
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
                CQResponseDTO.CQRecordInfo(
                    CQRecordInfoData(
                        it.absolutePath,
                        it.nameWithoutExtension,
                        it.nameWithoutExtension,
                        fileType
                    )
                )
            } ?: CQResponseDTO.CQPluginFailure()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    suspend fun cqGetImage(params: Map<String, JsonElement>): CQResponseDTO {
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
                    CQResponseDTO.CQImageInfo(
                        CQImageInfoData(
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
            } ?: CQResponseDTO.CQPluginFailure()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqCanSendImage(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQCanSendImage()
    }

    fun cqCanSendRecord(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQCanSendRecord()
    }

    fun cqGetStatus(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQPluginStatus(CQPluginStatusData(online = bot.isActive, good = bot.isActive))
    }

    fun cqGetVersionInfo(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQVersionInfo(
            CQVersionInfoData(
                coolq_directory = PluginBase.dataFolder.absolutePath
            )
        )
    }

    fun cqSetRestartPlugin(params: Map<String, JsonElement>): CQResponseDTO {
        val delay = params["delay"]?.jsonPrimitive?.int ?: 0
        return CQResponseDTO.CQGeneralSuccess()
    }

    fun cqCleanDataDir(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQGeneralSuccess()
    }

    fun cqCleanPluginLog(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQGeneralSuccess()
    }

    ////////////////
    ////  v11  ////
    //////////////

    fun cqSetGroupName(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val name = params["group_name"]?.jsonPrimitive?.content

        return if (groupId != null && name != null && name != "") {
            bot.getGroupOrFail(groupId).name = name
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    @MiraiExperimentalApi
    @LowLevelApi
    @Suppress("DuplicatedCode")
    suspend fun cqGetGroupHonorInfo(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.longOrNull
        val type = params["type"]?.jsonPrimitive?.contentOrNull

        return if (groupId != null && type != null) {
            var finalData: CQGroupHonorInfoData? = null

            if (type == "talkative" || type == "all") {
                val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.TALKATIVE)
                val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
                val cqData = jsonData?.let { Gson().fromJson(it, CQGroupHonorInfoData::class.java) }
                finalData = cqData?.let { cqData }
            }

            if (type == "performer" || type == "all") {
                val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.PERFORMER)
                val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
                val cqData = jsonData?.let { Gson().fromJson(it, CQGroupHonorInfoData::class.java) }
                finalData = finalData?.apply { performerList = cqData?.actorList } ?: cqData?.let { cqData }
            }

            if (type == "legend" || type == "all") {
                val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.LEGEND)
                val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
                val cqData = jsonData?.let { Gson().fromJson(it, CQGroupHonorInfoData::class.java) }
                finalData = finalData?.apply { legendList = cqData?.legendList } ?: cqData?.let { cqData }
            }

            if (type == "strong_newbie" || type == "all") {
                val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.STRONG_NEWBIE)
                val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
                val cqData = jsonData?.let { Gson().fromJson(it, CQGroupHonorInfoData::class.java) }
                finalData = finalData?.apply { strongNewbieList = cqData?.strongNewbieList } ?: cqData?.let { cqData }
            }

            if (type == "emotion" || type == "all") {
                val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.EMOTION)
                val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
                val cqData = jsonData?.let { Gson().fromJson(it, CQGroupHonorInfoData::class.java) }
                finalData = finalData?.apply { emotionList = cqData?.emotionList } ?: cqData?.let { cqData }
            }

            if (type == "active") {
                val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.ACTIVE)
                val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
                val cqData = jsonData?.let { Gson().fromJson(it, CQGroupHonorInfoData::class.java) }
                finalData = finalData?.apply {
                    activeObj = cqData?.activeObj
                    showActiveObj = cqData?.showActiveObj
                } ?: cqData?.let { cqData }
            }

            if (type == "exclusive") {
                val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.EXCLUSIVE)
                val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
                val cqData = jsonData?.let { Gson().fromJson(it, CQGroupHonorInfoData::class.java) }
                finalData = finalData?.apply { exclusiveList = cqData?.exclusiveList } ?: cqData?.let { cqData }
            }

            if (type == "manage") {
                val data = Mirai.getRawGroupHonorListData(bot, groupId, GroupHonorType.MANAGE)
                val jsonData = data?.let { Json.encodeToString(GroupHonorListData.serializer(), it) }
                val cqData = jsonData?.let { Gson().fromJson(it, CQGroupHonorInfoData::class.java) }
                finalData = finalData?.apply { manageList = cqData?.manageList } ?: cqData?.let { cqData }
            }

            finalData?.let { CQResponseDTO.CQHonorInfo(it) } ?: CQResponseDTO.CQMiraiFailure()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    /////////////////
    //// hidden ////
    ///////////////

    @MiraiExperimentalApi
    @LowLevelApi
    suspend fun cqSetGroupAnnouncement(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val content = params["content"]?.jsonPrimitive?.content

        return if (groupId != null && content != null && content != "") {
            Mirai.sendGroupAnnouncement(bot, groupId, GroupAnnouncement(msg = GroupAnnouncementMsg(text = content)))
            CQResponseDTO.CQGeneralSuccess()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    ////////////////////////////////
    //// currently unsupported ////
    //////////////////////////////

    fun cqGetCookies(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQMiraiFailure()
    }

    fun cqGetCSRFToken(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQMiraiFailure()
    }

    fun cqGetCredentials(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQMiraiFailure()
    }

    fun cqSendDiscussMessage(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQMiraiFailure()
    }

    fun cqSetGroupAnonymous(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val enable = params["enable"]?.jsonPrimitive?.long ?: true
        return if (groupId != null) {
            // Not supported
            // bot.getGroupOrFail(groupId).settings.isAnonymousChatEnabled = enable
            CQResponseDTO.CQMiraiFailure()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqSetGroupAdmin(params: Map<String, JsonElement>): CQResponseDTO {
        val groupId = params["group_id"]?.jsonPrimitive?.long
        val memberId = params["user_id"]?.jsonPrimitive?.long
        val enable = params["enable"]?.jsonPrimitive?.long ?: true
        return if (groupId != null && memberId != null) {
            // Not supported
            // bot.getGroupOrFail(groupId).getMemberOrFail(memberId).permission = if (enable) MemberPermission.ADMINISTRATOR else MemberPermission.MEMBER
            CQResponseDTO.CQMiraiFailure()
        } else {
            CQResponseDTO.CQInvalidRequest()
        }
    }

    fun cqSetDiscussLeave(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQMiraiFailure()
    }

    fun cqSendLike(params: Map<String, JsonElement>): CQResponseDTO {
        return CQResponseDTO.CQMiraiFailure()
    }


}