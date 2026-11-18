package com.kore.session

import java.io.File
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class SessionContractTest {
    @Test
    fun `factory creates a complete empty session without invented fitness`() {
        val session = SessionFactory.create("Carlos", Instant.parse("2026-11-18T10:00:00Z"))

        assertEquals(1, session.version)
        assertEquals("Carlos", session.profileLabel)
        assertNull(session.activeGoalLabel)
        assertNull(session.activePlanLabel)
        assertFalse(session.suuntoConnected)
        assertEquals(0, session.inventory().workouts)
        assertEquals(0, session.inventory().archivedPlans)
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
