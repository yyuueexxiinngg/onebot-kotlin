package com.github.yyuueexxiinngg.onebot.util

import com.github.yyuueexxiinngg.onebot.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.Voice
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.File

suspend fun getCachedRecordFile(name: String): File? = withContext(Dispatchers.IO) {
    if (name.endsWith(".cqrecord")) getDataFile("record", name)
    else getDataFile("record", "$name.cqrecord")
}

suspend fun tryResolveCachedRecord(name: String, contact: Contact?): Voice? {
    val cacheFile = getCachedRecordFile(name)
    if (cacheFile != null) {
        if (cacheFile.canRead()) {
            logger.info("此语音已缓存, 如需删除缓存请至 ${cacheFile.absolutePath}")
            return cacheFile.toExternalResource().use { res ->
                contact?.let {
                    (it as Group).uploadVoice(res)
                }
            }
        } else {
            logger.error("Record $name cache file cannot read.")
        }
    } else {
        logger.error("Record $name cache file cannot read.")
    }
    return null
}