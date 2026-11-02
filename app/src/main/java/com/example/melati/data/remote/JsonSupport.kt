package com.example.melati.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal val MelatiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/** Normalisasi nama key: `ticket_number`, `ticketNumber`, `TicketNumber` dianggap sama. */
private fun normalize(key: String): String = key.lowercase().replace("_", "").replace("-", "")

internal fun JsonElement?.asObjectOrNull(): JsonObject? = when (this) {
    is JsonObject -> this
    is JsonArray -> firstOrNull()?.asObjectOrNull()
    else -> null
}

internal fun JsonElement?.asStringOrNull(): String? = when (this) {
    is JsonPrimitive -> content.takeIf { it.isNotBlank() && it != "null" }
    else -> null
}

internal fun JsonObject.pick(vararg keys: String): JsonElement? {
    val wanted = keys.map(::normalize).toSet()
    return entries.firstOrNull { normalize(it.key) in wanted }?.value
}

internal fun JsonObject.pickString(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key -> pick(key)?.asStringOrNull() }

internal fun JsonObject.pickObject(vararg keys: String): JsonObject? =
    keys.firstNotNullOfOrNull { key -> pick(key).asObjectOrNull() }

internal fun JsonObject.pickArray(vararg keys: String): JsonArray? =
    keys.firstNotNullOfOrNull { key -> pick(key) as? JsonArray }

/**
 * Response API biasanya dibungkus (`{"data": {...}}`). Bila ada pembungkus yang
 * dikenali, turun satu level; jika tidak, pakai objek apa adanya.
 */
internal fun JsonObject.unwrapPayload(): JsonObject {
    val wrapped = pickObject("data", "ticket", "tiket", "result", "payload")
    return wrapped ?: this
}

internal fun JsonObject.allStringsRecursive(maxDepth: Int = 2): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    fun walk(obj: JsonObject, depth: Int) {
        if (depth > maxDepth) return
        for ((key, value) in obj) {
            when (value) {
                is JsonObject -> walk(value, depth + 1)
                is JsonArray -> value.forEach { item -> (item as? JsonObject)?.let { walk(it, depth + 1) } }
                else -> value.asStringOrNull()?.let { out += key to it }
            }
        }
    }
    walk(this, 0)
    return out
}

internal fun JsonElement?.jsonArrayOrNull(): JsonArray? = this as? JsonArray

internal fun JsonArray.objects(): List<JsonObject> =
    mapNotNull { element -> (element as? JsonObject) ?: runCatching { element.jsonObject }.getOrNull() }

internal fun JsonElement.stringifyPrimitive(): String? = runCatching { jsonPrimitive.content }.getOrNull()
