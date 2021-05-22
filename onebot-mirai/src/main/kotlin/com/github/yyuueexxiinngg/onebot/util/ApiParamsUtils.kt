package com.github.yyuueexxiinngg.onebot.util

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

typealias ApiParams = Map<String, JsonElement>

internal val JsonElement?.int: Int get() = this?.jsonPrimitive?.content?.toInt() ?: throw IllegalArgumentException()
internal val JsonElement?.intOrNull: Int? get() = this?.jsonPrimitive?.content?.toIntOrNull()

internal val JsonElement?.long: Long get() = this?.jsonPrimitive?.content?.toLong() ?: throw IllegalArgumentException()
internal val JsonElement?.longOrNull: Long? get() = this?.jsonPrimitive?.content?.toLongOrNull()

internal val JsonElement?.booleanOrNull: Boolean? get() = this?.jsonPrimitive?.content?.toBooleanStrictOrNull()

internal val JsonElement?.string: String get() = this?.jsonPrimitive?.content ?: throw IllegalArgumentException()
internal val JsonElement?.stringOrNull: String? get() = this?.jsonPrimitive?.contentOrNull

internal fun String.toBooleanStrictOrNull(): Boolean? = when {
    this.equals("true", ignoreCase = true) -> true
    this.equals("false", ignoreCase = true) -> false
    else -> null
}
