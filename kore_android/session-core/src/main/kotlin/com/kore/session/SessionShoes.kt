package com.kore.session

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ShoeUsagePhase(
    val fromKm: Double,
    val toKm: Double?,
    val purpose: String,
)

data class ShoeRatings(
    val comfort: Double,
    val response: Double,
    val stability: Double,
    val grip: Double,
    val durability: Double,
) {
    val average: Double
        get() = listOf(comfort, response, stability, grip, durability).average()
}

data class ShoeInput(
    val name: String,
    val type: String,
    val acquiredAt: String,
    val currentKm: Double,
    val limitKm: Double,
    val purchasePrice: Double,
    val currency: String = "EUR",
    val usagePhases: List<ShoeUsagePhase>,
    val ratings: ShoeRatings,
    val notes: String? = null,
)

data class RunnerShoe(
    val id: String,
    val name: String,
    val type: String,
    val acquiredAt: String,
    val currentKm: Double,
    val limitKm: Double,
    val purchasePrice: Double,
    val currency: String,
    val usagePhases: List<ShoeUsagePhase>,
    val ratings: ShoeRatings,
    val notes: String?,
    val status: String,
    val retiredAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

fun SessionDocument.shoes(): List<RunnerShoe> = (root["shoes"] as? JsonArray)
    .orEmpty()
    .mapNotNull { (it as? JsonObject)?.toRunnerShoe() }

fun SessionDocument.addShoe(
    input: ShoeInput,
    id: String = UUID.randomUUID().toString(),
    now: Instant = Instant.now(),
): SessionDocument {
    validateShoe(input)
    require(shoes().none { it.id == id }) { "A shoe with id $id already exists" }
    val timestamp = now.toString()
    val shoe = shoeObject(input, id, "active", null, timestamp, timestamp)
    val current = root["shoes"] as? JsonArray ?: JsonArray(emptyList())
    return withShoes(JsonArray(current + shoe), timestamp)
}

fun SessionDocument.updateShoe(
    id: String,
    input: ShoeInput,
    now: Instant = Instant.now(),
): SessionDocument {
    validateShoe(input)
    val current = root["shoes"] as? JsonArray ?: JsonArray(emptyList())
    var found = false
    val timestamp = now.toString()
    val updated = JsonArray(current.map { element ->
        val existing = element as? JsonObject
        if (existing?.textValue("id") != id) return@map element
        found = true
        val status = existing.textValue("status") ?: "active"
        val replacement = shoeObject(
            input = input,
            id = id,
            status = status,
            retiredAt = existing.textValue("retiredAt"),
            createdAt = existing.textValue("createdAt") ?: timestamp,
            updatedAt = timestamp,
        )
        JsonObject(existing.toMutableMap().apply { putAll(replacement) })
    })
    require(found) { "Shoe $id does not exist" }
    return withShoes(updated, timestamp)
}

fun SessionDocument.retireShoe(
    id: String,
    now: Instant = Instant.now(),
): SessionDocument {
    val current = root["shoes"] as? JsonArray ?: JsonArray(emptyList())
    var found = false
    val timestamp = now.toString()
    val updated = JsonArray(current.map { element ->
        val existing = element as? JsonObject
        if (existing?.textValue("id") != id) return@map element
        found = true
        JsonObject(existing.toMutableMap().apply {
            this["status"] = JsonPrimitive("retired")
            this["retiredAt"] = JsonPrimitive(timestamp)
            this["updatedAt"] = JsonPrimitive(timestamp)
        })
    })
    require(found) { "Shoe $id does not exist" }
    return withShoes(updated, timestamp)
}

private fun validateShoe(input: ShoeInput) {
    require(input.name.isNotBlank()) { "Shoe name is required" }
    require(input.type.isNotBlank()) { "Shoe type is required" }
    runCatching { LocalDate.parse(input.acquiredAt) }
        .getOrElse { throw IllegalArgumentException("Acquisition date must use YYYY-MM-DD") }
    require(input.currentKm >= 0) { "Current distance cannot be negative" }
    require(input.limitKm > 0) { "Distance limit must be positive" }
    require(input.purchasePrice >= 0) { "Purchase price cannot be negative" }
    require(input.currency.trim().isNotEmpty()) { "Currency is required" }
    input.usagePhases.forEach { phase ->
        require(phase.fromKm >= 0) { "Usage phase start cannot be negative" }
        require(phase.toKm == null || phase.toKm > phase.fromKm) { "Usage phase range is invalid" }
        require(phase.purpose.isNotBlank()) { "Usage phase purpose is required" }
    }
    listOf(
        input.ratings.comfort,
        input.ratings.response,
        input.ratings.stability,
        input.ratings.grip,
        input.ratings.durability,
    ).forEach { require(it in 1.0..5.0) { "Ratings must be between 1 and 5" } }
}

private fun shoeObject(
    input: ShoeInput,
    id: String,
    status: String,
    retiredAt: String?,
    createdAt: String,
    updatedAt: String,
): JsonObject = JsonObject(buildMap {
    put("id", JsonPrimitive(id))
    put("name", JsonPrimitive(input.name.trim()))
    put("type", JsonPrimitive(input.type.trim().lowercase()))
    put("acquiredAt", JsonPrimitive(input.acquiredAt))
    put("currentKm", JsonPrimitive(input.currentKm))
    put("limitKm", JsonPrimitive(input.limitKm))
    put("purchasePrice", JsonPrimitive(input.purchasePrice))
    put("currency", JsonPrimitive(input.currency.trim().uppercase()))
    put("usagePhases", JsonArray(input.usagePhases.map { phase ->
        JsonObject(buildMap {
            put("fromKm", JsonPrimitive(phase.fromKm))
            phase.toKm?.let { put("toKm", JsonPrimitive(it)) }
            put("purpose", JsonPrimitive(phase.purpose.trim()))
        })
    }))
    put("ratings", JsonObject(mapOf(
        "comfort" to JsonPrimitive(input.ratings.comfort),
        "response" to JsonPrimitive(input.ratings.response),
        "stability" to JsonPrimitive(input.ratings.stability),
        "grip" to JsonPrimitive(input.ratings.grip),
        "durability" to JsonPrimitive(input.ratings.durability),
    )))
    put(
        "notes",
        input.notes?.trim()?.takeIf(String::isNotEmpty)?.let(::JsonPrimitive) ?: JsonNull,
    )
    put("status", JsonPrimitive(status))
    retiredAt?.let { put("retiredAt", JsonPrimitive(it)) }
    put("createdAt", JsonPrimitive(createdAt))
    put("updatedAt", JsonPrimitive(updatedAt))
    put("sourceRef", JsonObject(mapOf("type" to JsonPrimitive("manual"))))
})

private fun JsonObject.toRunnerShoe(): RunnerShoe? {
    val ratingsObject = this["ratings"] as? JsonObject ?: return null
    val ratings = ShoeRatings(
        comfort = ratingsObject.number("comfort") ?: return null,
        response = ratingsObject.number("response") ?: return null,
        stability = ratingsObject.number("stability") ?: return null,
        grip = ratingsObject.number("grip") ?: return null,
        durability = ratingsObject.number("durability") ?: return null,
    )
    val phases = (this["usagePhases"] as? JsonArray).orEmpty().mapNotNull { element ->
        val phase = element as? JsonObject ?: return@mapNotNull null
        ShoeUsagePhase(
            fromKm = phase.number("fromKm") ?: return@mapNotNull null,
            toKm = phase.number("toKm"),
            purpose = phase.textValue("purpose") ?: return@mapNotNull null,
        )
    }
    return RunnerShoe(
        id = textValue("id") ?: return null,
        name = textValue("name") ?: return null,
        type = textValue("type") ?: return null,
        acquiredAt = textValue("acquiredAt") ?: return null,
        currentKm = number("currentKm") ?: return null,
        limitKm = number("limitKm") ?: return null,
        purchasePrice = number("purchasePrice") ?: return null,
        currency = textValue("currency") ?: "EUR",
        usagePhases = phases,
        ratings = ratings,
        notes = textValue("notes"),
        status = textValue("status") ?: "active",
        retiredAt = textValue("retiredAt"),
        createdAt = textValue("createdAt").orEmpty(),
        updatedAt = textValue("updatedAt").orEmpty(),
    )
}

private fun JsonObject.number(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull

private fun SessionDocument.withShoes(shoes: JsonArray, timestamp: String): SessionDocument {
    val profile = JsonObject(root.objectValue("profile").toMutableMap().apply {
        this["updatedAt"] = JsonPrimitive(timestamp)
    })
    return SessionImporter.fromObject(JsonObject(root.toMutableMap().apply {
        this["shoes"] = shoes
        this["profile"] = profile
    }))
}
