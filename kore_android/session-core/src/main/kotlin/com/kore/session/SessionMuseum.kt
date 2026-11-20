package com.kore.session

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class MuseumEntryInput(
    val activityId: String,
    val title: String,
    val motive: String,
    val story: String,
    val tags: List<String>,
    val shoeId: String? = null,
)

data class MuseumEntry(
    val id: String,
    val activityId: String,
    val title: String,
    val motive: String,
    val story: String,
    val tags: List<String>,
    val shoeId: String?,
    val date: String,
    val distanceMeters: Double,
    val durationSeconds: Int,
    val sport: String,
    val createdAt: String,
)

fun SessionDocument.museumEntries(): List<MuseumEntry> = (root["museum"] as? JsonArray)
    .orEmpty()
    .mapNotNull { (it as? JsonObject)?.toMuseumEntry() }

fun SessionDocument.addMuseumEntry(
    input: MuseumEntryInput,
    id: String = UUID.randomUUID().toString(),
    now: Instant = Instant.now(),
): SessionDocument {
    val activity = activities().find { it.id == input.activityId }
        ?: throw IllegalArgumentException("Activity ${input.activityId} does not exist")
    val title = input.title.trim()
    val motive = input.motive.trim()
    val story = input.story.trim()
    require(title.isNotEmpty()) { "Museum title is required" }
    require(motive.isNotEmpty()) { "Museum motive is required" }
    require(story.isNotEmpty()) { "Museum story is required" }
    require(museumEntries().none { it.activityId == input.activityId }) { "This activity is already in the museum" }
    input.shoeId?.let { shoeId -> require(shoes().any { it.id == shoeId }) { "Shoe $shoeId does not exist" } }
    val tags = input.tags.map(String::trim).filter(String::isNotEmpty).distinct()
    val timestamp = now.toString()
    val entry = JsonObject(buildMap {
        put("id", JsonPrimitive(id))
        put("activityId", JsonPrimitive(activity.id))
        put("title", JsonPrimitive(title))
        put("motive", JsonPrimitive(motive))
        put("story", JsonPrimitive(story))
        put("tags", JsonArray(tags.map(::JsonPrimitive)))
        input.shoeId?.let { put("shoeIds", JsonArray(listOf(JsonPrimitive(it)))) }
        put("snapshot", JsonObject(buildMap {
            put("date", JsonPrimitive(activity.date))
            put("sport", JsonPrimitive(activity.sport))
            put("distanceMeters", JsonPrimitive(activity.distanceMeters))
            put("durationSeconds", JsonPrimitive(activity.durationSeconds))
            activity.elevationGainMeters?.let { put("elevationGainMeters", JsonPrimitive(it)) }
            activity.averageHeartRate?.let { put("averageHeartRate", JsonPrimitive(it)) }
        }))
        put("createdAt", JsonPrimitive(timestamp))
    })
    val current = root["museum"] as? JsonArray ?: JsonArray(emptyList())
    val profile = JsonObject(root.objectValue("profile").toMutableMap().apply { this["updatedAt"] = JsonPrimitive(timestamp) })
    return SessionImporter.fromObject(JsonObject(root.toMutableMap().apply {
        this["museum"] = JsonArray(current + entry)
        this["profile"] = profile
    }))
}

private fun JsonObject.toMuseumEntry(): MuseumEntry? {
    val snapshot = this["snapshot"] as? JsonObject ?: return null
    return MuseumEntry(
        id = textValue("id") ?: return null,
        activityId = textValue("activityId") ?: return null,
        title = textValue("title") ?: return null,
        motive = textValue("motive") ?: return null,
        story = textValue("story") ?: return null,
        tags = (this["tags"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content },
        shoeId = ((this["shoeIds"] as? JsonArray)?.firstOrNull() as? JsonPrimitive)?.content,
        date = snapshot.textValue("date") ?: return null,
        distanceMeters = snapshot["distanceMeters"]?.jsonPrimitive?.doubleOrNull ?: return null,
        durationSeconds = snapshot["durationSeconds"]?.jsonPrimitive?.intOrNull ?: return null,
        sport = snapshot.textValue("sport") ?: return null,
        createdAt = textValue("createdAt").orEmpty(),
    )
}
