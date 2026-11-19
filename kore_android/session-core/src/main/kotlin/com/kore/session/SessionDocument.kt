package com.kore.session

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

const val CURRENT_SESSION_VERSION = 2

@OptIn(ExperimentalSerializationApi::class)
private val exportJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
    explicitNulls = true
}

class SessionDocument internal constructor(
    internal val root: JsonObject,
) {
    val version: Int
        get() = root.getValue("koreSession").jsonPrimitive.int

    val profileId: String
        get() = root.objectValue("profile").textValue("id")!!

    val profileLabel: String
        get() = root.objectValue("profile").textValue("label") ?: profileId

    fun inventory(): SessionInventory = SessionInventory(
        activities = root.arraySize("activities"),
        museumEntries = root.arraySize("museum"),
        personalBests = root.arraySize("personalBests"),
        shoes = root.arraySize("shoes"),
        routes = root.arraySize("routes"),
        sources = root.objectValue("sources").arraySize("connections"),
        unknownTopLevelKeys = root.keys - KNOWN_TOP_LEVEL_KEYS,
    )

    fun toJson(): String = exportJson.encodeToString(JsonObject.serializer(), root)
}

data class SessionInventory(
    val activities: Int,
    val museumEntries: Int,
    val personalBests: Int,
    val shoes: Int,
    val routes: Int,
    val sources: Int,
    val unknownTopLevelKeys: Set<String>,
)

internal val KNOWN_TOP_LEVEL_KEYS = setOf(
    "koreSession",
    "profile",
    "activities",
    "museum",
    "personalBests",
    "shoes",
    "routes",
    "sources",
    "settings",
)

internal fun JsonObject.arraySize(key: String): Int = (this[key] as? JsonArray)?.size ?: 0

internal fun JsonObject.objectValue(key: String): JsonObject = this[key] as? JsonObject
    ?: throw SessionFormatException("The $key field must be a JSON object")

internal fun JsonObject.textValue(key: String): String? =
    jsonPrimitiveOrNull(key)?.contentOrNull?.takeIf(String::isNotBlank)

private fun JsonObject.jsonPrimitiveOrNull(key: String) =
    this[key] as? kotlinx.serialization.json.JsonPrimitive
