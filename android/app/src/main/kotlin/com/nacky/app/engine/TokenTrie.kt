package com.nacky.app.engine

import com.nacky.app.patterns.Pattern

/** Represents a pre-tokenized phrase mapping back to an originating pattern. */
data class PhraseEntry(
    val patternId: String,
    val category: String?,
    val severity: String?,
    val tokenSequence: List<String>,
)

/** Match produced by TokenTrie.match */
data class Match(
    val patternId: String,
    val category: String?,
    val severity: String?,
    val startTokenIndex: Int,
    val endTokenIndex: Int,
)

private class Node {
    val next: MutableMap<String, Node> = mutableMapOf()
    // A node can correspond to multiple patterns ending at this sequence
    val terminal: MutableList<PhraseEntry> = mutableListOf()
}

/**
 * TokenTrie builds a trie of token sequences for exact token-level matching of words & multi-token phrases.
 * - No substring matches inside a single token.
 * - Matching is case / diacritic agnostic assuming upstream normalization.
 */
class TokenTrie private constructor(private val root: Node) {

    fun match(tokens: List<String>): List<Match> {
        if (tokens.isEmpty()) return emptyList()
        val results = mutableListOf<Match>()
        for (i in tokens.indices) {
            var node: Node? = root
            var j = i
            while (j < tokens.size && node != null) {
                val tok = tokens[j]
                node = node.next[tok]
                if (node == null) break
                if (node.terminal.isNotEmpty()) {
                    for (entry in node.terminal) {
                        results.add(
                            Match(
                                patternId = entry.patternId,
                                category = entry.category,
                                severity = entry.severity,
                                startTokenIndex = i,
                                endTokenIndex = j,
                            )
                        )
                    }
                }
                j++
            }
        }
        return results
    }

    companion object {
        fun build(entries: List<PhraseEntry>): TokenTrie {
            val root = Node()
            for (e in entries) {
                if (e.tokenSequence.isEmpty()) continue
                var node = root
                for (tok in e.tokenSequence) {
                    node = node.next.getOrPut(tok) { Node() }
                }
                node.terminal.add(e)
            }
            return TokenTrie(root)
        }
    }
}
