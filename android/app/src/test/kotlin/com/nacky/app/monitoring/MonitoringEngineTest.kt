package com.nacky.app.monitoring

import com.nacky.app.engine.TokenTrie
import com.nacky.app.engine.PhraseEntry
import com.nacky.app.patterns.Pattern
import com.nacky.app.text.TextNormalizer
import com.nacky.app.text.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringEngineTest {

    private fun norm(s: String) = TextNormalizer.normalize(s)
    private fun toks(norm: String) = Tokenizer.tokenizeUnicode(norm).map { it.text }

    private fun makeTrie(): TokenTrie {
        val patterns = listOf(
            Pattern(id = "p1", category = "cat", severity = "low", tokensOrPhrases = listOf("ass")),
            Pattern(id = "p2", category = "cat", severity = "med", tokensOrPhrases = listOf("reverse cowgirl")),
            Pattern(id = "p3", category = "cat", severity = "low", tokensOrPhrases = listOf("cul")),
        )
        val entries = patterns.flatMap { p ->
            p.tokensOrPhrases.map { raw ->
                val n = norm(raw)
                val t = toks(n)
                PhraseEntry(patternId = p.id, category = p.category, severity = p.severity, tokenSequence = t)
            }
        }
        return TokenTrie.build(entries)
    }

    @Test
    fun thresholdAndCooldown() {
        val trie = makeTrie()
        val countsLog = mutableListOf<Pair<Long, Int>>()
        val src = "com.example.app"
        val engine = MonitoringEngine(
            trie = trie,
            normalizer = ::norm,
            tokenizer = ::toks,
            settings = MonitoringSettings(minOccurrences = 3, windowSeconds = 300, cooldownMs = 1000, debugHook = { patternId, source, count, ts, trig ->
                if (patternId == "p1" && source == src) countsLog.add(ts to count)
            })
        )
        
        var now = 1_000L
    // debug print removed

        // a) Single snapshot with 2 occurrences < minOccurrences  no threshold
        val snapA = "ass !!! middle ass" // exactly two occurrences separated
        val dA = engine.processSnapshot(src, snapA, now)
        assertEquals(0, dA.size)
        // Ensure we observed count progression containing a 1 but not 2
        val maxCountA = countsLog.filter { it.first == now }.maxOfOrNull { it.second } ?: 0
        assertEquals(1, maxCountA)
    }

    @Test
    fun allMatchesMode() {
        val countsLog = mutableListOf<Pair<Long, Int>>()
        val src = "com.example.app"
        val trie = makeTrie()
        val engine = MonitoringEngine(
            trie = trie,
            normalizer = ::norm,
            tokenizer = ::toks,
            settings = MonitoringSettings(minOccurrences = 3, windowSeconds = 300, cooldownMs = 1000, countMode = CountMode.ALL_MATCHES, debugHook = { patternId, source, count, ts, trig ->
                if (patternId == "p1" && source == src) countsLog.add(ts to count)
            })
        )
    var now = 1_000L
    // debug print removed
        val snapA = "ass !!! middle ass" // two matches
        val dA = engine.processSnapshot(src, snapA, now)
    // Should increment count by 2 for two matches (ALL_MATCHES mode)
        val maxCountA = countsLog.filter { it.first == now }.maxOfOrNull { it.second } ?: 0
    assertEquals(2, maxCountA)

            // b) Third occurrence triggers threshold
            now += 100
            val snapB = "ass"
            val dB = engine.processSnapshot(src, snapB, now)
            assertEquals(1, dB.size)

            // c) Inside cooldown → no new detection
            now += 200
            val snapC = "ass"
            val dC = engine.processSnapshot(src, snapC, now)
            assertEquals(0, dC.size)

            // After cooldown -> next detection on additional occurrences reaching threshold again
            // Provide 2 occurrences first (still below) then third to trigger again
            now += 1_200 // past cooldown
            val dD1 = engine.processSnapshot(src, "ass", now)
            assertEquals(0, dD1.size)
            now += 50
            val dD2 = engine.processSnapshot(src, "ass", now)
            assertEquals(0, dD2.size)
            now += 50
    val dD3 = engine.processSnapshot(src, "ass", now)
    // debug print removed
    assertEquals(1, dD3.size)

            // d) Different sourceId independent counters
            val other = "com.other.app"
            now += 50
            val o1 = engine.processSnapshot(other, "ass ass ass", now)
            assertEquals(1, o1.size) // fresh source should trigger immediately with 3 occurrences in single snapshot

            // e) Phrase with punctuation/newline still matches
            val phraseSrc = "com.phrase.app"
            now += 50
            val phraseText = "reverse \n cowgirl" // newline acts as separator but tokens remain consecutive
            val phraseEngine = MonitoringEngine(
                trie = trie,
                normalizer = ::norm,
                tokenizer = ::toks,
                settings = MonitoringSettings(minOccurrences = 1, windowSeconds = 60, cooldownMs = 500)
            )
            val phraseDetections = phraseEngine.processSnapshot(phraseSrc, phraseText, now)
            assertEquals(1, phraseDetections.size)
            assertEquals("p2", phraseDetections[0].patternId)
    }

    // Duplicate allMatchesMode test removed. Only one properly annotated function remains.
}
