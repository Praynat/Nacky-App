package com.nacky.app.live

import android.view.accessibility.AccessibilityEvent
import com.nacky.app.engine.*
import com.nacky.app.text.TextNormalizer
import com.nacky.app.text.Tokenizer

data class LiveSettings(
    val debounceMs: Long = 500,
    val blockHighSeverityOnly: Boolean = true,
)

interface LiveObserver { fun onDecision(patternId: String, severity: String, action: Action, reason: String) }

data class LiveDecision(val patternId: String, val severity: String, val action: Action, val reason: String)

class LiveTypingDetector(
    private val trie: TokenTrie,
    private val step2: Step2Engine,
    private val step3: Step3Engine,
    private val settings: LiveSettings = LiveSettings(),
    private val observer: LiveObserver? = null,
    private val nowProvider: NowProvider = RealNowProvider(),
    private val scheduler: DebounceScheduler = RealDebounceScheduler(),
) {
    private val buffer = StringBuilder()
    private var lastEventWallMs: Long = 0L
    private var debounceScheduled = false
    private val boundaryRegex = Regex("""[\u0020\p{P}\p{S}\d_\-]""")

    fun onTextChanged(event: AccessibilityEvent, originalText: CharSequence?) { try { processRawInput(originalText) } catch (_: Throwable) {} }

    private fun processRawInput(originalText: CharSequence?) {
        val now = nowProvider.nowMs()
        lastEventWallMs = now
        scheduleDebounce()
        val raw = originalText?.toString() ?: return
        val norm = TextNormalizer.normalize(raw)
        if (norm.length < buffer.length - 2 || norm.length < 4 && buffer.length > 10) {
            buffer.clear(); buffer.append(norm)
        } else if (norm.startsWith(buffer) && norm.length >= buffer.length) {
            buffer.append(norm.substring(buffer.length))
        } else if (buffer.isEmpty()) {
            buffer.append(norm)
        } else {
            buffer.clear(); buffer.append(norm)
        }
        val prev = buffer.takeIf { it.isNotEmpty() }?.getOrNull(buffer.length - 2)
        val newChar = buffer.takeIf { it.isNotEmpty() }?.getOrNull(buffer.length - 1)
        if (isBoundary(prev, newChar)) finalizeAndRun(buffer.toString(), clear = true)
    }

    fun onFocusChanged() { buffer.clear(); lastEventWallMs = 0L; cancelDebounce() }

    private fun scheduleDebounce() {
        if (debounceScheduled) return
        debounceScheduled = true
        scheduler.schedule(settings.debounceMs) {
            debounceScheduled = false
            val now = nowProvider.nowMs()
            if (lastEventWallMs > 0 && now - lastEventWallMs >= settings.debounceMs) {
                if (buffer.isNotEmpty()) finalizeAndRun(buffer.toString(), clear = true)
            } else if (buffer.isNotEmpty()) {
                scheduleDebounce()
            }
        }
    }

    private fun cancelDebounce() { scheduler.cancelAll(); debounceScheduled = false }

    private fun isBoundary(prevChar: Char?, newChar: Char?): Boolean {
        if (newChar == null) return false
        if (boundaryRegex.matches(newChar.toString())) return true
        return false
    }

    private fun finalizeAndRun(normalized: String, clear: Boolean = false): List<LiveDecision> {
        if (normalized.isBlank()) return emptyList()
        val tokenTexts = Tokenizer.tokenizeUnicode(normalized).map { it.text }
        val decisions = mutableListOf<LiveDecision>()
        val step1Matches = try { trie.match(tokenTexts) } catch (_: Throwable) { emptyList() }
        for (m in step1Matches) {
            val sev = m.severity ?: "low"
            val action = if (!settings.blockHighSeverityOnly || sev.lowercase() == "high") Action.BLOCK else Action.ALLOW
            recordDecision(m.patternId, sev, action, "STEP1", decisions)
        }
        val candidates = try { step2.findCandidates(normalized) } catch (_: Throwable) { emptyList() }
        for (cand in candidates) {
            val d = runStep3(normalized, cand)
            val finalAction = if (d.action == Action.BLOCK && settings.blockHighSeverityOnly && cand.severity.lowercase() != "high") Action.ALLOW else d.action
            val benignAllow = finalAction == Action.ALLOW && (
                d.reason == "R6" || d.reason == "R6B" || (d.reason == "R5" && d.host != cand.term)
            )
            if (!benignAllow) {
                recordDecision(cand.patternId, cand.severity, finalAction, d.reason, decisions)
            }
        }
        if (clear) buffer.clear()
        return decisions
    }

    private fun runStep3(normalized: String, cand: Candidate): Decision =
        try { step3.decide(normalized, cand) } catch (_: Throwable) { Decision(Action.ALLOW, "ERR", "") }

    private fun recordDecision(patternId: String, severity: String, action: Action, reason: String, sink: MutableList<LiveDecision>) {
        // Ensure we ALWAYS add the decision to sink even if android.util.Log (not mocked in JVM tests) throws.
        try {
            android.util.Log.i(
                "Nacky",
                "LIVE ${(if (action == Action.BLOCK) "block" else "warn")} pat=$patternId sev=$severity reason=$reason"
            )
        } catch (_: Throwable) { /* ignore logging issues in unit tests */ }
        try { observer?.onDecision(patternId, severity, action, reason) } catch (_: Throwable) { }
        sink.add(LiveDecision(patternId, severity, action, reason))
    }

    internal fun _forceFinalizeForTest(): List<LiveDecision> = if (buffer.isNotEmpty()) finalizeAndRun(buffer.toString(), clear = true) else emptyList()
    internal fun _testOnTextChanged(snapshot: String) = processRawInput(snapshot)
    internal fun finalizeForTest(inputNorm: String): List<LiveDecision> = finalizeAndRun(inputNorm, clear = false)
    internal fun _debugTokenTexts(inputNorm: String): List<String> = Tokenizer.tokenizeUnicode(inputNorm).map { it.text }
}
