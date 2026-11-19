package com.kore.session

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val parser = Json {
    ignoreUnknownKeys = false
    explicitNulls = true
}

class SessionFormatException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

object SessionImporter {
    fun fromJson(text: String): SessionDocument {
        val root = try {
            parser.parseToJsonElement(text).jsonObject
        } catch (error: Exception) {
            throw SessionFormatException("The file does not contain a valid Kore session", error)
        }
        return fromObject(root)
    }

    fun fromObject(root: JsonObject): SessionDocument {
        val version = root["koreSession"]?.jsonPrimitive?.intOrNull
            ?: throw SessionFormatException("koreSession is missing or is not an integer")
        val migrated = when {
            version > CURRENT_SESSION_VERSION -> throw SessionFormatException(
                "The session uses version $version, which is newer than the app ($CURRENT_SESSION_VERSION)",
            )
            version == 1 -> migrateV1(root)
            version < CURRENT_SESSION_VERSION -> throw SessionFormatException(
                "Session version $version has no available migrator",
            )
            else -> normalizeV2(root)
        }
        validateEnvelope(migrated)
        return SessionDocument(migrated)
    }

    private fun validateEnvelope(root: JsonObject) {
        val profile = root["profile"] as? JsonObject
            ?: throw SessionFormatException("The session is missing the profile object")
        if (profile.textValue("id") == null) {
            throw SessionFormatException("The session is missing profile.id")
        }
        for (key in REQUIRED_COLLECTIONS) {
            if (root[key] !is JsonArray) {
                throw SessionFormatException("The $key field must be a JSON array")
            }
        }
        val sources = root["sources"] as? JsonObject
            ?: throw SessionFormatException("The sources field must be a JSON object")
        if (sources["connections"] !is JsonArray) {
            throw SessionFormatException("The sources.connections field must be a JSON array")
        }
        if (root["settings"] !is JsonObject) {
            throw SessionFormatException("The settings field must be a JSON object")
        }
    }

    private val REQUIRED_COLLECTIONS = setOf(
        "activities",
        "museum",
        "personalBests",
        "shoes",
        "routes",
    )

    private fun migrateV1(root: JsonObject): JsonObject = JsonObject(
        root.toMutableMap().apply {
            this["koreSession"] = JsonPrimitive(CURRENT_SESSION_VERSION)
            this.putIfAbsent("activities", root["workouts"] as? JsonArray ?: JsonArray(emptyList()))
            this.putIfAbsent("museum", JsonArray(emptyList()))
            this.putIfAbsent("personalBests", root["personalRecords"] as? JsonArray ?: JsonArray(emptyList()))
            this.putIfAbsent("shoes", JsonArray(emptyList()))
            this.putIfAbsent("routes", JsonArray(emptyList()))
            this.putIfAbsent(
                "sources",
                JsonObject(mapOf("connections" to JsonArray(emptyList()))),
            )
            this.putIfAbsent("settings", JsonObject(emptyMap()))
        },
    )

    private fun normalizeV2(root: JsonObject): JsonObject = JsonObject(
        root.toMutableMap().apply {
            this.putIfAbsent("routes", JsonArray(emptyList()))
        },
    )
}
