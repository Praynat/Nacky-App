package com.nacky.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.SystemClock
import android.util.Log
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.live.LiveTypingDetector
import com.nacky.app.live.LiveSettings
import com.nacky.app.engine.Action

class NackyAccessibilityService : AccessibilityService() {
    private val monitoringEngine by lazy { Engines.buildMonitoring() }
    private val liveEngines by lazy { Engines.buildLive() }
    private val liveDetector by lazy {
        val (trie, step2, step3) = liveEngines
        LiveTypingDetector(trie, step2, step3, LiveSettings(), observer = null)
    }
    private val LIVE_ENGINE_ENABLED = true // TODO: settings toggle
    
    override fun onServiceConnected() {
        Log.i("Nacky", "Service connected: pattern pipeline ACTIVE (legacy removed)")
    }
    
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                if (LIVE_ENGINE_ENABLED) {
                    try {
                        val text = event.text?.firstOrNull()
                        liveDetector.onTextChanged(event, text)
                    } catch (_: Throwable) {}
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
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
                if (LIVE_ENGINE_ENABLED) liveDetector.onFocusChanged()
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> if (LIVE_ENGINE_ENABLED) liveDetector.onFocusChanged()
        }
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
