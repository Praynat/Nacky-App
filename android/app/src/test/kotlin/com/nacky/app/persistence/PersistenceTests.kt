package com.nacky.app.persistence

import com.nacky.app.DetectionSettings
import com.nacky.app.patterns.Pattern
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class PersistenceTests {
    private fun tempRoot(): File {
        val base = System.getProperty("java.io.tmpdir")
        val root = File(base, "nacky_persist_${UUID.randomUUID()}")
        root.mkdirs()
        return root.absoluteFile
    }

    @Test
    fun settings_round_trip() {
        val s = DetectionSettings(liveEnabled = true, monitoringEnabled = false, minOccurrences = 5, windowSeconds = 42, cooldownMs = 777, countMode = "ALL_MATCHES", blockHighSeverityOnly = true, debounceMs = 321)
        val json = SettingsStore.serialize(s)
        val loaded = SettingsStore.deserialize(json)!!
        assertEquals(s.normalized(), loaded)
    }

    @Test
    fun patterns_round_trip() {
        val patterns = listOf(
            Pattern("p1", "Adult", "High", listOf("one", "two words")),
            Pattern("p2", "Misc", "Low", listOf("x"))
        )
        val snap = PatternsStore.Snapshot(10, patterns, mapOf("source" to "test"))
        val json = PatternsStore.serialize(snap)
        val decoded = PatternsStore.deserialize(json)!!
        assertEquals(10, decoded.version)
        assertEquals(2, decoded.patterns.size)
        assertEquals(setOf("p1", "p2"), decoded.patterns.map { it.id }.toSet())
        val p1 = decoded.patterns.first { it.id == "p1" }
        assertEquals(listOf("one", "two words"), p1.tokensOrPhrases)
    assertEquals("adult", p1.category.lowercase())
    assertEquals("high", p1.severity.lowercase())
    }

    @Test
    fun corrupted_file_handling() {
        val broken = "{not-json"
        assertNull(PatternsStore.deserialize(broken))
    }

    @Test
    fun failed_write_handling() {
        // Simulate failed write by passing snapshot serialize then corrupting content
        val s = DetectionSettings()
        val json = SettingsStore.serialize(s)
        val corrupted = json.replace("{", "")
        assertNull(SettingsStore.deserialize(corrupted))
    }
}
