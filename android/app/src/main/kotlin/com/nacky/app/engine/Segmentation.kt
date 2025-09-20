package com.nacky.app.engine

import com.nacky.app.lexicon.TrieNode

/** Result of segmentation DP. */
data class SegmentationResult(
    val minCost: Int,
    val containsStandaloneTerm: Boolean,
)

/**
 * Perform a minimal-cost segmentation of [host] using a lexicon trie.
 * Cost model: known word => 0, unknown segment => length(segment).
 * Returns minimal cost and whether ANY minimal-cost segmentation contains [term] as its own segment.
 */
object Segmenter {
    fun analyze(host: String, term: String, trie: TrieNode): SegmentationResult {
        val n = host.length
        if (n == 0) return SegmentationResult(0, false)

        // dpCost[i] = minimal cost for prefix host[0 until i]
        val dpCost = IntArray(n + 1) { Int.MAX_VALUE / 4 }
        val dpTerm = BooleanArray(n + 1) // whether term appears as standalone in ANY minimal-cost segmentation for prefix
        dpCost[0] = 0

        for (i in 0 until n) {
            if (dpCost[i] == Int.MAX_VALUE / 4) continue
            // Traverse trie for known words starting at i
            var node: TrieNode? = trie
            var j = i
            while (j < n && node != null) {
                val c = host[j]
                node = node.children[c]
                if (node == null) break
                if (node.terminal) {
                    val word = host.substring(i, j + 1)
                    val newCost = dpCost[i] // known word cost 0
                    val termHere = (word == term) || dpTerm[i]
                    if (newCost < dpCost[j + 1]) {
                        dpCost[j + 1] = newCost
                        dpTerm[j + 1] = termHere
                    } else if (newCost == dpCost[j + 1] && termHere && !dpTerm[j + 1]) {
                        dpTerm[j + 1] = true
                    }
                }
                j++
            }
            // Option: extend unknown segment from i to k (we aggregate cost per segment, not per char) to avoid O(n^2) explosion we can just add one char at a time -> cost 1 each char; but we want segment cost = length.
            // Simpler: treat each additional char as part of a growing unknown; we must consider all k. Accept O(n^2) for small test hosts (<50 chars).
            for (k in i + 1..n) {
                val segLen = k - i
                val newCost = dpCost[i] + segLen
                val word = host.substring(i, k)
                val termHere = (word == term) || dpTerm[i]
                if (newCost < dpCost[k]) {
                    dpCost[k] = newCost
                    dpTerm[k] = termHere
                } else if (newCost == dpCost[k] && termHere && !dpTerm[k]) {
                    dpTerm[k] = true
                }
            }
        }
        return SegmentationResult(dpCost[n], dpTerm[n])
    }
    // adjacency helper removed per spec (any segmentation containing term as segment counts)
}
