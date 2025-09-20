package com.nacky.app.engine

import com.nacky.app.patterns.Pattern
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.text.TextNormalizer
import com.nacky.app.text.Tokenizer
import org.junit.Assert.*
import org.junit.Test

class Step2EngineTest {

    private fun norm(s: String) = TextNormalizer.normalize(s)
    private fun toks(n: String) = Tokenizer.tokenizeUnicode(n).map { it.text }

    private fun buildEngineWithDefaultPatterns(): Step2Engine {
        // Inject patterns directly into repository for test isolation
        val patterns = listOf(
            Pattern(id = "p1", category = "cat", severity = "low", tokensOrPhrases = listOf("ass")),
            Pattern(id = "p2", category = "cat", severity = "med", tokensOrPhrases = listOf("cul")),
            Pattern(id = "p3", category = "cat", severity = "high", tokensOrPhrases = listOf("reverse cowgirl")),
        )
        // Bypass updateFromPayload; directly set via reflection-like approach not available – instead emulate repo API by calling private fields? Simpler: Build terms from patterns variable itself.
        val terms = patterns.flatMap { p ->
            p.tokensOrPhrases.mapNotNull { raw ->
                val n = norm(raw)
                val t = toks(n)
                if (t.size == 1) TermEntry(patternId = p.id, term = t[0], category = p.category, severity = p.severity) else null
            }
        }
        return Step2Engine(terms, Step2Settings())
    }

    @Test
    fun spacedLettersDetectAss() {
        val engine = buildEngineWithDefaultPatterns()
        val original = norm("a s s")
        val cands = engine.findCandidates(original)
        val ass = cands.firstOrNull { it.term == "ass" }
        assertNotNull(ass)
        val candidate = ass!!
        // mapping correctness
        assertEquals(original[candidate.startOrig], 'a')
        assertEquals(original[candidate.endOrig], 's')
    }

    @Test
    fun symbolSplitDetectLeet() {
        val engine = buildEngineWithDefaultPatterns()
        val original = norm("a$$")
        val cands = engine.findCandidates(original)
        assertTrue(cands.any { it.term == "ass" })
        val cand = cands.first { it.term == "ass" }
        assertEquals(0, cand.startOrig)
        assertEquals(2, cand.endOrig)
    }

    @Test
    fun hyphenUnderscoreCul() {
        val engine = buildEngineWithDefaultPatterns()
        val hyphen = norm("c-u-l")
        val underscore = norm("c_u_l")
        val ch = engine.findCandidates(hyphen)
        val cu = engine.findCandidates(underscore)
        assertTrue(ch.any { it.term == "cul" })
        assertTrue(cu.any { it.term == "cul" })
    }

    @Test
    fun zeroWidthJoiners() {
        val engine = buildEngineWithDefaultPatterns()
        val original = norm("a\u200B s\u200D s")
        val cands = engine.findCandidates(original)
        assertTrue(cands.any { it.term == "ass" })
    }

    @Test
    fun pureNumbersNoLeet() {
        val engine = buildEngineWithDefaultPatterns()
        val original = norm("123 303")
        val cands = engine.findCandidates(original)
        assertTrue(cands.none { it.term == "ass" })
        assertTrue(cands.none { it.term == "cul" })
    }

    @Test
    fun nonLatinNoLeet() {
        val engine = buildEngineWithDefaultPatterns()
        val original = norm("שלום 3")
        val cands = engine.findCandidates(original)
        // Should not transform 3 -> e causing matches
        assertTrue(cands.isEmpty())
    }

    @Test
    fun ambiguousHostAllowsSubstring() {
        val engine = buildEngineWithDefaultPatterns()
        val original = norm("assis")
        val cands = engine.findCandidates(original)
        assertTrue(cands.any { it.term == "ass" })
    }

    @Test
    fun phraseIgnoredInStep2() {
        val engine = buildEngineWithDefaultPatterns()
        val original = norm("reverse cowgirl")
        val cands = engine.findCandidates(original)
        // Phrase should NOT appear as Step2 candidate
        assertTrue(cands.none { it.term.contains("reverse") })
    }
}
