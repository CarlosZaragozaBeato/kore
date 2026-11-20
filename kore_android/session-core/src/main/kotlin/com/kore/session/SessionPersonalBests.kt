package com.kore.session

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private val EVIDENCE_TYPES = setOf("official_race", "training", "estimate", "manual")

data class PersonalBestInput(
    val distanceMeters: Double,
    val durationMillis: Long,
    val achievedAt: String,
    val evidenceType: String,
    val activityId: String? = null,
    val museumEntryId: String? = null,
    val notes: String? = null,
)

data class RunnerPersonalBest(
    val id: String,
    val distanceMeters: Double,
    val durationMillis: Long,
    val achievedAt: String,
    val evidenceType: String,
    val activityId: String?,
    val museumEntryId: String?,
    val notes: String?,
    val createdAt: String,
)

fun SessionDocument.personalBests(): List<RunnerPersonalBest> = (root["personalBests"] as? JsonArray)
    .orEmpty()
    .mapNotNull { (it as? JsonObject)?.toRunnerPersonalBest() }

fun SessionDocument.currentPersonalBests(): List<RunnerPersonalBest> = personalBests()
    .groupBy { it.distanceMeters }
    .values
    .mapNotNull { records -> records.minByOrNull { it.durationMillis } }
    .sortedBy { it.distanceMeters }

fun SessionDocument.addPersonalBest(
    input: PersonalBestInput,
    id: String = UUID.randomUUID().toString(),
    now: Instant = Instant.now(),
): SessionDocument {
    val evidence = input.evidenceType.trim().lowercase()
    require(input.distanceMeters > 0) { "Record distance must be positive" }
    require(input.durationMillis > 0) { "Record duration must be positive" }
    runCatching { LocalDate.parse(input.achievedAt) }
        .getOrElse { throw IllegalArgumentException("Record date must use YYYY-MM-DD") }
    require(evidence in EVIDENCE_TYPES) { "Unsupported evidence type" }
    val activity = input.activityId?.let { activityId ->
        activities().find { it.id == activityId }
            ?: throw IllegalArgumentException("Activity $activityId does not exist")
    }
    val museumEntry = input.museumEntryId?.let { museumId ->
        museumEntries().find { it.id == museumId }
            ?: throw IllegalArgumentException("Museum entry $museumId does not exist")
    }
    require(activity == null || museumEntry == null || museumEntry.activityId == activity.id) {
        "Activity and museum evidence must refer to the same session"
    }
    require(personalBests().none {
        it.distanceMeters == input.distanceMeters && it.durationMillis == input.durationMillis && it.achievedAt == input.achievedAt
    }) { "This personal best already exists" }

    val timestamp = now.toString()
    val record = JsonObject(buildMap {
        put("id", JsonPrimitive(id))
        put("distanceMeters", JsonPrimitive(input.distanceMeters))
        put("durationMillis", JsonPrimitive(input.durationMillis))
        put("achievedAt", JsonPrimitive(input.achievedAt))
        put("evidenceType", JsonPrimitive(evidence))
        input.activityId?.let { put("activityId", JsonPrimitive(it)) }
        input.museumEntryId?.let { put("museumEntryId", JsonPrimitive(it)) }
        input.notes?.trim()?.takeIf(String::isNotEmpty)?.let { put("notes", JsonPrimitive(it)) }
        put("createdAt", JsonPrimitive(timestamp))
    })
    val current = root["personalBests"] as? JsonArray ?: JsonArray(emptyList())
    val museum = linkMuseumEntry(root["museum"] as? JsonArray ?: JsonArray(emptyList()), input.museumEntryId, id)
    val profile = JsonObject(root.objectValue("profile").toMutableMap().apply { this["updatedAt"] = JsonPrimitive(timestamp) })
    return SessionImporter.fromObject(JsonObject(root.toMutableMap().apply {
        this["personalBests"] = JsonArray(current + record)
        this["museum"] = museum
        this["profile"] = profile
    }))
}

private fun JsonObject.toRunnerPersonalBest(): RunnerPersonalBest? {
    return RunnerPersonalBest(
        id = textValue("id") ?: return null,
        distanceMeters = this["distanceMeters"]?.jsonPrimitive?.doubleOrNull ?: return null,
        durationMillis = this["durationMillis"]?.jsonPrimitive?.longOrNull ?: return null,
        achievedAt = textValue("achievedAt") ?: return null,
        evidenceType = textValue("evidenceType") ?: "manual",
        activityId = textValue("activityId"),
        museumEntryId = textValue("museumEntryId"),
        notes = textValue("notes"),
        createdAt = textValue("createdAt").orEmpty(),
    )
}

private fun linkMuseumEntry(museum: JsonArray, museumEntryId: String?, personalBestId: String): JsonArray {
    if (museumEntryId == null) return museum
    return JsonArray(museum.map { element ->
        val entry = element as? JsonObject
        if (entry?.textValue("id") != museumEntryId) return@map element
        val currentIds = entry["personalBestIds"] as? JsonArray ?: JsonArray(emptyList())
        JsonObject(entry.toMutableMap().apply {
            this["personalBestIds"] = JsonArray(currentIds + JsonPrimitive(personalBestId))
        })
    })
}
