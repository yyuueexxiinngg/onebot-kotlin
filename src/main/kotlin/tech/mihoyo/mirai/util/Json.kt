package tech.mihoyo.mirai.util

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import tech.mihoyo.mirai.data.common.*
import kotlin.reflect.KClass

inline fun <reified T : Any> T.toJson(
    serializer: SerializationStrategy<T>? = null
): String = if (serializer == null) {
    CQJson.json.encodeToString(this)
} else CQJson.json.encodeToString(serializer, this)

// 序列化列表时，stringify需要使用的泛型是T，而非List<T>
// 因为使用的stringify的stringify(objs: List<T>)重载
inline fun <reified T : Any> List<T>.toJson(
    serializer: SerializationStrategy<List<T>>? = null
): String = if (serializer == null) CQJson.json.encodeToString(this)
else CQJson.json.encodeToString(serializer, this)

/**
 * Json解析规则，需要注册支持的多态的类
 */
object CQJson {
    @OptIn(InternalSerializationApi::class)
    val json = Json {

        isLenient = true
        ignoreUnknownKeys = true

        @Suppress("UNCHECKED_CAST")
        serializersModule = SerializersModule {
            polymorphic(CQEventDTO::class) {
                this.subclass(CQGroupMessagePacketDTO.serializer())
                this.subclass(CQPrivateMessagePacketDTO.serializer())
                this.subclass(CQIgnoreEventDTO.serializer())

                /*
                 * BotEventDTO为sealed Class，以BotEventDTO为接收者的函数可以自动进行多态序列化
                 * 这里通过向EventDTO为接收者的方法进行所有事件类型的多态注册
                 */
                CQBotEventDTO::class.sealedSubclasses.forEach {
                    val clazz = it as KClass<CQBotEventDTO>
                    this.subclass(clazz.serializer())
                }

                CQResponseDTO::class.sealedSubclasses.forEach {
                    val clazz = it as KClass<CQBotEventDTO>
                    this.subclass(clazz.serializer())
                }

                CQResponseDataDTO::class.sealedSubclasses.forEach {
                    val clazz = it as KClass<CQBotEventDTO>
                    this.subclass(clazz.serializer())
                }
            }
        }
    }
}