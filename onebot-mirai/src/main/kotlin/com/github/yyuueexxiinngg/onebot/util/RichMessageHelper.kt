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

import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.ServiceMessage
import net.mamoe.mirai.message.data.SimpleServiceMessage
import net.mamoe.mirai.message.data.buildXmlMessage
import net.mamoe.mirai.utils.MiraiExperimentalApi

@OptIn(MiraiExperimentalApi::class)
object RichMessageHelper {
    fun share(u: String, title: String?, content: String?, image: String?) = buildXmlMessage(60) {
        templateId = 12345
        serviceId = 1
        action = "web"
        brief = "[分享] " + (title ?: "")
        url = u
        item {
            layout = 2
            if (image != null) {
                picture(image)
            }
            if (title != null) {
                title(title)
            }
            if (content != null) {
                summary(content)
            }
        }
    }

    fun contactQQ(bot: Bot, id: Long): ServiceMessage {
        val nick = bot.getFriend(id)?.nick
        return xmlMessage(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                    "<msg templateID=\"12345\" action=\"plugin\" p_actionData=\"AppCmd://OpenContactInfo/?uin=$id\" " +
                    "brief=\"推荐了$nick\" serviceID=\"14\" " +
                    "i_actionData=\"mqqapi://card/show_pslcard?src_type=internal&amp;source=sharecard&amp;version=1&amp;uin=$id\" " +
                    "a_actionData=\"mqqapi://card/show_pslcard?src_type=internal&amp;source=sharecard&amp;version=1&amp;uin=$id\">" +
                    "<item layout=\"0\" mode=\"1\"><summary>推荐联系人</summary><hr/></item>" +
                    "<item layout=\"2\" mode=\"1\">" +
                    "<picture cover=\"mqqapi://card/show_pslcard?src_type=internal&amp;source=sharecard&amp;version=1&amp;uin=$id\"/>" +
                    "<title>$nick</title><summary>帐号：$id</summary></item><source/></msg>"
        )
    }

    fun contactGroup(bot: Bot, id: Long): SimpleServiceMessage {
        val group = bot.getGroup(id)
        // TODO: 创建人，链接
        val founder = "未知创建人"
        val url = "https://github.com/mamoe/mirai"
        return xmlMessage(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                    "<msg templateID=\"-1\" action=\"web\" brief=\"推荐了${group?.name}群\" serviceID=\"15\"" +
                    " i_actionData=\"group:$id\" url=\"$url\"><item layout=\"0\" mode=\"1\"><summary>推荐群</summary><hr/></item>" +
                    "<item layout=\"2\" mode=\"1\"><picture cover=\"http://p.qlogo.cn/gh/$id/$id/100\"/>" +
                    "<title>${group?.name}</title><summary>创建人：$founder</summary></item><source/></msg>"
        )
    }
}

@OptIn(MiraiExperimentalApi::class)
fun xmlMessage(content: String) = SimpleServiceMessage(60, content)

@OptIn(MiraiExperimentalApi::class)
fun jsonMessage(content: String) = SimpleServiceMessage(1, content)
