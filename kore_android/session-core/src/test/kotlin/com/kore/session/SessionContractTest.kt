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
}
