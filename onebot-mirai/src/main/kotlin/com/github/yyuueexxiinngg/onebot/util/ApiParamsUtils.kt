package com.github.yyuueexxiinngg.onebot.util

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

typealias ApiParams = Map<String, JsonElement>

internal val JsonElement?.int: Int get() = this?.jsonPrimitive?.int ?: throw IllegalArgumentException()
internal val JsonElement?.intOrNull: Int? get() = this?.jsonPrimitive?.intOrNull

internal val JsonElement?.long: Long get() = this?.jsonPrimitive?.long ?: throw IllegalArgumentException()
internal val JsonElement?.longOrNull: Long? get() = this?.jsonPrimitive?.longOrNull

internal val JsonElement?.booleanOrNull: Boolean? get() = this?.jsonPrimitive?.booleanOrNull

internal val JsonElement?.string: String get() = this?.jsonPrimitive?.content ?: throw IllegalArgumentException()
internal val JsonElement?.stringOrNull: String? get() = this?.jsonPrimitive?.contentOrNull

