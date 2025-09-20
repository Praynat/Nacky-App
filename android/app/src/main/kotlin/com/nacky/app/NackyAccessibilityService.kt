package com.nacky.app

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.SystemClock
import android.util.Log
import java.text.Normalizer
import java.util.Locale
// Future: use patterns instead of flat ForbiddenStore.words
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.live.LiveTypingDetector
import com.nacky.app.live.LiveSettings
import com.nacky.app.engine.Action

class NackyAccessibilityService : AccessibilityService() {
    // New monitoring engine (pattern-based) – legacy ForbiddenStore logic retained below.
    private val monitoringEngine by lazy { Engines.buildMonitoring() }
    private val liveEngines by lazy { Engines.buildLive() }
    private val liveDetector by lazy {
        val (trie, step2, step3) = liveEngines
        LiveTypingDetector(trie, step2, step3, LiveSettings(), observer = null)
    }
    private val LIVE_ENGINE_ENABLED = true // TODO: settings toggle
    override fun onServiceConnected() {}
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTyping(event)
                if (LIVE_ENGINE_ENABLED) {
                    try {
                        val text = event.text?.firstOrNull()
                        liveDetector.onTextChanged(event, text)
                    } catch (_: Throwable) {}
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Legacy content handling
                handleContent()
                // New monitoring path
                val src = event.packageName?.toString() ?: "unknown"
                val nowMs = SystemClock.elapsedRealtime()
                val snapshot = extractVisibleTextSafely(rootInActiveWindow)
                if (!snapshot.isNullOrBlank()) {
                    val detections = monitoringEngine.processSnapshot(src, snapshot, nowMs)
                    for (d in detections) {
                        // Privacy-safe: no raw snapshot text logged
                        Log.i("Nacky", "MON hit src=$src pat=${d.patternId} sev=${d.severity}")
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleContent()
                if (LIVE_ENGINE_ENABLED) liveDetector.onFocusChanged()
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> if (LIVE_ENGINE_ENABLED) liveDetector.onFocusChanged()
        }
    }

    private fun handleTyping(e: AccessibilityEvent) {
        val txt = (e.text?.joinToString("") ?: "").normalizeWord()
        if (txt.isEmpty()) return
        if (matchesForbidden(txt)) {
            clearCurrentInput(e.source)
            if (!ForbiddenStore.shouldThrottle()) {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun handleContent() {
        val root = rootInActiveWindow ?: return
        val count = countForbiddenInTree(root)
        if (count >= 3 && !ForbiddenStore.shouldThrottle()) {
            repeat(3) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                try { Thread.sleep(100) } catch (_: InterruptedException) {}
            }
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun clearCurrentInput(node: AccessibilityNodeInfo?) {
        val n = node ?: return
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        }
        if (n.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }) {
            n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } else {
            n.refresh()
            n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            performGlobalAction(GLOBAL_ACTION_BACK) // fallback
        }
    }

    private fun countForbiddenInTree(root: AccessibilityNodeInfo): Int {
        var total = 0
        fun dfs(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val chunks = listOfNotNull(node.text, node.contentDescription).map { it.toString() }
            if (chunks.isNotEmpty()) {
                val text = chunks.joinToString(" ").normalizeWord()
                total += countMatches(text)
            }
            for (i in 0 until node.childCount) dfs(node.getChild(i))
        }
        dfs(root)
        return total
    }

    private fun matchesForbidden(s: String): Boolean {
        if (ForbiddenStore.words.isEmpty()) return false
        val tokens = s.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        for (t in tokens) if (t in ForbiddenStore.words) return true
        return false
    }

    private fun countMatches(s: String): Int {
        if (ForbiddenStore.words.isEmpty()) return 0
        var c = 0
        val tokens = s.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
        for (t in tokens) if (t in ForbiddenStore.words) c++
        return c
    }

    // Extract visible text with safety constraints; never logs raw snapshot.
    private fun extractVisibleTextSafely(root: AccessibilityNodeInfo?): String {
        if (root == null) return ""
        val maxNodes = 500
        val maxChars = 10_000
        val queue: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        val seenTexts = LinkedHashSet<String>()
        var totalChars = 0
        try {
            queue.add(root)
            var processed = 0
            while (queue.isNotEmpty() && processed < maxNodes && totalChars < maxChars) {
                val node = queue.removeFirst()
                processed++
                try {
                    if (node.isPassword) continue
                    if (!node.isVisibleToUser) continue
                    val texts = sequenceOf(node.text, node.contentDescription)
                        .mapNotNull { it?.toString() }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    for (t in texts) {
                        if (t !in seenTexts) {
                            seenTexts.add(t)
                            totalChars += t.length + 1
                            if (totalChars >= maxChars) break
                        }
                    }
                    val childCount = node.childCount
                    for (i in 0 until childCount) {
                        val child = node.getChild(i)
                        if (child != null) queue.addLast(child)
                    }
                } catch (_: Throwable) {
                    // Swallow node-level issues
                }
            }
        } catch (_: Throwable) {
            return ""
        }
        return seenTexts.joinToString(" ")
    }
}

/**
 * Normalizes text for consistent processing across languages and platforms.
 * 
 * This function:
 * - Converts to lowercase
 * - Removes accent marks (é → e) using Unicode NFD normalization
 * - Squashes repeated separators (multiple spaces → single space)
 * - Preserves all scripts (Hebrew, Arabic, etc.)
 * 
 * Examples:
 * - "Crème brûlée!" → "creme brulee!"
 * - "HELLO   WORLD" → "hello world"
 * - "עברית123" → "עברית123"
 */
private fun String.normalizeWord(): String {
    if (this.isEmpty()) return this
    
    // Step 1: Trim and lowercase
    val lower = this.trim().lowercase(Locale.ROOT)
    
    // Step 2: Unicode NFD normalization to decompose characters
    val norm = Normalizer.normalize(lower, Normalizer.Form.NFD)
    
    // Step 3: Remove combining marks (diacritics)
    val withoutDiacritics = norm.replace("\\p{Mn}+".toRegex(), "")
    
    // Step 4: Squash repeated separators (spaces, tabs, etc.)
    return withoutDiacritics.replace("\\s+".toRegex(), " ").trim()
}
