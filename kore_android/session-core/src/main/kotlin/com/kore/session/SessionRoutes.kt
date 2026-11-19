package com.kore.session

import java.time.Instant
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class RouteInput(
    val name: String,
    val distanceKm: Double,
    val location: String,
    val surface: String,
    val elevationGainMeters: Int? = null,
    val notes: String? = null,
)

data class RunnerRoute(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val location: String,
    val surface: String,
    val elevationGainMeters: Int?,
    val notes: String?,
    val createdAt: String,
    val sourceType: String,
)

fun SessionDocument.routes(): List<RunnerRoute> = (root["routes"] as? JsonArray)
    .orEmpty()
    .mapNotNull { element ->
        val route = element as? JsonObject ?: return@mapNotNull null
        val id = route.textValue("id") ?: return@mapNotNull null
        val name = route.textValue("name") ?: return@mapNotNull null
        val distance = route["distanceKm"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        RunnerRoute(
            id = id,
            name = name,
            distanceKm = distance,
            location = route.textValue("location").orEmpty(),
            surface = route.textValue("surface") ?: "unknown",
            elevationGainMeters = route["elevationGainMeters"]?.jsonPrimitive?.intOrNull,
            notes = route.textValue("notes"),
            createdAt = route.textValue("createdAt").orEmpty(),
            sourceType = (route["sourceRef"] as? JsonObject)?.textValue("type") ?: "manual",
        )
    }

fun SessionDocument.addRoute(
    input: RouteInput,
    id: String = UUID.randomUUID().toString(),
    now: Instant = Instant.now(),
): SessionDocument {
    val name = input.name.trim()
    val location = input.location.trim()
    val surface = input.surface.trim().lowercase()
    require(name.isNotEmpty()) { "Route name is required" }
    require(input.distanceKm > 0.0) { "Route distance must be positive" }
    require(location.isNotEmpty()) { "Route location is required" }
    require(surface.isNotEmpty()) { "Route surface is required" }
    require(input.elevationGainMeters == null || input.elevationGainMeters >= 0) {
        "Elevation gain cannot be negative"
    }
    require(routes().none { it.id == id }) { "A route with id $id already exists" }

    val timestamp = now.toString()
    val route = JsonObject(
        buildMap {
            put("id", JsonPrimitive(id))
            put("name", JsonPrimitive(name))
            put("distanceKm", JsonPrimitive(input.distanceKm))
            put("location", JsonPrimitive(location))
            put("surface", JsonPrimitive(surface))
            input.elevationGainMeters?.let { put("elevationGainMeters", JsonPrimitive(it)) }
            input.notes?.trim()?.takeIf(String::isNotEmpty)?.let { put("notes", JsonPrimitive(it)) }
            put("createdAt", JsonPrimitive(timestamp))
            put("sourceRef", JsonObject(mapOf("type" to JsonPrimitive("manual"))))
        },
    )
    val currentRoutes = root["routes"] as? JsonArray ?: JsonArray(emptyList())
    val updatedProfile = JsonObject(root.objectValue("profile").toMutableMap().apply {
        this["updatedAt"] = JsonPrimitive(timestamp)
    })
    return SessionImporter.fromObject(JsonObject(root.toMutableMap().apply {
        this["routes"] = JsonArray(currentRoutes + route)
        this["profile"] = updatedProfile
    }))
}
