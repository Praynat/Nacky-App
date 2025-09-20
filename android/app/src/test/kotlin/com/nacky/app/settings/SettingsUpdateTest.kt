package com.nacky.app.settings

import com.nacky.app.monitoring.*
import org.junit.Assert.*
import org.junit.Test

class SettingsUpdateTest {
    @Test
    fun monitoringSettingsUpdateApplies() {
        val engine = MonitoringEngine(
            trie = com.nacky.app.engine.TokenTrie.build(emptyList()),
            normalizer = { it },
            tokenizer = { emptyList() },
            settings = MonitoringSettings(minOccurrences = 3, windowSeconds = 300, cooldownMs = 5000, countMode = CountMode.UNIQUE_PER_SNAPSHOT)
        )
        assertEquals(3, engine.settings.minOccurrences)
        engine.updateSettings(MonitoringSettings(minOccurrences = 1, windowSeconds = 10, cooldownMs = 100, countMode = CountMode.ALL_MATCHES))
        assertEquals(1, engine.settings.minOccurrences)
        assertEquals(10, engine.settings.windowSeconds)
        assertEquals(100, engine.settings.cooldownMs)
        assertEquals(CountMode.ALL_MATCHES, engine.settings.countMode)
    }
}
