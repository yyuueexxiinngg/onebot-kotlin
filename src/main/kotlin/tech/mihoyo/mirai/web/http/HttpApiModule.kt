/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package tech.mihoyo.mirai.web.http

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import tech.mihoyo.mirai.BotSession
import tech.mihoyo.mirai.callMiraiApi
import tech.mihoyo.mirai.data.common.CQResponseDTO
import tech.mihoyo.mirai.util.logger
import tech.mihoyo.mirai.util.toJson
import java.nio.charset.Charset
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
@KtorExperimentalAPI
fun Application.cqHttpApiServer(session: BotSession, serviceConfig: HttpApiServerServiceConfig) {
    // it.second -> if is async call
    routing {
        cqHttpApi("/send_msg", serviceConfig) {
            val responseDTO = callMiraiApi("send_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/send_private_msg", serviceConfig) {
            val responseDTO = callMiraiApi("send_private_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/send_group_msg", serviceConfig) {
            val responseDTO = callMiraiApi("send_group_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/send_discuss_msg", serviceConfig) {
            val responseDTO = callMiraiApi("send_discuss_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/delete_msg", serviceConfig) {
            val responseDTO = callMiraiApi("delete_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/send_like", serviceConfig) {
            val responseDTO = callMiraiApi("send_like", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_kick", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_kick", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_ban", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_ban", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_anonymous_ban", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_anonymous_ban", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_whole_ban", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_whole_ban", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_admin", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_admin", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_anonymous", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_anonymous", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_card", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_card", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_leave", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_leave", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_special_title", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_special_title", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_discuss_leave", serviceConfig) {
            val responseDTO = callMiraiApi("set_discuss_leave", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_friend_add_request", serviceConfig) {
            val responseDTO = callMiraiApi("set_friend_add_request", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_group_add_request", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_add_request", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_login_info", serviceConfig) {
            val responseDTO = callMiraiApi("get_login_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_stranger_info", serviceConfig) {
            val responseDTO = callMiraiApi("get_stranger_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_friend_list", serviceConfig) {
            val responseDTO = callMiraiApi("get_friend_list", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_group_list", serviceConfig) {
            val responseDTO = callMiraiApi("get_group_list", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_group_info", serviceConfig) {
            val responseDTO = callMiraiApi("get_group_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_group_member_info", serviceConfig) {
            val responseDTO = callMiraiApi("get_group_member_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_group_member_list", serviceConfig) {
            val responseDTO = callMiraiApi("get_group_member_list", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_cookies", serviceConfig) {
            val responseDTO = callMiraiApi("get_cookies", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_csrf_token", serviceConfig) {
            val responseDTO = callMiraiApi("get_csrf_token", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_credentials", serviceConfig) {
            val responseDTO = callMiraiApi("get_credentials", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_record", serviceConfig) {
            val responseDTO = callMiraiApi("get_record", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_image", serviceConfig) {
            val responseDTO = callMiraiApi("get_image", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/can_send_image", serviceConfig) {
            val responseDTO = callMiraiApi("can_send_image", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/can_send_record", serviceConfig) {
            val responseDTO = callMiraiApi("can_send_record", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_status", serviceConfig) {
            val responseDTO = callMiraiApi("get_status", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/get_version_info", serviceConfig) {
            val responseDTO = callMiraiApi("get_version_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/set_restart_plugin", serviceConfig) {
            val responseDTO = callMiraiApi("set_restart_plugin", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/clean_data_dir", serviceConfig) {
            val responseDTO = callMiraiApi("clean_data_dir", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/clean_plugin_log", serviceConfig) {
            val responseDTO = callMiraiApi("clean_plugin_log", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        cqHttpApi("/.handle_quick_operation", serviceConfig) {
            val responseDTO = callMiraiApi(".handle_quick_operation", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }

        ////////////////
        //// addon ////
        //////////////

        cqHttpApi("/set_group_name", serviceConfig) {
            val responseDTO = callMiraiApi("set_group_name", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
    }
}

internal suspend fun ApplicationCall.responseDTO(dto: CQResponseDTO) {
    val jsonToSend = dto.toJson()
    logger.debug("HTTP API response: $jsonToSend")
    respondText(jsonToSend, defaultTextContentType(ContentType("application", "json")))
}

suspend fun checkAccessToken(call: ApplicationCall, serviceConfig: HttpApiServerServiceConfig): Boolean {
    if (serviceConfig.accessToken != null && serviceConfig.accessToken != "") {
        val accessToken = call.parameters["access_token"] ?: call.request.headers["Authorization"]
        if (accessToken != null) {
            if (accessToken != serviceConfig.accessToken) {
                call.respond(HttpStatusCode.Forbidden)
                return false
            }
        } else {
            call.respond(HttpStatusCode.Unauthorized)
            return false
        }
    }
    return true
}

@OptIn(UnstableDefault::class)
fun paramsToJson(params: Parameters): JsonObject {
/*    val parsed = "{\"" + URLDecoder.decode(params.formUrlEncode(), "UTF-8")
        .replace("\"", "\\\"")
        .replace("&", "\",\"")
        .replace("=", "\":\"") + "\"}"*/
    val mapped = params.toMap().map { it.key to (it.value[0].toLongOrNull() ?: it.value[0]) }
    var parsed = "{"
    mapped.forEach {
        parsed += "\"${it.first}\":"
        parsed += if (it.second is String)
            "\"${it.second}\""
        else
            "${it.second}"

        if (it != mapped.last())
            parsed += ","
    }
    parsed += "}"
    logger.debug("HTTP API Received: $parsed")
    return Json.parseJson(parsed).jsonObject
}

@OptIn(UnstableDefault::class)
@KtorExperimentalAPI
@ExperimentalCoroutinesApi
@ContextDsl
internal inline fun Route.cqHttpApi(
    path: String,
    serviceConfig: HttpApiServerServiceConfig,
    crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(Pair<Map<String, JsonElement>, Boolean>) -> Unit
) {
    route(path) {
        get {
            if (checkAccessToken(call, serviceConfig)) {
                body(Pair(paramsToJson(call.parameters), false))
            }
        }
        post {
            if (checkAccessToken(call, serviceConfig)) {
                body(Pair(Json.parseJson(call.receiveTextWithCorrectEncoding()).jsonObject, false))
            }
        }
    }

    route("${path}_async") {
        get {
            if (checkAccessToken(call, serviceConfig)) {
                val req = call.parameters
                call.responseDTO(CQResponseDTO.CQAsyncStarted())
                CoroutineScope(EmptyCoroutineContext).launch {
                    body(Pair(paramsToJson(req), true))
                }
            }
        }
        post {
            if (checkAccessToken(call, serviceConfig)) {
                val req = call.receiveTextWithCorrectEncoding()
                call.responseDTO(CQResponseDTO.CQAsyncStarted())
                CoroutineScope(EmptyCoroutineContext).launch {
                    body(Pair(Json.parseJson(req).jsonObject, true))
                }
            }
        }
    }
}

// https://github.com/ktorio/ktor/issues/384#issuecomment-458542686
/**
 * Receive the request as String.
 * If there is no Content-Type in the HTTP header specified use ISO_8859_1 as default charset, see https://www.w3.org/International/articles/http-charset/index#charset.
 * But use UTF-8 as default charset for application/json, see https://tools.ietf.org/html/rfc4627#section-3
 */
private suspend fun ApplicationCall.receiveTextWithCorrectEncoding(): String {
    fun ContentType.defaultCharset(): Charset = when (this) {
        ContentType.Application.Json -> Charsets.UTF_8
        else -> Charsets.ISO_8859_1
    }

    val contentType = request.contentType()
    val suitableCharset = contentType.charset() ?: contentType.defaultCharset()
    return receiveStream().bufferedReader(charset = suitableCharset).readText()
}