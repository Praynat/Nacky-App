package com.nacky.app.engine

import kotlin.text.Regex

data class TermEntry(
    val patternId: String,
    val term: String,
    val category: String,
    val severity: String,
)

data class Candidate(
    val patternId: String,
    val term: String,
    val severity: String,
    val startOrig: Int,
    val endOrig: Int,
)

data class Step2Settings(
    val removeSeparators: Boolean = true,
    val separatorsRegex: Regex = Regex("""[\p{Z}\p{P}\p{S}\u200B\u200C\u200D_\-]+"""),
    val leetEnabled: Boolean = true,
    val windowChars: Int = 0,
) {
    // strict leet map
    val leetMap: Map<Char, Char> = mapOf(
        '0' to 'o',
        '3' to 'e',
        '4' to 'a',
        '@' to 'a',
        '$' to 's',
    )
}

class Step2Engine(
    private val terms: List<TermEntry>,
    private val settings: Step2Settings = Step2Settings(),
) {
    private data class ACNode(
        val children: MutableMap<Char, ACNode> = mutableMapOf(),
        var fail: ACNode? = null,
        val outputs: MutableList<TermEntry> = mutableListOf()
    )

    private val root: ACNode = buildAutomaton(terms)

    private fun buildAutomaton(terms: List<TermEntry>): ACNode {
        val r = ACNode()
        for (t in terms) {
            if (t.term.isBlank()) continue
            var node = r
            for (ch in t.term) {
                node = node.children.getOrPut(ch) { ACNode() }
            }
            node.outputs.add(t)
        }
        // BFS to set fail links
        val queue: ArrayDeque<ACNode> = ArrayDeque()
        // depth 1 fail -> root
        for (child in r.children.values) {
            child.fail = r
            queue.add(child)
        }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for ((c, nxt) in current.children) {
                var f = current.fail
                while (f != null && f.children[c] == null) {
                    f = f.fail
                }
                nxt.fail = f?.children?.get(c) ?: r
                nxt.outputs.addAll(nxt.fail!!.outputs)
                queue.addLast(nxt)
            }
        }
        return r
    }

    private fun isLatinLetter(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'
    private fun isLetter(c: Char): Boolean = c.isLetter()

    private data class RecomposeResult(val recomposed: String, val mapIdx: IntArray)

    private fun recompose(original: String): RecomposeResult {
        if (original.isEmpty()) return RecomposeResult("", IntArray(0))
        val recomposed = StringBuilder()
        val map = IntArray(original.length) // upper bound, will slice later
        var outLen = 0
        val sepRegex = settings.separatorsRegex
        val s = original
        val leetMap = settings.leetMap
        for (i in s.indices) {
            val ch = s[i]
            val str = ch.toString()
            if (settings.removeSeparators && sepRegex.matches(str)) {
                continue
            }
            var emit = ch
            if (settings.leetEnabled && leetMap.containsKey(ch)) {
                // adjacency rule
                val prev = if (i > 0) s[i - 1] else null
                val next = if (i + 1 < s.length) s[i + 1] else null
                val adjacentLetter = (prev != null && isLetter(prev)) || (next != null && isLetter(next))
                val latinContext = (prev != null && isLatinLetter(prev)) || (next != null && isLatinLetter(next))
                if (adjacentLetter && latinContext) {
                    emit = leetMap[ch] ?: ch
                }
            }
            recomposed.append(emit)
            map[outLen] = i
            outLen++
        }
        return RecomposeResult(recomposed.toString(), map.copyOf(outLen))
    }

    fun findCandidates(originalNorm: String): List<Candidate> {
        if (originalNorm.isEmpty()) return emptyList()
        val (recomposed, mapIdx) = recompose(originalNorm)
        if (recomposed.isEmpty()) return emptyList()
        val out = mutableListOf<Candidate>()
        var node = root
        for (pos in recomposed.indices) {
            val c = recomposed[pos]
            while (node != root && node.children[c] == null) {
                node = node.fail ?: root
            }
            node = node.children[c] ?: root
            if (node.outputs.isNotEmpty()) {
                for (term in node.outputs) {
                    val len = term.term.length
                    val startR = pos - len + 1
                    if (startR >= 0) {
                        val startOrig = mapIdx[startR]
                        val endOrig = mapIdx[pos]
                        out.add(
                            Candidate(
                                patternId = term.patternId,
                                term = term.term,
                                severity = term.severity,
                                startOrig = startOrig,
                                endOrig = endOrig,
                            )
                        )
                    }
                }
            }
        }
        return out
    }
}
