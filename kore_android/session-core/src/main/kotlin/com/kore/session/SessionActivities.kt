package com.kore.session

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ActivityInput(
    val title: String,
    val sport: String,
    val date: String,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val elevationGainMeters: Int? = null,
    val averageHeartRate: Int? = null,
)

data class RunnerActivity(
    val id: String,
    val title: String,
    val sport: String,
    val date: String,
    val startedAt: String,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val elevationGainMeters: Int?,
    val averageHeartRate: Int?,
    val sourceType: String,
)

fun SessionDocument.activities(): List<RunnerActivity> = (root["activities"] as? JsonArray)
    .orEmpty()
    .mapNotNull { (it as? JsonObject)?.toRunnerActivity() }

fun SessionDocument.addActivity(
    input: ActivityInput,
    id: String = UUID.randomUUID().toString(),
    now: Instant = Instant.now(),
): SessionDocument {
    val title = input.title.trim()
    val sport = input.sport.trim().lowercase()
    require(title.isNotEmpty()) { "Activity title is required" }
    require(sport.isNotEmpty()) { "Activity sport is required" }
    runCatching { LocalDate.parse(input.date) }
        .getOrElse { throw IllegalArgumentException("Activity date must use YYYY-MM-DD") }
    require(input.durationSeconds > 0) { "Activity duration must be positive" }
    require(input.distanceMeters >= 0) { "Activity distance cannot be negative" }
    require(input.elevationGainMeters == null || input.elevationGainMeters >= 0) { "Elevation gain cannot be negative" }
    require(input.averageHeartRate == null || input.averageHeartRate in 30..250) { "Average heart rate is invalid" }
    require(activities().none { it.id == id }) { "An activity with id $id already exists" }

    val timestamp = now.toString()
    val activity = JsonObject(buildMap {
        put("id", JsonPrimitive(id))
        put("title", JsonPrimitive(title))
        put("sport", JsonPrimitive(sport))
        put("date", JsonPrimitive(input.date))
        put("startedAt", JsonPrimitive("${input.date}T12:00:00Z"))
        put("durationSeconds", JsonPrimitive(input.durationSeconds))
        put("distanceMeters", JsonPrimitive(input.distanceMeters))
        input.elevationGainMeters?.let { put("elevationGainMeters", JsonPrimitive(it)) }
        input.averageHeartRate?.let { put("averageHeartRate", JsonPrimitive(it)) }
        put("sourceRef", JsonObject(mapOf(
            "type" to JsonPrimitive("manual"),
            "importedAt" to JsonPrimitive(timestamp),
        )))
    })
    val current = root["activities"] as? JsonArray ?: JsonArray(emptyList())
    return withActivities(JsonArray(current + activity), timestamp)
}

private fun JsonObject.toRunnerActivity(): RunnerActivity? {
    return RunnerActivity(
        id = textValue("id") ?: return null,
        title = textValue("title") ?: "Actividad",
        sport = textValue("sport") ?: return null,
        date = textValue("date") ?: textValue("startedAt")?.take(10) ?: return null,
        startedAt = textValue("startedAt") ?: return null,
        durationSeconds = this["durationSeconds"]?.jsonPrimitive?.intOrNull ?: return null,
        distanceMeters = this["distanceMeters"]?.jsonPrimitive?.doubleOrNull ?: return null,
        elevationGainMeters = this["elevationGainMeters"]?.jsonPrimitive?.intOrNull,
        averageHeartRate = this["averageHeartRate"]?.jsonPrimitive?.intOrNull,
        sourceType = (this["sourceRef"] as? JsonObject)?.textValue("type") ?: "unknown",
    )
}

private fun SessionDocument.withActivities(activities: JsonArray, timestamp: String): SessionDocument {
    val profile = JsonObject(root.objectValue("profile").toMutableMap().apply {
        this["updatedAt"] = JsonPrimitive(timestamp)
    })
    return SessionImporter.fromObject(JsonObject(root.toMutableMap().apply {
        this["activities"] = activities
        this["profile"] = profile
    }))
}
