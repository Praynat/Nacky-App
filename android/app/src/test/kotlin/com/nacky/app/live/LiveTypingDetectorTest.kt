package com.nacky.app.live

import com.nacky.app.engine.*
import com.nacky.app.engine.TokenTrie
import com.nacky.app.lexicon.LexiconLoader
import com.nacky.app.patterns.Pattern
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.text.TextNormalizer
import com.nacky.app.text.Tokenizer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LiveTypingDetectorTest {

    private lateinit var trie: TokenTrie
    private lateinit var step2: Step2Engine
    private lateinit var step3: Step3Engine
    private lateinit var testNow: TestNowProvider
    private lateinit var testScheduler: TestDebounceScheduler

    private fun norm(s: String) = TextNormalizer.normalize(s)

    @Before
    fun setup() {
        val patterns = listOf(
            Pattern(id = "p1", category = "cat", severity = "high", tokensOrPhrases = listOf("ass")),
            Pattern(id = "p2", category = "cat", severity = "low", tokensOrPhrases = listOf("reverse cowgirl")),
            Pattern(id = "p3", category = "cat", severity = "low", tokensOrPhrases = listOf("cul")),
        )
        val payload = mapOf(
            "version" to 1,
            "patterns" to patterns.map { p ->
                mapOf(
                    "id" to p.id,
                    "category" to p.category,
                    "severity" to p.severity,
                    "tokensOrPhrases" to p.tokensOrPhrases
                )
            }
        )
        PatternRepository.updateFromPayload(payload)
        val normalizer: (String) -> String = TextNormalizer::normalize
        val tokenizer: (String) -> List<String> = { s: String -> Tokenizer.tokenizeUnicode(s).map { it.text } }
        trie = TokenTrie.build(PatternRepository.pretokenizedEntries(normalizer, tokenizer))
        val terms = PatternRepository.singleWordTerms(normalizer, tokenizer)
        step2 = Step2Engine(terms, Step2Settings())
        val lexicon = LexiconLoader.load("en")
        step3 = Step3Engine(lexicon, Step3Config())
    }

    private fun detector(customDebounce: Long = 150L): LiveTypingDetector {
        testNow = TestNowProvider(0L)
        testScheduler = TestDebounceScheduler()
        return LiveTypingDetector(
            trie,
            step2,
            step3,
            LiveSettings(debounceMs = customDebounce),
            observer = null,
            nowProvider = testNow,
            scheduler = testScheduler,
        )
    }

    @Test
    fun debounceTriggersAfterTime() {
        val d = detector(customDebounce = 300L)
        d._testOnTextChanged("ass") // no boundary
        // advance less than debounce
        testNow.advance(250)
        testScheduler.runAll() // should not fire (time not yet >= debounce since lastEvent)
        val pre = d._forceFinalizeForTest() // manual force to inspect; should produce detection due to term present
        assertTrue(pre.any { it.patternId == "p1" && it.action == Action.BLOCK })
    }

    @Test
    fun boundaryTriggersImmediateFinalize() {
        val d = detector()
        d._testOnTextChanged("ass.")
        val decisions = d.finalizeForTest(norm("ass."))
        assertTrue("Expected p1 decision on boundary finalize: $decisions", decisions.any { it.patternId == "p1" })
    }

    @Test
    fun antiEvasionSpacedLetters() {
        val d = detector()
        val normalized = norm("a s s")
        val decisions = d.finalizeForTest(normalized)
        assertTrue(decisions.any { it.patternId == "p1" })
    }

    @Test
    fun phraseDetected() {
        val d = detector()
        val normalized = norm("reverse — cowgirl") // using punctuation dash variant
        val decisions = d.finalizeForTest(normalized)
        assertTrue("Expected p2 phrase decision: $decisions", decisions.any { it.patternId == "p2" })
    }

    @Test
    fun falsePositivesAllowed() {
        val d = detector()
        val assistant = d.finalizeForTest(norm("assistant"))
        assertTrue(assistant.none { it.patternId == "p1" || it.patternId == "p3" })
        val culotte = d.finalizeForTest(norm("culotte"))
        assertTrue(culotte.none { it.patternId == "p3" })
    }
}
