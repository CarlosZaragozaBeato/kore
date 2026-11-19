package com.kore.session

import java.io.File
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

class SessionContractTest {
    @Test
    fun `factory creates an empty runner archive without provider coupling`() {
        val session = SessionFactory.create("Carlos", Instant.parse("2026-11-18T10:00:00Z"))

        assertEquals(2, session.version)
        assertEquals("Carlos", session.profileLabel)
        assertEquals(0, session.inventory().activities)
        assertEquals(0, session.inventory().museumEntries)
        assertEquals(0, session.inventory().personalBests)
        assertEquals(0, session.inventory().shoes)
        assertEquals(0, session.inventory().routes)
        assertEquals(0, session.inventory().sources)
    }

    @Test
    fun `round trip preserves unknown data`() {
        val original = SessionFactory.create("Carlos")
        val extended = JsonObject(
            original.root.toMutableMap().apply {
                this["futureFeature"] = JsonObject(
                    mapOf("nested" to JsonArray(listOf(JsonPrimitive("keep")))),
                )
            },
        )

        val restored = SessionImporter.fromJson(SessionDocument(extended).toJson())

        assertEquals(extended, restored.root)
        assertTrue("futureFeature" in restored.inventory().unknownTopLevelKeys)
    }

    @Test
    fun `future sessions are rejected before persistence`() {
        val current = SessionFactory.create("Carlos")
        val future = JsonObject(
            current.root.toMutableMap().apply {
                this["koreSession"] = JsonPrimitive(99)
            },
        )

        assertFailsWith<SessionFormatException> {
            SessionImporter.fromObject(future)
        }
    }

    @Test
    fun `version one is migrated without filtering legacy or unknown data`() {
        val current = SessionFactory.create("Carlos")
        val legacy = JsonObject(
            current.root.toMutableMap().apply {
                this["koreSession"] = JsonPrimitive(1)
                this.remove("activities")
                this.remove("museum")
                this.remove("personalBests")
                this.remove("sources")
                this["workouts"] = JsonArray(listOf(JsonObject(mapOf("id" to JsonPrimitive("old")))))
                this["legacyPlan"] = JsonPrimitive("keep")
            },
        )

        val migrated = SessionImporter.fromObject(legacy)

        assertEquals(2, migrated.version)
        assertEquals(1, migrated.inventory().activities)
        assertEquals("keep", migrated.root["legacyPlan"]?.jsonPrimitive?.content)
    }

    @Test
    fun `atomic store writes a readable session`() {
        val directory = createTempDirectory("kore-session-").toFile()
        try {
            val file = File(directory, "nested/active.kore.json")
            SessionFileStore.writeAtomically(file, SessionFactory.create("Carlos"))
            assertEquals("Carlos", SessionFileStore.read(file).profileLabel)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `adding a route persists normalized data and preserves unknown fields`() {
        val original = SessionFactory.create("Carlos")
        val withUnknown = SessionDocument(JsonObject(original.root + ("future" to JsonPrimitive("keep"))))

        val updated = withUnknown.addRoute(
            RouteInput(
                name = "  Vuelta al Retiro  ",
                distanceKm = 8.4,
                location = "Madrid",
                surface = "Asfalto",
                elevationGainMeters = 74,
                notes = "Ruta rápida",
            ),
            id = "route-1",
            now = Instant.parse("2026-11-19T10:00:00Z"),
        )

        val route = updated.routes().single()
        assertEquals("Vuelta al Retiro", route.name)
        assertEquals("asfalto", route.surface)
        assertEquals(74, route.elevationGainMeters)
        assertEquals("keep", updated.root["future"]?.jsonPrimitive?.content)
        assertEquals(1, updated.inventory().routes)
    }

    @Test
    fun `shoe lifecycle preserves future fields and moves it to history`() {
        val original = SessionFactory.create("Carlos")
        val input = ShoeInput(
            name = "  Nike Vaporfly 3 ",
            type = "Competición",
            acquiredAt = "2026-11-19",
            currentKm = 12.5,
            limitKm = 350.0,
            purchasePrice = 259.99,
            usagePhases = listOf(
                ShoeUsagePhase(0.0, 200.0, "Series y competición"),
                ShoeUsagePhase(200.0, null, "Rodajes rápidos"),
            ),
            ratings = ShoeRatings(4.5, 5.0, 4.0, 4.5, 4.0),
        )
        val added = original.addShoe(input, "shoe-1", Instant.parse("2026-11-19T10:00:00Z"))
        val withFutureField = SessionDocument(JsonObject(added.root.toMutableMap().apply {
            val shoe = (this["shoes"] as JsonArray).single() as JsonObject
            this["shoes"] = JsonArray(listOf(JsonObject(shoe + ("futureShoeField" to JsonPrimitive("keep")))))
        }))

        val edited = withFutureField.updateShoe(
            "shoe-1",
            input.copy(currentKm = 42.0),
            Instant.parse("2026-11-20T10:00:00Z"),
        )
        val retired = edited.retireShoe("shoe-1", Instant.parse("2026-11-21T10:00:00Z"))

        assertEquals(42.0, retired.shoes().single().currentKm)
        assertEquals("retired", retired.shoes().single().status)
        assertEquals("keep", ((retired.root["shoes"] as JsonArray).single() as JsonObject)["futureShoeField"]?.jsonPrimitive?.content)
        assertEquals(1, retired.inventory().shoes)
    }
}
