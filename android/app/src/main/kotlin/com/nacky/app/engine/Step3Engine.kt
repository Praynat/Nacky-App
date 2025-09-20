package com.nacky.app.engine

import com.nacky.app.lexicon.Lexicon
import com.nacky.app.text.TextNormalizer

data class Decision(val action: Action, val reason: String, val host: String)
enum class Action { BLOCK, ALLOW }

data class Step3Config(
    val minHostLong: Int = 6,
    val smallHostRatioBlock: Double = 0.6,
    val severityHighBlocks: Boolean = true,
    val segmentationEnabled: Boolean = true,
    val segmentationMaxCost: Int = 4,
)

class Step3Engine(
    private val lexicon: Lexicon,
    private val cfg: Step3Config = Step3Config(),
) {
    /** Decide for candidate; cand indices inclusive over normalized string. */
    fun decide(originalNorm: String, cand: Candidate): Decision {
        if (originalNorm.isEmpty()) return Decision(Action.ALLOW, "EMPTY", "")
        val n = originalNorm.length
        var left = cand.startOrig
        var right = cand.endOrig
        if (left < 0 || right >= n || left > right) return Decision(Action.ALLOW, "BOUNDS", "")

        // Expand to host word boundaries (letters/marks same script)
        while (left - 1 >= 0 && sameScriptLetter(originalNorm[left - 1], originalNorm[left])) left--
        while (right + 1 < n && sameScriptLetter(originalNorm[right + 1], originalNorm[right])) right++
        val host = originalNorm.substring(left, right + 1)
        val term = cand.term

        // R2 (edge boundary) evaluated BEFORE R1 when punctuation immediately outside host word.
        val outerBefore = if (left - 1 >= 0) originalNorm[left - 1] else null
        val outerAfter = if (right + 1 < n) originalNorm[right + 1] else null
        val coversHost = cand.startOrig == left && cand.endOrig == right
        val outerBeforeBoundary = outerBefore != null && !outerBefore.isLetter()
        val outerAfterBoundary = outerAfter != null && !outerAfter.isLetter()
        val candidateAtHostPrefix = cand.startOrig == left
        val candidateAtHostSuffix = cand.endOrig == right
        if (coversHost && (outerBeforeBoundary || outerAfterBoundary)) {
            // Prefer R2 over R1 when punctuation boundary exists.
            return Decision(Action.BLOCK, "R2", host)
        } else if (!coversHost) {
            // Term at prefix/suffix inside larger host word AND boundary outside other side.
            if (candidateAtHostPrefix && outerBeforeBoundary) return Decision(Action.BLOCK, "R2", host)
            if (candidateAtHostSuffix && outerAfterBoundary) return Decision(Action.BLOCK, "R2", host)
        }

        // R1 Exact match (no punctuation boundary case)
        if (host == term) return Decision(Action.BLOCK, "R1", host)

        // R3 Short host dominated
        if (host.length <= 5) {
            val ratio = term.length.toDouble() / host.length.toDouble()
            if (ratio >= cfg.smallHostRatioBlock) return Decision(Action.BLOCK, "R3", host)
        }

        // R5 Safe host (lexicon word)
        if (lexicon.words.contains(host)) return Decision(Action.ALLOW, "R5", host)

        // R6B Prefix/Suffix partial inside longer host (e.g., 'ass' in 'assistant') -> allow (after lexicon so R5 preferred when host safe)
        if (host.length >= cfg.minHostLong) {
            val innerIdx = host.indexOf(term)
            if (innerIdx == 0 && term.length < host.length) {
                val after = host.getOrNull(term.length)
                if (after != null && after.isLetter()) {
                    return Decision(Action.ALLOW, "R6B", host)
                }
            } else if (innerIdx > 0 && innerIdx + term.length == host.length) {
                val before = host.getOrNull(innerIdx - 1)
                if (before != null && before.isLetter()) {
                    return Decision(Action.ALLOW, "R6B", host)
                }
            }
        }

        // R4 Severity high (moved after R5/R6B so safe or partial-host allows win first)
        if (cfg.severityHighBlocks && cand.severity.lowercase() == "high") {
            return Decision(Action.BLOCK, "R4", host)
        }

        // R7 Segmentation DP check (moved before R6 so segmentation can BLOCK before long-host allow)
        if (cfg.segmentationEnabled) {
            val seg = Segmenter.analyze(host, term, lexicon.trie)
            if (seg.minCost <= cfg.segmentationMaxCost) {
                if (seg.containsStandaloneTerm) {
                    return Decision(Action.BLOCK, "R7", host)
                } else {
                    return Decision(Action.ALLOW, "R7A", host)
                }
            }
        }

        // R6 Long host with term strictly inside (both sides letters) -> allow (only if not already handled by segmentation)
        if (host.length >= cfg.minHostLong) {
            val innerIndex = host.indexOf(term)
            if (innerIndex > 0 && innerIndex + term.length < host.length) {
                val before = host[innerIndex - 1]
                val after = host[innerIndex + term.length]
                if (before.isLetter() && after.isLetter()) {
                    return Decision(Action.ALLOW, "R6", host)
                }
            }
        }

        // Default
        return Decision(Action.ALLOW, "FALLBACK", host)
    }

    private fun sameScriptLetter(a: Char, b: Char): Boolean {
        if (!a.isLetter() || !b.isLetter()) return false
        return TextNormalizer.isLetterSameScript(a, b)
    }
}
