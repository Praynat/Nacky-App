package com.nacky.app

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.text.Normalizer
import java.util.Locale

class NackyAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {}
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleTyping(event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleContent()
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
