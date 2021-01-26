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

package com.github.yyuueexxiinngg.onebot.util

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.util.*
import kotlinx.serialization.json.*
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MusicKind
import net.mamoe.mirai.message.data.MusicShare
import net.mamoe.mirai.message.data.SimpleServiceMessage

@OptIn(KtorExperimentalAPI::class)
abstract class MusicProvider {
    val http = HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
    }

    abstract suspend fun send(id: String): Message
}

object Music {
    fun custom(url: String, audio: String, title: String, content: String?, image: String?): Message {
        return xmlMessage(
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

object QQMusic : MusicProvider() {
    suspend fun search(name: String, page: Int, cnt: Int): JsonElement {
        val result =
            http.get<String>("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?aggr=1&cr=1&flag_qc=0&p=$page&n=$cnt&w=$name")
        return Json.parseToJsonElement(result.substring(8, result.length - 1))
    }

    suspend fun getPlayUrl(mid: String): String {
        val result = http.get<String>(
            "https://c.y.qq.com/base/fcgi-bin/fcg_music_express_mobile3.fcg?&jsonpCallback=MusicJsonCallback&cid=205361747&songmid=" +
                    mid + "&filename=C400" + mid + ".m4a&guid=7549058080"
        )
        val json =
            Json.parseToJsonElement(result).jsonObject.getValue("data").jsonObject.getValue("items").jsonArray[0].jsonObject
        if (json["subcode"]?.jsonPrimitive?.int == 0) {
            return "http://aqqmusic.tc.qq.com/amobile.music.tc.qq.com/C400$mid.m4a?guid=7549058080&amp;vkey=${json["vkey"]!!.jsonPrimitive.content}&amp;uin=0&amp;fromtag=38"
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
        return Json.parseToJsonElement(result).jsonObject.getValue("songinfo").jsonObject.getValue("data").jsonObject
    }

    fun toXmlMessage(
        song: String,
        singer: String,
        songId: String,
        albumId: String,
        playUrl: String
    ): Message {
        return xmlMessage(
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
        val trackInfo = info.getValue("track_info").jsonObject
        val url = getPlayUrl(trackInfo.getValue("file").jsonObject["media_mid"]!!.jsonPrimitive.content)
        return MusicShare(
            MusicKind.QQMusic,
            trackInfo["name"]!!.jsonPrimitive.content,
            trackInfo.getValue("singer").jsonArray[0].jsonObject["name"]!!.jsonPrimitive.content,
            "https://i.y.qq.com/v8/playsong.html?_wv=1&amp;songid=$id&amp;souce=qqshare&amp;source=qqshare&amp;ADTAG=qqshare",
            "http://imgcache.qq.com/music/photo/album_500/${id.substring(id.length - 2)}/500_albumpic_${id}_0.jpg",
            url
        )
    }
}

object NeteaseMusic : MusicProvider() {
    suspend fun getSongInfo(id: String = ""): JsonObject {
        val result = http.get<String>("http://music.163.com/api/song/detail/?id=$id&ids=%5B$id%5D")
        return Json.parseToJsonElement(result).jsonObject.getValue("songs").jsonArray[0].jsonObject
    }

    fun toXmlMessage(song: String, singer: String, songId: String, coverUrl: String): SimpleServiceMessage {
        return xmlMessage(
            "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                    "<msg serviceID=\"2\" templateID=\"1\" action=\"web\" brief=\"[分享] $song\" sourceMsgId=\"0\" " +
                    "url=\"http://music.163.com/m/song/$songId\" " +
                    "flag=\"0\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"2\">" +
                    "<audio cover=\"$coverUrl?param=90y90\" " +
                    "src=\"https://music.163.com/song/media/outer/url?id=$songId.mp3\" /><title>$song</title><summary>$singer</summary></item><source name=\"网易云音乐\" " +
                    "icon=\"https://pic.rmb.bdstatic.com/911423bee2bef937975b29b265d737b3.png\" " +
                    "url=\"http://web.p.qq.com/qqmpmobile/aio/app.html?id=100495085\" action=\"app\" " +
                    "a_actionData=\"com.netease.cloudmusic\" i_actionData=\"tencent100495085://\" appid=\"100495085\" /></msg>"
        )
    }

    override suspend fun send(id: String): Message {
        val info = getSongInfo(id)
        val song = info.getValue("name").jsonPrimitive.content
        val artists = info.getValue("artists").jsonArray
        val albumInfo = info.getValue("album").jsonObject

        return MusicShare(
            MusicKind.NeteaseCloudMusic,
            song,
            artists[0].jsonObject.getValue("name").jsonPrimitive.content,
            "http://music.163.com/m/song/$id",
            "http://imgcache.qq.com/music/photo/album_500/${id.substring(id.length - 2)}/500_albumpic_${id}_0.jpg",
            albumInfo.getValue("picUrl").jsonPrimitive.content,
            "[分享] $song"
        )
    }
}
