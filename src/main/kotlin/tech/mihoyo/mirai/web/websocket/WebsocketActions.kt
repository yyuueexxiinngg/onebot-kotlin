package tech.mihoyo.mirai.web.websocket

import io.ktor.http.cio.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import net.mamoe.mirai.LowLevelAPI
import tech.mihoyo.mirai.MiraiApi
import tech.mihoyo.mirai.callMiraiApi
import tech.mihoyo.mirai.data.common.CQResponseDTO
import tech.mihoyo.mirai.util.logger
import tech.mihoyo.mirai.util.toJson
import kotlin.coroutines.EmptyCoroutineContext

suspend fun handleWebSocketActions(outgoing: SendChannel<Frame>, mirai: MiraiApi, cqActionText: String) {
    try {
        logger.debug(cqActionText)
        val json = Json.parseToJsonElement(cqActionText).jsonObject
        val echo = json["echo"]
        var action = json["action"]?.let { it.jsonPrimitive.content }
        val responseDTO: CQResponseDTO
        if (action?.endsWith("_async") == true) {
            responseDTO = CQResponseDTO.CQAsyncStarted()
            action = action.replace("_async", "")
            CoroutineScope(EmptyCoroutineContext).launch {
                callMiraiApi(action, json["params"]?.jsonObject?: mapOf(), mirai)
            }
        } else {
            responseDTO = callMiraiApi(action, json["params"]?.jsonObject?: mapOf(), mirai)
        }
        responseDTO.echo = echo
        val jsonToSend = responseDTO.toJson()
        logger.debug(jsonToSend)
        outgoing.send(Frame.Text(jsonToSend))
    } catch (e: Exception) {
        logger.error(e)
    }
}
