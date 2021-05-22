package com.github.yyuueexxiinngg.onebot.util

import java.security.MessageDigest

@OptIn(ExperimentalUnsignedTypes::class)
internal fun ByteArray.toUHexString(
    separator: String = " ",
    offset: Int = 0,
    length: Int = this.size - offset
): String {
    if (length == 0) {
        return ""
    }
    val lastIndex = offset + length
    return buildString(length * 2) {
        this@toUHexString.forEachIndexed { index, it ->
            if (index in offset until lastIndex) {
                var ret = it.toUByte().toString(16).toUpperCase()
                if (ret.length == 1) ret = "0$ret"
                append(ret)
                if (index < lastIndex - 1) append(separator)
            }
        }
    }
}

fun md5(data: ByteArray): ByteArray {
    return MessageDigest.getInstance("MD5").digest(data)
}

fun md5(str: String): ByteArray = md5(str.toByteArray())