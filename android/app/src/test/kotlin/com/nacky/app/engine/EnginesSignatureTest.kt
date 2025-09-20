package com.nacky.app.engine

import com.nacky.app.patterns.PatternRepository
import com.nacky.app.DetectionSettings
import com.nacky.app.DetectionSettingsStore
import com.nacky.app.Engines
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests signature-based rebuild skip logic in Engines. */
class EnginesSignatureTest {

    @Test
    fun `second signature identical - should not rebuild`() {
        // Seed patterns
        val payload = mapOf(
            "version" to 1,
            "patterns" to listOf(
                mapOf(
                    "id" to "p1",
                    "category" to "adult",
                    "severity" to "high",
                    "tokensOrPhrases" to listOf("bad", "very bad")
                ),
                mapOf(
                    "id" to "p2",
                    "category" to "adult",
                    "severity" to "low",
                    "tokensOrPhrases" to listOf("soft")
                )
            ),
            "meta" to mapOf("ts" to System.currentTimeMillis().toString())
        )
        PatternRepository.updateFromPayload(payload)
        val settings = DetectionSettings(liveEnabled = true, monitoringEnabled = true)
        DetectionSettingsStore.current.set(settings)

        val sig1 = Engines.computeSignature(settings)
        assertTrue("First call should rebuild", Engines.shouldRebuild(sig1))

        // Simulate only a timestamp meta change (non-signature field) by updating repo with same patterns different meta
        val payload2 = payload.toMutableMap()
        payload2["meta"] = mapOf("ts" to (System.currentTimeMillis() + 10_000).toString())
        PatternRepository.updateFromPayload(payload2)

        val sig2 = Engines.computeSignature(settings)
        assertFalse("Signature unchanged, should not rebuild again", Engines.shouldRebuild(sig2))
    }
}
