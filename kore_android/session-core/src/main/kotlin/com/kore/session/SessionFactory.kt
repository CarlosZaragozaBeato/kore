package com.kore.session

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object SessionFactory {
    fun create(label: String, now: Instant = Instant.now()): SessionDocument {
        val normalizedLabel = label.trim()
        require(normalizedLabel.isNotEmpty()) { "Session name is required" }
        val timestamp = now.toString()
        return SessionImporter.fromObject(
            JsonObject(
                mapOf(
                    "koreSession" to JsonPrimitive(CURRENT_SESSION_VERSION),
                    "profile" to JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(UUID.randomUUID().toString()),
                            "label" to JsonPrimitive(normalizedLabel),
                            "createdAt" to JsonPrimitive(timestamp),
                            "updatedAt" to JsonPrimitive(timestamp),
                        ),
                    ),
                    "activities" to JsonArray(emptyList()),
                    "museum" to JsonArray(emptyList()),
                    "personalBests" to JsonArray(emptyList()),
                    "shoes" to JsonArray(emptyList()),
                    "routes" to JsonArray(emptyList()),
                    "sources" to JsonObject(
                        mapOf(
                            "connections" to JsonArray(emptyList()),
                        ),
                    ),
                    "settings" to JsonObject(emptyMap()),
                ),
            ),
        )
    }
}
