package com.github.yyuueexxiinngg.onebot.util

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.Member
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

class ImgUtil {
    enum class ImageState {
        RequireUpload,
        FileExist
    }

    companion object {
        private val imgStoreGroupPicUpClass =
            Class.forName("net.mamoe.mirai.internal.network.protocol.packet.chat.image.ImgStore\$GroupPicUp")
        private val imgStoreGroupPicUpClassConstructor = imgStoreGroupPicUpClass.getDeclaredConstructor()
        private val netWorkHandlerClass = Class.forName("net.mamoe.mirai.internal.network.QQAndroidBotNetworkHandler")
        private val netWorkHandlerClassConstructor = netWorkHandlerClass.getDeclaredConstructor(
            Class.forName("kotlin.coroutines.CoroutineContext"),
            Class.forName("net.mamoe.mirai.internal.QQAndroidBot")
        )
        private val sendAndExpectMethod = netWorkHandlerClass.declaredMethods.find { it.name == "sendAndExpect" }
        private val groupPicUpInvokeMethod = imgStoreGroupPicUpClass.getDeclaredMethod(
            "invoke",
            Class.forName("net.mamoe.mirai.internal.network.QQAndroidClient"),
            Long::class.java,
            Long::class.java,
            ByteArray::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            Long::class.java,
            String::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java,
            Int::class.java
        )

        init {
            imgStoreGroupPicUpClassConstructor.isAccessible = true
            netWorkHandlerClassConstructor.isAccessible = true
            groupPicUpInvokeMethod.isAccessible = true
        }

        suspend fun tryGroupPicUp(bot: Bot, groupCode: Long, md5: String, size: Int): ImageState {
            val botClientFiled = bot.javaClass.superclass.getDeclaredField("client")
            botClientFiled.isAccessible = true
            val botClient = botClientFiled.get(bot)

            val network = bot::class.members.find { it.name == "_network" }

            val outGoingPacket = groupPicUpInvokeMethod.invoke(
                imgStoreGroupPicUpClassConstructor.newInstance(),
                botClient,
                bot.id,
                groupCode,
                hexStringToByteArray(md5),
                size,
                0, 0, 1000, 0, getRandomString(16) + ".gif", 5, 9, 1, 1006, 0
            )
            val response = sendAndExpectMethod?.kotlinFunction?.callSuspend(
                network!!.call(bot),
                outGoingPacket,
                5000, 2
            )

            return if (response.toString().contains("FileExists")) {
                ImageState.FileExist
            } else {
                ImageState.RequireUpload
            }
        }

        fun md5ToImageId(md5: String, contact: Contact): String {
            return when (contact) {
                is Group -> "{${md5.substring(0, 8)}-" +
                        "${md5.substring(8, 12)}-" +
                        "${md5.substring(12, 16)}-" +
                        "${md5.substring(16, 20)}-" +
                        "${md5.substring(20)}}.mirai"
                is Friend, is Member -> "/0-00-$md5"
                else -> ""
            }
        }

        private fun getRandomString(length: Int): String =
            getRandomString(length, *defaultRanges)

        private val defaultRanges: Array<CharRange> = arrayOf('a'..'z', 'A'..'Z', '0'..'9')

        private fun getRandomString(length: Int, vararg charRanges: CharRange): String =
            String(CharArray(length) { charRanges[Random.Default.nextInt(0..charRanges.lastIndex)].random() })

        private fun hexStringToByteArray(s: String): ByteArray {
            val len = s.length
            val data = ByteArray(len / 2)
            var i = 0
            while (i < len) {
                data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                        + Character.digit(s[i + 1], 16)).toByte()
                i += 2
            }
            return data
        }
    }
}