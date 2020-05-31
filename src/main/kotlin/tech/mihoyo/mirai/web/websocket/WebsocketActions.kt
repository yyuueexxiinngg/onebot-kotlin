package tech.mihoyo.mirai.web.websocket

import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.*
import net.mamoe.mirai.contact.PermissionDeniedException
import tech.mihoyo.mirai.MiraiApi
import tech.mihoyo.mirai.data.common.CQResponseDTO
import tech.mihoyo.mirai.util.logger
import tech.mihoyo.mirai.util.toJson
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(UnstableDefault::class)
suspend fun handleWebSocketActions(outgoing: SendChannel<Frame>, mirai: MiraiApi, cqActionText: String) {
    try {
        logger.debug(cqActionText)
        val json = Json.parseJson(cqActionText).jsonObject
        val echo = json["echo"]
        var action = json["action"]?.content
        val responseDTO: CQResponseDTO
        if (action?.endsWith("_async") == true) {
            responseDTO = CQResponseDTO.CQAsyncStarted()
            action = action.replace("_async", "")
            CoroutineScope(EmptyCoroutineContext).launch {
                callMiraiApi(action, json["params"]!!.jsonObject, mirai)
            }
        } else {
            responseDTO = callMiraiApi(action, json["params"]!!.jsonObject, mirai)
        }
        responseDTO.echo = echo
        val jsonToSend = responseDTO.toJson()
        logger.debug(jsonToSend)
        outgoing.send(Frame.Text(jsonToSend))
    } catch (e: Exception) {
        logger.error(e)
    }
}

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
            "set_group_anonymous_ban" -> responseDTO = mirai.cqSetAnonymousBan(params)
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
            else -> {
                logger.error("未知CQHTTP API: $action")
            }
        }
    } catch (e: PermissionDeniedException) {
        responseDTO = CQResponseDTO.CQMiraiFailure()
    } catch (e: Exception) {
        logger.error(e)
        responseDTO = CQResponseDTO.CQPluginFailure()
    }

    return responseDTO
}