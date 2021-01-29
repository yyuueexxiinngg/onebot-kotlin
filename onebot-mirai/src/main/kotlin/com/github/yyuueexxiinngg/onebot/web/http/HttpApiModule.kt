/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package com.github.yyuueexxiinngg.onebot.web.http

import com.github.yyuueexxiinngg.onebot.BotSession
import com.github.yyuueexxiinngg.onebot.PluginSettings
import com.github.yyuueexxiinngg.onebot.callMiraiApi
import com.github.yyuueexxiinngg.onebot.data.common.CQResponseDTO
import com.github.yyuueexxiinngg.onebot.logger
import com.github.yyuueexxiinngg.onebot.util.toJson
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.nio.charset.Charset
import kotlin.coroutines.EmptyCoroutineContext

fun Application.oneBotApiServer(session: BotSession, settings: PluginSettings.HTTPSettings) {
    install(CallLogging)
    // it.second -> if is async call
    routing {
        oneBotApi("/send_msg", settings) {
            val responseDTO = callMiraiApi("send_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/send_private_msg", settings) {
            val responseDTO = callMiraiApi("send_private_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/send_group_msg", settings) {
            val responseDTO = callMiraiApi("send_group_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/send_discuss_msg", settings) {
            val responseDTO = callMiraiApi("send_discuss_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/delete_msg", settings) {
            val responseDTO = callMiraiApi("delete_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/send_like", settings) {
            val responseDTO = callMiraiApi("send_like", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_kick", settings) {
            val responseDTO = callMiraiApi("set_group_kick", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_ban", settings) {
            val responseDTO = callMiraiApi("set_group_ban", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_anonymous_ban", settings) {
            val responseDTO = callMiraiApi("set_group_anonymous_ban", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_whole_ban", settings) {
            val responseDTO = callMiraiApi("set_group_whole_ban", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_admin", settings) {
            val responseDTO = callMiraiApi("set_group_admin", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_anonymous", settings) {
            val responseDTO = callMiraiApi("set_group_anonymous", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_card", settings) {
            val responseDTO = callMiraiApi("set_group_card", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_leave", settings) {
            val responseDTO = callMiraiApi("set_group_leave", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_special_title", settings) {
            val responseDTO = callMiraiApi("set_group_special_title", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_discuss_leave", settings) {
            val responseDTO = callMiraiApi("set_discuss_leave", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_friend_add_request", settings) {
            val responseDTO = callMiraiApi("set_friend_add_request", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_group_add_request", settings) {
            val responseDTO = callMiraiApi("set_group_add_request", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_login_info", settings) {
            val responseDTO = callMiraiApi("get_login_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_stranger_info", settings) {
            val responseDTO = callMiraiApi("get_stranger_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_friend_list", settings) {
            val responseDTO = callMiraiApi("get_friend_list", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_group_list", settings) {
            val responseDTO = callMiraiApi("get_group_list", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_group_info", settings) {
            val responseDTO = callMiraiApi("get_group_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_group_member_info", settings) {
            val responseDTO = callMiraiApi("get_group_member_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_group_member_list", settings) {
            val responseDTO = callMiraiApi("get_group_member_list", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_cookies", settings) {
            val responseDTO = callMiraiApi("get_cookies", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_csrf_token", settings) {
            val responseDTO = callMiraiApi("get_csrf_token", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_credentials", settings) {
            val responseDTO = callMiraiApi("get_credentials", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_record", settings) {
            val responseDTO = callMiraiApi("get_record", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_image", settings) {
            val responseDTO = callMiraiApi("get_image", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/can_send_image", settings) {
            val responseDTO = callMiraiApi("can_send_image", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/can_send_record", settings) {
            val responseDTO = callMiraiApi("can_send_record", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_status", settings) {
            val responseDTO = callMiraiApi("get_status", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/get_version_info", settings) {
            val responseDTO = callMiraiApi("get_version_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/set_restart_plugin", settings) {
            val responseDTO = callMiraiApi("set_restart_plugin", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/clean_data_dir", settings) {
            val responseDTO = callMiraiApi("clean_data_dir", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/clean_plugin_log", settings) {
            val responseDTO = callMiraiApi("clean_plugin_log", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
        oneBotApi("/.handle_quick_operation", settings) {
            val responseDTO = callMiraiApi(".handle_quick_operation", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }

        ////////////////
        ////  v11  ////
        //////////////

        oneBotApi("/set_group_name", settings) {
            val responseDTO = callMiraiApi("set_group_name", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }

        oneBotApi("/get_group_honor_info", settings) {
            val responseDTO = callMiraiApi("get_group_honor_info", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }

        oneBotApi("/get_msg", settings) {
            val responseDTO = callMiraiApi("get_msg", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }

        /////////////////
        //// hidden ////
        ///////////////

        oneBotApi("/_set_group_announcement", settings) {
            val responseDTO = callMiraiApi("_set_group_announcement", it.first, session.cqApiImpl)
            if (!it.second) call.responseDTO(responseDTO)
        }
    }
}

internal suspend fun ApplicationCall.responseDTO(dto: CQResponseDTO) {
    val jsonToSend = dto.toJson()
    logger.debug("HTTP API response: $jsonToSend")
    respondText(jsonToSend, defaultTextContentType(ContentType("application", "json")))
}

suspend fun checkAccessToken(call: ApplicationCall, settings: PluginSettings.HTTPSettings): Boolean {
    if (settings.accessToken != "") {
        val accessToken =
            call.parameters["access_token"] ?: call.request.headers["Authorization"]?.let {
                Regex("""(?:[Tt]oken|Bearer)\s+(.*)""").find(it)?.groupValues?.get(1)
            }
        if (accessToken != null) {
            if (accessToken != settings.accessToken) {
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
    return Json.parseToJsonElement(parsed).jsonObject
}

internal inline fun Route.oneBotApi(
    path: String,
    settings: PluginSettings.HTTPSettings,
    crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(Pair<Map<String, JsonElement>, Boolean>) -> Unit
) {
    route(path) {
        get {
            if (checkAccessToken(call, settings)) {
                body(Pair(paramsToJson(call.parameters), false))
            }
        }
        post {
            if (checkAccessToken(call, settings)) {
                val contentType = call.request.contentType()
                when {
                    contentType.contentSubtype.contains("form-urlencoded") -> {
                        body(Pair(paramsToJson(call.receiveParameters()), false))
                    }
                    contentType.contentSubtype.contains("json") -> {
                        body(Pair(Json.parseToJsonElement(call.receiveTextWithCorrectEncoding()).jsonObject, false))
                    }
                    else -> {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }

    route("${path}_async") {
        get {
            if (checkAccessToken(call, settings)) {
                val req = call.parameters
                call.responseDTO(CQResponseDTO.CQAsyncStarted())
                CoroutineScope(EmptyCoroutineContext).launch {
                    body(Pair(paramsToJson(req), true))
                }
            }
        }
        post {
            if (checkAccessToken(call, settings)) {
                val contentType = call.request.contentType()
                when {
                    contentType.contentSubtype.contains("form-urlencoded") -> {
                        body(Pair(paramsToJson(call.receiveParameters()), true))
                    }
                    contentType.contentSubtype.contains("json") -> {
                        val req = call.receiveTextWithCorrectEncoding()
                        call.responseDTO(CQResponseDTO.CQAsyncStarted())
                        CoroutineScope(EmptyCoroutineContext).launch {
                            body(Pair(Json.parseToJsonElement(req).jsonObject, true))
                        }
                    }
                    else -> {
                        call.respond(HttpStatusCode.BadRequest)
                    }
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