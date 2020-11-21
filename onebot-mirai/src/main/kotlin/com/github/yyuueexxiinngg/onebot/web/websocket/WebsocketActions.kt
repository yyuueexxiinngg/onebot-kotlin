package com.github.yyuueexxiinngg.onebot.web.websocket

import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.json.*
import com.github.yyuueexxiinngg.onebot.MiraiApi
import com.github.yyuueexxiinngg.onebot.callMiraiApi
import com.github.yyuueexxiinngg.onebot.data.common.CQResponseDTO
import com.github.yyuueexxiinngg.onebot.logger
import com.github.yyuueexxiinngg.onebot.util.toJson
import kotlin.coroutines.EmptyCoroutineContext

suspend fun handleWebSocketActions(outgoing: SendChannel<Frame>, mirai: MiraiApi, cqActionText: String) {
    try {
        logger.debug("WebSocket收到操作请求: $cqActionText")
        val json = Json.parseToJsonElement(cqActionText).jsonObject
        val echo = json["echo"]
        var action = json["action"]?.jsonPrimitive?.content
        val responseDTO: CQResponseDTO
        if (action?.endsWith("_async") == true) {
            responseDTO = CQResponseDTO.CQAsyncStarted()
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
