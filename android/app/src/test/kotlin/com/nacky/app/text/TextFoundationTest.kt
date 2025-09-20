package com.nacky.app.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TextFoundationTest {

    @Test
    fun helloWorld() {
        val norm = TextNormalizer.normalize("Hello, world!")
        val toks = Tokenizer.tokenizeUnicode(norm)
        assertEquals(listOf("hello", "world"), toks.map { it.text })
    }

    @Test
    fun hebrewSimple() {
        val norm = TextNormalizer.normalize("שלום—עולם")
        val toks = Tokenizer.tokenizeUnicode(norm)
        assertEquals(listOf("שלום", "עולם"), toks.map { it.text })
    }

    @Test
    fun emojiSeparator() {
        val norm = TextNormalizer.normalize("go🏃fast")
        val toks = Tokenizer.tokenizeUnicode(norm)
        assertEquals(listOf("go", "fast"), toks.map { it.text })
    }

    @Test
    fun mixedUnderscoreDigits() {
        val norm = TextNormalizer.normalize("HELLO_world-123")
        val toks = Tokenizer.tokenizeUnicode(norm)
        assertEquals(listOf("hello", "world", "123"), toks.map { it.text })
    }

    @Test
    fun hebrewNiqqudStripping() {
        val raw = "שָׁלוֹם" // with niqqud
        val withStrip = TextNormalizer.normalize(raw, NormalizerConfig(stripHebrewNiqqud = true))
        val withoutStrip = TextNormalizer.normalize(raw, NormalizerConfig(stripHebrewNiqqud = false))
        // Tokenize; removing niqqud should not alter base letter sequence comparison (after stripping manually)
        assertEquals(withStrip, TextNormalizer.normalize(withoutStrip, NormalizerConfig(stripHebrewNiqqud = true)))
    }

    @Test
    fun camelCaseLatin() {
        val norm = TextNormalizer.normalize("goFastNow")
        val toks = Tokenizer.tokenizeUnicode(norm)
        // After normalization everything is lowercase so camel split may not trigger; ensure single token fallback.
        assertEquals(listOf("gofastnow"), toks.map { it.text })
    }
}
