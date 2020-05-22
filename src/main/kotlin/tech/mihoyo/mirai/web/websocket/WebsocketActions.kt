package tech.mihoyo.mirai.web.websocket

import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.*
import net.mamoe.mirai.contact.PermissionDeniedException
import tech.mihoyo.mirai.MiraiApi
import tech.mihoyo.mirai.PluginBase
import tech.mihoyo.mirai.data.common.CQResponseDTO
import tech.mihoyo.mirai.util.toJson

@OptIn(UnstableDefault::class)
suspend fun handleWebSocketActions(outgoing: SendChannel<Frame>, mirai: MiraiApi, cqActionText: String) {
    try {
        val json = Json.parseJson(cqActionText).jsonObject
        val echo = json["echo"]
        lateinit var responseDTO: CQResponseDTO
        try {
            when (json["action"]?.content) {
                "send_msg" -> responseDTO = mirai.cqSendMessage(json["params"]!!.jsonObject)
                "send_private_msg" -> responseDTO = mirai.cqSendPrivateMessage(json["params"]!!.jsonObject)
                "send_group_msg" -> responseDTO = mirai.cqSendGroupMessage(json["params"]!!.jsonObject)
                "send_discuss_msg" -> responseDTO = mirai.cqSendDiscussMessage(json["params"]!!.jsonObject)
                "delete_msg" -> responseDTO = mirai.cqDeleteMessage(json["params"]!!.jsonObject)
                "send_like" -> responseDTO = mirai.cqSendLike(json["params"]!!.jsonObject)
                "set_group_kick" -> responseDTO = mirai.cqSetGroupKick(json["params"]!!.jsonObject)
                "set_group_ban" -> responseDTO = mirai.cqSetGroupBan(json["params"]!!.jsonObject)
                "set_group_anonymous_ban" -> responseDTO = mirai.cqSetAnonymousBan(json["params"]!!.jsonObject)
                "set_group_whole_ban" -> responseDTO = mirai.cqSetWholeGroupBan(json["params"]!!.jsonObject)
                "set_group_admin" -> responseDTO = mirai.cqSetGroupAdmin(json["params"]!!.jsonObject)
                "set_group_anonymous" -> responseDTO = mirai.cqSetGroupAnonymous(json["params"]!!.jsonObject)
                "set_group_card" -> responseDTO = mirai.cqSetGroupCard(json["params"]!!.jsonObject)
                "set_group_leave" -> responseDTO = mirai.cqSetGroupLeave(json["params"]!!.jsonObject)
                "set_group_special_title" -> responseDTO = mirai.cqSetGroupSpecialTitle(json["params"]!!.jsonObject)
                "set_discuss_leave" -> responseDTO = mirai.cqSetDiscussLeave(json["params"]!!.jsonObject)
                "set_friend_add_request" -> responseDTO = mirai.cqSetFriendAddRequest(json["params"]!!.jsonObject)
                "set_group_add_request" -> responseDTO = mirai.cqSetGroupAddRequest(json["params"]!!.jsonObject)
                "get_login_info" -> responseDTO = mirai.cqGetLoginInfo(json["params"]!!.jsonObject)
                "get_stranger_info" -> responseDTO = mirai.cqGetStrangerInfo(json["params"]!!.jsonObject)
                "get_friend_list" -> responseDTO = mirai.cqGetFriendList(json["params"]!!.jsonObject)
                "get_group_list" -> responseDTO = mirai.cqGetGroupList(json["params"]!!.jsonObject)
                "get_group_info" -> responseDTO = mirai.cqGetGroupInfo(json["params"]!!.jsonObject)
                "get_group_member_info" -> responseDTO = mirai.cqGetGroupMemberInfo(json["params"]!!.jsonObject)
                "get_group_member_list" -> responseDTO = mirai.cqGetGroupMemberList(json["params"]!!.jsonObject)
                "get_cookies" -> responseDTO = mirai.cqGetCookies(json["params"]!!.jsonObject)
                "get_csrf_token" -> responseDTO = mirai.cqGetCSRFToken(json["params"]!!.jsonObject)
                "get_credentials" -> responseDTO = mirai.cqGetCredentials(json["params"]!!.jsonObject)
                "get_record" -> responseDTO = mirai.cqGetRecord(json["params"]!!.jsonObject)
                "get_image" -> responseDTO = mirai.cqGetImage(json["params"]!!.jsonObject)
                "can_send_image" -> responseDTO = mirai.cqCanSendImage(json["params"]!!.jsonObject)
                "can_send_record" -> responseDTO = mirai.cqCanSendRecord(json["params"]!!.jsonObject)
                "get_status" -> responseDTO = mirai.cqGetStatus(json["params"]!!.jsonObject)
                "get_version_info" -> responseDTO = mirai.cqGetVersionInfo(json["params"]!!.jsonObject)
                "set_restart_plugin" -> responseDTO = mirai.cqSetRestartPlugin(json["params"]!!.jsonObject)
                "clean_data_dir" -> responseDTO = mirai.cqCleanDataDir(json["params"]!!.jsonObject)
                "clean_plugin_log" -> responseDTO = mirai.cqCleanPluginLog(json["params"]!!.jsonObject)
                else -> println(json["action"]?.content)
            }
        } catch (e: PermissionDeniedException) {
            responseDTO = CQResponseDTO.CQMiraiFailure
        } catch (e: Exception) {
            PluginBase.logger.error(e)
            responseDTO = CQResponseDTO.CQPluginFailure
        }
        responseDTO.echo = echo
        outgoing.send(Frame.Text(responseDTO.toJson()))
    } catch (e: Exception) {
        PluginBase.logger.error(e)
    }
}