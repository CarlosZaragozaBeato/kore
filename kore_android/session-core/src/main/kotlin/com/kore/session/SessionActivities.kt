package com.kore.session

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
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
    val externalId: String?,
)

data class ImportedActivity(
    val externalId: String,
    val provider: String,
    val title: String,
    val sport: String,
    val date: String,
    val startedAt: String,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val elevationGainMeters: Int? = null,
    val averageHeartRate: Int? = null,
    val importedAt: String,
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

fun SessionDocument.importActivities(imports: List<ImportedActivity>): SessionDocument {
    if (imports.isEmpty()) return this
    val uniqueImports = imports.distinctBy { "${it.provider}:${it.externalId}" }
    uniqueImports.forEach(::validateImport)
    val timestamp = uniqueImports.maxOf { it.importedAt }
    val existing = (root["activities"] as? JsonArray).orEmpty().toMutableList()
    uniqueImports.forEach { input ->
        val index = existing.indexOfFirst { element ->
            val source = (element as? JsonObject)?.get("sourceRef") as? JsonObject
            source?.textValue("provider") == input.provider && source.textValue("externalId") == input.externalId
        }
        val replacement = importedActivityObject(input)
        if (index >= 0) {
            val current = existing[index] as? JsonObject ?: JsonObject(emptyMap())
            existing[index] = JsonObject(current.toMutableMap().apply { putAll(replacement) })
        } else {
            val stableId = "${input.provider}:${input.externalId}"
            require(existing.none { (it as? JsonObject)?.textValue("id") == stableId }) { "Activity id collision for $stableId" }
            existing += JsonObject(replacement.toMutableMap().apply { this["id"] = JsonPrimitive(stableId) })
        }
    }
    return withActivities(JsonArray(existing), timestamp)
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
        sourceType = (this["sourceRef"] as? JsonObject)?.let { it.textValue("provider") ?: it.textValue("type") } ?: "unknown",
        externalId = (this["sourceRef"] as? JsonObject)?.textValue("externalId"),
    )
}

private fun validateImport(input: ImportedActivity) {
    require(input.externalId.isNotBlank()) { "External activity id is required" }
    require(input.provider.isNotBlank()) { "Activity provider is required" }
    require(input.title.isNotBlank()) { "Activity title is required" }
    require(input.sport.isNotBlank()) { "Activity sport is required" }
    runCatching { LocalDate.parse(input.date) }
        .getOrElse { throw IllegalArgumentException("Activity date must use YYYY-MM-DD") }
    runCatching { Instant.parse(input.startedAt) }
        .getOrElse { throw IllegalArgumentException("Activity start must be an instant") }
    require(input.durationSeconds > 0) { "Activity duration must be positive" }
    require(input.distanceMeters >= 0) { "Activity distance cannot be negative" }
    require(input.elevationGainMeters == null || input.elevationGainMeters >= 0) { "Elevation gain cannot be negative" }
    require(input.averageHeartRate == null || input.averageHeartRate in 30..250) { "Average heart rate is invalid" }
    runCatching { Instant.parse(input.importedAt) }
        .getOrElse { throw IllegalArgumentException("Import time must be an instant") }
}

private fun importedActivityObject(input: ImportedActivity): JsonObject = JsonObject(buildMap {
    put("id", JsonPrimitive("${input.provider}:${input.externalId}"))
    put("title", JsonPrimitive(input.title.trim()))
    put("sport", JsonPrimitive(input.sport.trim().lowercase()))
    put("date", JsonPrimitive(input.date))
    put("startedAt", JsonPrimitive(input.startedAt))
    put("durationSeconds", JsonPrimitive(input.durationSeconds))
    put("distanceMeters", JsonPrimitive(input.distanceMeters))
    put("elevationGainMeters", input.elevationGainMeters?.let(::JsonPrimitive) ?: JsonNull)
    put("averageHeartRate", input.averageHeartRate?.let(::JsonPrimitive) ?: JsonNull)
    put("sourceRef", JsonObject(mapOf(
        "provider" to JsonPrimitive(input.provider),
        "externalId" to JsonPrimitive(input.externalId),
        "importedAt" to JsonPrimitive(input.importedAt),
    )))
})

private fun SessionDocument.withActivities(activities: JsonArray, timestamp: String): SessionDocument {
    val profile = JsonObject(root.objectValue("profile").toMutableMap().apply {
        this["updatedAt"] = JsonPrimitive(timestamp)
    })
    return SessionImporter.fromObject(JsonObject(root.toMutableMap().apply {
        this["activities"] = activities
        this["profile"] = profile
    }))
}
