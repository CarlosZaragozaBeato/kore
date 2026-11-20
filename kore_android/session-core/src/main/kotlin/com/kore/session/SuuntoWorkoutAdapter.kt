package com.kore.session

import java.time.Instant
import java.time.ZoneOffset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object SuuntoWorkoutAdapter {
    private val json = Json { ignoreUnknownKeys = true }

    fun parsePage(text: String, importedAt: Instant = Instant.now()): List<ImportedActivity> {
        val root = try {
            json.parseToJsonElement(text).jsonObject
        } catch (error: Exception) {
            throw SessionFormatException("Suunto response is not valid JSON", error)
        }
        root.textValue("error")?.let { throw SessionFormatException("Suunto returned an API error") }
        val payload = root["payload"] as? JsonArray
            ?: throw SessionFormatException("Suunto response has no workout payload")
        return payload.map { element ->
            val workout = element as? JsonObject
                ?: throw SessionFormatException("Suunto workout is not an object")
            workout.toImportedActivity(importedAt)
        }
    }

    private fun JsonObject.toImportedActivity(importedAt: Instant): ImportedActivity {
        val key = textValue("workoutKey")
            ?: throw SessionFormatException("Suunto workout has no workoutKey")
        val startTime = this["startTime"]?.jsonPrimitive?.longOrNull
            ?: throw SessionFormatException("Suunto workout has no startTime")
        val duration = this["totalTime"]?.jsonPrimitive?.doubleOrNull
            ?: throw SessionFormatException("Suunto workout has no totalTime")
        val activityId = this["activityId"]?.jsonPrimitive?.intOrNull
        val offsetMinutes = this["timeOffsetInMinutes"]?.jsonPrimitive?.intOrNull ?: 0
        val start = Instant.ofEpochMilli(startTime)
        val sport = sportFor(activityId)
        val heartRate = this["hrdata"] as? JsonObject
        return ImportedActivity(
            externalId = key,
            provider = "suunto",
            title = titleFor(sport),
            sport = sport,
            date = start.atOffset(ZoneOffset.ofTotalSeconds(offsetMinutes * 60)).toLocalDate().toString(),
            startedAt = start.toString(),
            durationSeconds = duration.toInt(),
            distanceMeters = this["totalDistance"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            elevationGainMeters = this["totalAscent"]?.jsonPrimitive?.doubleOrNull?.toInt(),
            averageHeartRate = heartRate?.get("workoutAvgHR")?.jsonPrimitive?.doubleOrNull?.toInt()
                ?: heartRate?.get("avg")?.jsonPrimitive?.doubleOrNull?.toInt(),
            importedAt = importedAt.toString(),
        )
    }

    private fun sportFor(activityId: Int?): String = when (activityId) {
        1 -> "running"
        22 -> "trail_running"
        53 -> "treadmill_running"
        59, 103 -> "track_running"
        115 -> "vertical_running"
        0 -> "walking"
        2, 10, 52, 99 -> "cycling"
        11 -> "hiking"
        21 -> "swimming"
        85 -> "open_water_swimming"
        23 -> "strength"
        null -> "unknown"
        else -> "suunto_activity_$activityId"
    }

    private fun titleFor(sport: String): String = when (sport) {
        "running" -> "Carrera Suunto"
        "trail_running" -> "Trail Suunto"
        "treadmill_running" -> "Cinta Suunto"
        "track_running" -> "Pista Suunto"
        "vertical_running" -> "Carrera vertical Suunto"
        "walking" -> "Caminata Suunto"
        "cycling" -> "Ciclismo Suunto"
        "hiking" -> "Senderismo Suunto"
        "swimming", "open_water_swimming" -> "Natación Suunto"
        "strength" -> "Fuerza Suunto"
        else -> "Actividad Suunto"
    }
}
