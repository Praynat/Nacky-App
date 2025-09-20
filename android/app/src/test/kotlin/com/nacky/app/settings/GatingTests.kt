package com.nacky.app.settings

import com.nacky.app.monitoring.*
import com.nacky.app.live.*
import com.nacky.app.lexicon.Lexicon
import com.nacky.app.lexicon.TrieNode
import com.nacky.app.engine.*
import org.junit.Assert.*
import org.junit.Test

class GatingTests {
    @Test
    fun monitoringMinOccurrencesOneTriggers() {
        val trie = TokenTrie.build(listOf(
            PhraseEntry(patternId = "p1", category = "cat", severity = "low", tokenSequence = listOf("ass"))
        ))
        val engine = MonitoringEngine(
            trie = trie,
            normalizer = { it },
            tokenizer = { it.split(" ") },
            settings = MonitoringSettings(minOccurrences = 1, windowSeconds = 60, cooldownMs = 1000, countMode = CountMode.UNIQUE_PER_SNAPSHOT)
        )
        val det = engine.processSnapshot("src", "ass", nowMs = 1000L)
        assertEquals(1, det.size)
        assertEquals("p1", det[0].patternId)
    }

    @Test
    fun liveUpdateSettingsAppliesBlockHighSeverityOnly() {
        val trie = TokenTrie.build(listOf(
            PhraseEntry(patternId = "p1", category = "cat", severity = "low", tokenSequence = listOf("ass")),
            PhraseEntry(patternId = "p2", category = "cat", severity = "high", tokenSequence = listOf("cul"))
        ))
        val step2 = Step2Engine(
            terms = listOf(
                TermEntry(patternId = "p1", term = "ass", category = "cat", severity = "low"),
                TermEntry(patternId = "p2", term = "cul", category = "cat", severity = "high")
            ),
            settings = Step2Settings()
        )
    val emptyLexicon = Lexicon(words = emptySet(), trie = TrieNode())
    val step3 = Step3Engine(lexicon = emptyLexicon, cfg = Step3Config())
        val detector = LiveTypingDetector(trie, step2, step3, LiveSettings(blockHighSeverityOnly = true))
        // low severity should ALLOW when high-only blocking
        val decisionsLow = detector.finalizeForTest("ass")
        assertTrue(decisionsLow.any { it.patternId == "p1" && it.action == Action.ALLOW })
        // high severity blocks
        val decisionsHigh = detector.finalizeForTest("cul")
        assertTrue(decisionsHigh.any { it.patternId == "p2" && it.action == Action.BLOCK })
        // Turn off high-only mode => low severity now BLOCK
        detector.updateSettings(LiveSettings(blockHighSeverityOnly = false))
        val decisionsLow2 = detector.finalizeForTest("ass")
        assertTrue(decisionsLow2.any { it.patternId == "p1" && it.action == Action.BLOCK })
    }
}
