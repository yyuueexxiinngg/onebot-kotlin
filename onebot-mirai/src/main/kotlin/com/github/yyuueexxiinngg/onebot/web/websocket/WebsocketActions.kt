package com.github.yyuueexxiinngg.onebot.web.websocket

import com.github.yyuueexxiinngg.onebot.MiraiApi
import com.github.yyuueexxiinngg.onebot.callMiraiApi
import com.github.yyuueexxiinngg.onebot.data.common.ResponseDTO
import com.github.yyuueexxiinngg.onebot.logger
import com.github.yyuueexxiinngg.onebot.util.toJson
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.EmptyCoroutineContext

suspend fun handleWebSocketActions(outgoing: SendChannel<Frame>, mirai: MiraiApi, actionText: String) {
    try {
        logger.debug("WebSocket收到操作请求: $actionText")
        val json = Json.parseToJsonElement(actionText).jsonObject
        val echo = json["echo"]
        var action = json["action"]?.jsonPrimitive?.content
        val responseDTO: ResponseDTO
        if (action?.endsWith("_async") == true) {
            responseDTO = ResponseDTO.AsyncStarted()
            action = action.replace("_async", "")
            CoroutineScope(EmptyCoroutineContext).launch {
                callMiraiApi(action, json["params"]?.jsonObject ?: mapOf(), mirai)
            }
        } else {
            responseDTO = callMiraiApi(action, json["params"]?.jsonObject ?: mapOf(), mirai)
        }
        responseDTO.echo = echo
        val jsonToSend = responseDTO.toJson()
        logger.debug("WebSocket将返回结果: $jsonToSend")
        outgoing.send(Frame.Text(jsonToSend))
    } catch (e: Exception) {
        logger.error(e)
    }
}
