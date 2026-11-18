package com.kore.session

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
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
        when {
            version > CURRENT_SESSION_VERSION -> throw SessionFormatException(
                "The session uses version $version, which is newer than the app ($CURRENT_SESSION_VERSION)",
            )
            version < CURRENT_SESSION_VERSION -> throw SessionFormatException(
                "Session version $version has no available migrator",
            )
        }
        validateEnvelope(root)
        return SessionDocument(root)
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
        validateNullableObject(root, "activeGoal")
        validateNullableObject(root, "activePlan")
        val ratings = root["ratings"] as? JsonObject
            ?: throw SessionFormatException("The ratings field must be a JSON object")
        validateNullableObject(ratings, "current")
        if (ratings["history"] !is JsonArray) {
            throw SessionFormatException("The ratings.history field must be a JSON array")
        }
        val sync = root["sync"] as? JsonObject
            ?: throw SessionFormatException("The sync field must be a JSON object")
        if (sync["suunto"] !is JsonObject) {
            throw SessionFormatException("The sync.suunto field must be a JSON object")
        }
        if (root["settings"] !is JsonObject) {
            throw SessionFormatException("The settings field must be a JSON object")
        }
    }

    private fun validateNullableObject(root: JsonObject, key: String) {
        if (root[key] != JsonNull && root[key] !is JsonObject) {
            throw SessionFormatException("The $key field must be a JSON object or null")
        }
    }

    private val REQUIRED_COLLECTIONS = setOf(
        "goalHistory",
        "planArchive",
        "workouts",
        "personalRecords",
        "shoes",
        "diagnostics",
        "pinnedWorkouts",
        "milestones",
    )
}
