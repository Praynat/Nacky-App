package com.nacky.app.engine

import com.nacky.app.patterns.Pattern
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.text.TextNormalizer
import com.nacky.app.text.Tokenizer
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenTrieTest {

    private fun norm(s: String) = TextNormalizer.normalize(s)
    private fun toks(norm: String) = Tokenizer.tokenizeUnicode(norm).map { it.text }

    private fun buildRepo(patterns: List<Pattern>) {
        // Use repository update to store patterns for helper usage.
        // We craft a pseudo-payload structure directly by reflection-free path: create a minimal JSON map.
        // For simplicity, we bypass version/meta.
        val payload = mapOf(
            "version" to 1,
            "patterns" to patterns.map {
                mapOf(
                    "id" to it.id,
                    "category" to it.category,
                    "severity" to it.severity,
                    "tokensOrPhrases" to it.tokensOrPhrases
                )
            }
        )
        PatternRepository.updateFromPayload(payload)
    }

    @Test
    fun scenarios() {
        val patterns = listOf(
            Pattern(id = "p1", category = "cat", severity = "low", tokensOrPhrases = listOf("ass")),
            Pattern(id = "p2", category = "cat", severity = "med", tokensOrPhrases = listOf("reverse cowgirl")),
            Pattern(id = "p3", category = "cat", severity = "low", tokensOrPhrases = listOf("cul")),
        )
        buildRepo(patterns)
        val entries = PatternRepository.pretokenizedEntries(::norm, ::toks)
        val trie = TokenTrie.build(entries)

        fun matchSentence(s: String): List<Match> {
            val n = norm(s)
            val t = toks(n)
            return trie.match(t)
        }

        // 1. "Hello, world!" → no matches
        assertEquals(emptyList<Match>(), matchSentence("Hello, world!"))

        // 2. "reverse cowgirl" → match phrase over 2 tokens
        val m2 = matchSentence("reverse cowgirl")
        assertEquals(1, m2.size)
        assertEquals("p2", m2[0].patternId)
        assertEquals(0, m2[0].startTokenIndex)
        assertEquals(1, m2[0].endTokenIndex)

        // 3. "reverse — cowgirl" punctuation variant -> still 2 tokens
        val m3 = matchSentence("reverse — cowgirl")
        assertEquals(1, m3.size)
        assertEquals("p2", m3[0].patternId)

        // 4. "class" should NOT match "ass"
        val m4 = matchSentence("class")
        assertEquals(0, m4.size)

        // 5. "culotte" should NOT match "cul"
        val m5 = matchSentence("culotte")
        assertEquals(0, m5.size)

        // 6. "regarde ce cul" should match single token pattern cul
        val m6 = matchSentence("regarde ce cul")
        assertEquals(1, m6.size)
        assertEquals("p3", m6[0].patternId)
    }
}
