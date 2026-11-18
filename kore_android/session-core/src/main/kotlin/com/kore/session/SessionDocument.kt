package com.kore.session

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

const val CURRENT_SESSION_VERSION = 1

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

    val activeGoalLabel: String?
        get() = root.nullableObject("activeGoal")?.let { goal ->
            goal.textValue("label") ?: goal.textValue("distance")
        }

    val activePlanLabel: String?
        get() = root.nullableObject("activePlan")?.let { plan ->
            plan.textValue("label") ?: plan.textValue("tag") ?: plan.textValue("id")
        }

    val suuntoConnected: Boolean
        get() = root.objectValue("sync").objectValue("suunto").booleanValue("enabled") ?: false

    fun inventory(): SessionInventory = SessionInventory(
        archivedPlans = root.arraySize("planArchive"),
        workouts = root.arraySize("workouts"),
        personalRecords = root.arraySize("personalRecords"),
        shoes = root.arraySize("shoes"),
        diagnostics = root.arraySize("diagnostics"),
        ratingSnapshots = root.objectValue("ratings").arraySize("history"),
        unknownTopLevelKeys = root.keys - KNOWN_TOP_LEVEL_KEYS,
    )

    fun toJson(): String = exportJson.encodeToString(JsonObject.serializer(), root)
}

data class SessionInventory(
    val archivedPlans: Int,
    val workouts: Int,
    val personalRecords: Int,
    val shoes: Int,
    val diagnostics: Int,
    val ratingSnapshots: Int,
    val unknownTopLevelKeys: Set<String>,
)

internal val KNOWN_TOP_LEVEL_KEYS = setOf(
    "koreSession",
    "profile",
    "activeGoal",
    "goalHistory",
    "activePlan",
    "planArchive",
    "workouts",
    "personalRecords",
    "shoes",
    "diagnostics",
    "ratings",
    "pinnedWorkouts",
    "milestones",
    "sync",
    "settings",
)

internal fun JsonObject.arraySize(key: String): Int = (this[key] as? JsonArray)?.size ?: 0

internal fun JsonObject.objectValue(key: String): JsonObject = this[key] as? JsonObject
    ?: throw SessionFormatException("The $key field must be a JSON object")

internal fun JsonObject.nullableObject(key: String): JsonObject? = when (val value = this[key]) {
    null, JsonNull -> null
    is JsonObject -> value
    else -> throw SessionFormatException("The $key field must be a JSON object or null")
}

internal fun JsonObject.textValue(key: String): String? =
    jsonPrimitiveOrNull(key)?.contentOrNull?.takeIf(String::isNotBlank)

internal fun JsonObject.booleanValue(key: String): Boolean? =
    jsonPrimitiveOrNull(key)?.contentOrNull?.toBooleanStrictOrNull()

private fun JsonObject.jsonPrimitiveOrNull(key: String) =
    this[key] as? kotlinx.serialization.json.JsonPrimitive
