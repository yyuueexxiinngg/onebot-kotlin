/*
 *
 * Mirai Native
 *
 * Copyright (C) 2020 iTX Technologies
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author PeratX
 * @website https://github.com/iTXTech/mirai-native
 *
 */

package tech.mihoyo.mirai.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.*
import net.mamoe.mirai.message.data.Message

@OptIn(KtorExperimentalAPI::class)
abstract class MusicProvider {
    val http = HttpClient(CIO)

    abstract suspend fun send(id: String): Message
}

object Music {
    fun custom(url: String, audio: String, title: String, content: String?, image: String?): Message {
        return XmlMessage(
            "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                    "<msg serviceID=\"2\" templateID=\"1\" action=\"web\" brief=\"[分享] $title\" sourceMsgId=\"0\" " +
                    "url=\"$url\" " +
                    "flag=\"0\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"2\">" +
                    "<audio cover=\"$image\" " +
                    "src=\"$audio\" /><title>$title</title><summary>$content</summary></item><source name=\"Mirai\" " +
                    "icon=\"https://i.gtimg.cn/open/app_icon/01/07/98/56/1101079856_100_m.png\" " +
                    "url=\"http://web.p.qq.com/qqmpmobile/aio/app.html?id=1101079856\" action=\"app\" " +
                    "a_actionData=\"com.tencent.qqmusic\" i_actionData=\"tencent1101079856://\" appid=\"1101079856\" /></msg>"
        )
    }
}

@OptIn(UnstableDefault::class)
object QQMusic : MusicProvider() {
    suspend fun search(name: String, page: Int, cnt: Int): JsonElement {
        val result =
            http.get<String>("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?aggr=1&cr=1&flag_qc=0&p=$page&n=$cnt&w=$name")
        return Json.parseJson(result.substring(8, result.length - 1))
    }

    suspend fun getPlayUrl(mid: String): String {
        val result = http.get<String>(
            "https://c.y.qq.com/base/fcgi-bin/fcg_music_express_mobile3.fcg?&jsonpCallback=MusicJsonCallback&cid=205361747&songmid=" +
                    mid + "&filename=C400" + mid + ".m4a&guid=7549058080"
        )
        val json = Json.parseJson(result).jsonObject.getObject("data").getArray("items").getObject(0)
        if (json["subcode"]?.int == 0) {
            return "http://aqqmusic.tc.qq.com/amobile.music.tc.qq.com/C400$mid.m4a?guid=7549058080&amp;vkey=${json["vkey"]!!.content}&amp;uin=0&amp;fromtag=38"
        }
        return ""
    }

    suspend fun getSongInfo(id: String = "", mid: String = ""): JsonObject {
        val result = http.get<String>(
            "https://u.y.qq.com/cgi-bin/musicu.fcg?format=json&inCharset=utf8&outCharset=utf-8&notice=0&" +
                    "platform=yqq.json&needNewCode=0&data=" +
                    "{%22comm%22:{%22ct%22:24,%22cv%22:0},%22songinfo%22:{%22method%22:%22get_song_detail_yqq%22,%22param%22:" +
                    "{%22song_type%22:0,%22song_mid%22:%22$mid%22,%22song_id%22:$id},%22module%22:%22music.pf_song_detail_svr%22}}"
        )
        return Json.parseJson(result).jsonObject.getObject("songinfo").getObject("data")
    }

    fun toXmlMessage(song: String, singer: String, songId: String, albumId: String, playUrl: String): XmlMessage {
        return XmlMessage(
            "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                    "<msg serviceID=\"2\" templateID=\"1\" action=\"web\" brief=\"[分享] $song\" sourceMsgId=\"0\" " +
                    "url=\"https://i.y.qq.com/v8/playsong.html?_wv=1&amp;songid=$songId&amp;souce=qqshare&amp;source=qqshare&amp;ADTAG=qqshare\" " +
                    "flag=\"0\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"2\">" +
                    "<audio cover=\"http://imgcache.qq.com/music/photo/album_500/${albumId.substring(albumId.length - 2)}/500_albumpic_${albumId}_0.jpg\" " +
                    "src=\"$playUrl\" /><title>$song</title><summary>$singer</summary></item><source name=\"QQ音乐\" " +
                    "icon=\"https://i.gtimg.cn/open/app_icon/01/07/98/56/1101079856_100_m.png\" " +
                    "url=\"http://web.p.qq.com/qqmpmobile/aio/app.html?id=1101079856\" action=\"app\" " +
                    "a_actionData=\"com.tencent.qqmusic\" i_actionData=\"tencent1101079856://\" appid=\"1101079856\" /></msg>"
        )
    }

    override suspend fun send(id: String): Message {
        val info = getSongInfo(id)
        val trackInfo = info.getObject("track_info")
        val url = getPlayUrl(trackInfo.getObject("file")["media_mid"]!!.content)
        return toXmlMessage(
            trackInfo["name"]!!.content,
            trackInfo.getArray("singer").getObject(0)["name"]!!.content,
            id,
            trackInfo.getObject("album")["id"]!!.content,
            url
        )
    }
}

object NeteaseMusic : MusicProvider() {
    override suspend fun send(id: String): XmlMessage {
        TODO("Not yet implemented")
    }
}
