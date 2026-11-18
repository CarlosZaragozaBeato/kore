package com.kore.session

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
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
                    "activeGoal" to JsonNull,
                    "goalHistory" to JsonArray(emptyList()),
                    "activePlan" to JsonNull,
                    "planArchive" to JsonArray(emptyList()),
                    "workouts" to JsonArray(emptyList()),
                    "personalRecords" to JsonArray(emptyList()),
                    "shoes" to JsonArray(emptyList()),
                    "diagnostics" to JsonArray(emptyList()),
                    "ratings" to JsonObject(
                        mapOf(
                            "current" to JsonNull,
                            "history" to JsonArray(emptyList()),
                        ),
                    ),
                    "pinnedWorkouts" to JsonArray(emptyList()),
                    "milestones" to JsonArray(emptyList()),
                    "sync" to JsonObject(
                        mapOf(
                            "suunto" to JsonObject(
                                mapOf(
                                    "enabled" to JsonPrimitive(false),
                                    "userId" to JsonNull,
                                    "cursor" to JsonNull,
                                    "lastAttemptAt" to JsonNull,
                                    "lastSuccessAt" to JsonNull,
                                ),
                            ),
                        ),
                    ),
                    "settings" to JsonObject(emptyMap()),
                ),
            ),
        )
    }
}
