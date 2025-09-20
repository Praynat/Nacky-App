package com.nacky.app

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.SystemClock
import android.util.Log
import java.text.Normalizer
import java.util.Locale
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.persistence.PatternsStore
import com.nacky.app.persistence.SettingsStore
import com.nacky.app.live.LiveTypingDetector
import com.nacky.app.live.LiveSettings
import com.nacky.app.engine.Action

class NackyAccessibilityService : AccessibilityService() {
    private val monitoringEngine by lazy { Engines.buildMonitoring() }
    private val liveEngines by lazy { Engines.buildLive() }
    private val liveDetector by lazy {
        val (trie, step2, step3) = liveEngines
        LiveTypingDetector(trie, step2, step3, LiveSettings(), observer = null).also { Engines.registerLiveDetector(it) }
    }
    override fun onServiceConnected() {
        // 1. Load persisted settings (if any) into in-memory store
        val loadedSettings = try { SettingsStore.load(applicationContext) } catch (_: Throwable) { null }
        loadedSettings?.let { DetectionSettingsStore.current.set(it) }

        // 2. Load persisted patterns (only if repo empty to avoid clobbering a push done earlier in session)
        val loadedPatterns = try {
            if (PatternRepository.all().isEmpty()) PatternsStore.load(applicationContext) else null
        } catch (_: Throwable) { null }
        if (loadedPatterns != null) {
            PatternRepository.updateFromPayload(
                mapOf(
                    "version" to loadedPatterns.version,
                    "patterns" to loadedPatterns.patterns.map { p ->
                        mapOf(
                            "id" to p.id,
                            "category" to p.category,
                            "severity" to p.severity,
                            "tokensOrPhrases" to p.tokensOrPhrases
                        )
                    },
                    "meta" to loadedPatterns.meta
                ),
                applicationContext
            )
        }

        // 3. Compute signature and skip heavy rebuilds if unchanged
        val sig = try { Engines.computeSignature() } catch (_: Throwable) { null }
        val rebuild = sig == null || Engines.shouldRebuild(sig)
        if (rebuild) {
            // Force builds (lazy) only when signature changed
            try { monitoringEngine } catch (_: Throwable) {}
            try { liveEngines } catch (_: Throwable) {}
            try { liveDetector } catch (_: Throwable) {}
        }

        // 4. Apply runtime settings to engines (Monitoring + Live) without exposing raw text
        val effective = DetectionSettingsStore.current.get()
        try {
            Engines.updateMonitoringSettings(
                com.nacky.app.monitoring.MonitoringSettings(
                    minOccurrences = effective.minOccurrences,
                    windowSeconds = effective.windowSeconds, // Long already
                    cooldownMs = effective.cooldownMs, // Long already
                    countMode = if (effective.countMode == "ALL_MATCHES") com.nacky.app.monitoring.CountMode.ALL_MATCHES else com.nacky.app.monitoring.CountMode.UNIQUE_PER_SNAPSHOT
                )
            )
            Engines.updateLiveSettings(
                LiveSettings(
                    debounceMs = effective.debounceMs, // Long
                    blockHighSeverityOnly = effective.blockHighSeverityOnly
                )
            )
        } catch (_: Throwable) {}

        // 5. Concise logging (no raw user content)
        try {
            val patCount = PatternRepository.all().size
            val version = loadedPatterns?.version ?: "-"
            Log.i("Nacky", "ServiceConnected: settingsLoaded=${loadedSettings!=null} patternsLoaded=${loadedPatterns!=null} version=$version patternCount=$patCount live=${effective.liveEnabled} monitoring=${effective.monitoringEnabled} rebuild=$rebuild")
        } catch (_: Throwable) {}
    }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val settings = DetectionSettingsStore.current.get()
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTyping(event) // legacy flat word logic
                if (settings.liveEnabled) {
                    try {
                        val text = event.text?.firstOrNull()
                        liveDetector.onTextChanged(event, text)
                    } catch (_: Throwable) {}
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleContent()
                if (settings.monitoringEnabled) {
                    val src = event.packageName?.toString() ?: "unknown"
                    val nowMs = SystemClock.elapsedRealtime()
                    val snapshot = extractVisibleTextSafely(rootInActiveWindow)
                    if (!snapshot.isNullOrBlank()) {
                        val effectiveSettings = settings
                        // Apply runtime monitoring overrides
                        monitoringEngine.updateSettings(
                            monitoringEngine.settings.copy(
                                minOccurrences = effectiveSettings.minOccurrences,
                                windowSeconds = effectiveSettings.windowSeconds.toLong(),
                                cooldownMs = effectiveSettings.cooldownMs.toLong(),
                                countMode = if (effectiveSettings.countMode == "ALL_MATCHES") com.nacky.app.monitoring.CountMode.ALL_MATCHES else com.nacky.app.monitoring.CountMode.UNIQUE_PER_SNAPSHOT
                            )
                        )
                        val detections = monitoringEngine.processSnapshot(src, snapshot, nowMs)
                        for (d in detections) {
                            Log.i("Nacky", "MON hit src=$src pat=${d.patternId} sev=${d.severity}")
                        }
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleContent()
                if (settings.liveEnabled) liveDetector.onFocusChanged()
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> if (settings.liveEnabled) liveDetector.onFocusChanged()
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
                } catch (_: Throwable) { }
            }
        } catch (_: Throwable) { return "" }
        return seenTexts.joinToString(" ")
    }
}

private fun String.normalizeWord(): String {
    if (this.isEmpty()) return this
    val lower = this.trim().lowercase(Locale.ROOT)
    val norm = Normalizer.normalize(lower, Normalizer.Form.NFD)
    val withoutDiacritics = norm.replace("\\p{Mn}+".toRegex(), "")
    return withoutDiacritics.replace("\\s+".toRegex(), " ").trim()
}
