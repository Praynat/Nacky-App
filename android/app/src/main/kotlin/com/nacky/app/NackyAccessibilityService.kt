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

    private fun tokenizeUnicode(text: String): List<String> {
        // Split on non-letter, non-mark, non-digit boundaries (Unicode-aware)
        // This now preserves non-Latin scripts like Hebrew, Arabic, Chinese, etc.
        return text.split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.isNotBlank() }
    }

    private fun matchesForbidden(s: String): Boolean {
        if (ForbiddenStore.words.isEmpty()) return false
        val tokens = tokenizeUnicode(s)
        for (t in tokens) if (t in ForbiddenStore.words) return true
        return false
    }

    private fun countMatches(s: String): Int {
        if (ForbiddenStore.words.isEmpty()) return 0
        var c = 0
        val tokens = tokenizeUnicode(s)
        for (t in tokens) if (t in ForbiddenStore.words) c++
        return c
    }
}

private fun String.normalizeWord(): String {
    val lower = this.lowercase(Locale.ROOT)
    val norm = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return norm.replace("\\p{Mn}+".toRegex(), "")
}
