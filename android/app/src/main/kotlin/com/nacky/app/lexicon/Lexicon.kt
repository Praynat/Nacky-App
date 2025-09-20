package com.nacky.app.lexicon

/** Simple Trie node for lexicon word lookups / segmentation. */
class TrieNode {
    val children: MutableMap<Char, TrieNode> = mutableMapOf()
    var terminal: Boolean = false
}

data class Lexicon(
    val words: Set<String>,
    val trie: TrieNode,
)

object LexiconLoader {
    /**
     * Load a lexicon for the given locale (language code). For now returns a tiny in-memory set
     * sufficient for unit tests. Later this can load from assets / on-device database (20–50k words).
     */
    fun load(locale: String): Lexicon {
        val lower = locale.lowercase()
        val wordSet: Set<String> = when (lower) {
            "en", "en_us", "en_gb" -> EN_WORDS
            "fr", "fr_fr" -> FR_WORDS
            "he", "iw", "he_il" -> HE_WORDS
            else -> EN_WORDS // fallback to English minimal set
        }
        return Lexicon(wordSet, buildTrie(wordSet))
    }

    private fun buildTrie(words: Set<String>): TrieNode {
        val root = TrieNode()
        for (w in words) {
            if (w.isBlank()) continue
            var node = root
            for (c in w) {
                node = node.children.getOrPut(c) { TrieNode() }
            }
            node.terminal = true
        }
        return root
    }

    // Minimal sets (keep lowercase; normalization already lowercases) – extend as needed for tests.
    private val EN_WORDS = setOf(
        // Omit "assistant" so tests can exercise R6 (long host rule) rather than R5.
        "look", "this", "is", "good", "host", "word", "sample", "unknown"
    )
    private val FR_WORDS = setOf(
        "culotte", "bonjour", "monde"
    )
    private val HE_WORDS = setOf(
        // Hebrew words without niqqud (already stripped by normalizer when configured)
        "שלום", "עולם"
    )
}
