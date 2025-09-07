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

/**
 * Unicode-aware tokenization for text processing across all languages and scripts.
 * 
 * This function:
 * - Finds sequences of letters (with their combining marks) for ALL scripts
 * - Finds sequences of digits
 * - Treats punctuation, spaces, and emojis as separators
 * - Preserves word boundaries correctly for multi-script text
 * 
 * Examples:
 * - "Hello, world!" → ["hello", "world"]
 * - "שלום—עולם" → ["שלום", "עולם"] 
 * - "go🏃fast" → ["go", "fast"]
 * - "canción123" → ["canción", "123"]
 * - "café@home" → ["café", "home"]
 */
private fun tokenizeUnicode(input: String): List<String> {
    if (input.isEmpty()) return emptyList()
    
    val tokens = mutableListOf<String>()
    val buffer = StringBuilder()
    var inWord = false
    
    for (char in input) {
        val codePoint = char.code
        
        if (isWordCharacter(codePoint)) {
            // Letter, digit, or combining mark - part of a word
            buffer.append(char)
            inWord = true
        } else {
            // Separator (space, punctuation, emoji, etc.)
            if (inWord && buffer.isNotEmpty()) {
                tokens.add(buffer.toString())
                buffer.clear()
                inWord = false
            }
        }
    }
    
    // Don't forget the last token
    if (inWord && buffer.isNotEmpty()) {
        tokens.add(buffer.toString())
    }
    
    return tokens
}

/**
 * Determines if a Unicode code point is part of a word.
 * 
 * Includes:
 * - Letters from all scripts (Latin, Hebrew, Arabic, Cyrillic, etc.)
 * - Digits (0-9 and Unicode digits)
 * - Combining marks (diacritics, accents)
 * 
 * Excludes:
 * - Punctuation and symbols
 * - Whitespace
 * - Emoji and pictographs
 */
private fun isWordCharacter(codePoint: Int): Boolean {
    // Letters (all scripts)
    if ((codePoint in 0x0041..0x005A) || // A-Z
        (codePoint in 0x0061..0x007A) || // a-z
        (codePoint in 0x00C0..0x00D6) || // Latin-1 Supplement (À-Ö)
        (codePoint in 0x00D8..0x00F6) || // Latin-1 Supplement (Ø-ö)
        (codePoint in 0x00F8..0x00FF) || // Latin-1 Supplement (ø-ÿ)
        (codePoint in 0x0100..0x017F) || // Latin Extended-A
        (codePoint in 0x0180..0x024F) || // Latin Extended-B
        (codePoint in 0x1E00..0x1EFF) || // Latin Extended Additional
        (codePoint in 0x0590..0x05FF) || // Hebrew
        (codePoint in 0x0600..0x06FF) || // Arabic
        (codePoint in 0x0750..0x077F) || // Arabic Supplement
        (codePoint in 0x08A0..0x08FF) || // Arabic Extended-A
        (codePoint in 0x0400..0x04FF) || // Cyrillic
        (codePoint in 0x0370..0x03FF) || // Greek and Coptic
        (codePoint in 0x3040..0x309F) || // Hiragana
        (codePoint in 0x30A0..0x30FF) || // Katakana
        (codePoint in 0x4E00..0x9FFF)    // CJK Unified Ideographs
    ) {
        return true
    }
    
    // Digits (ASCII and Unicode)
    if ((codePoint in 0x0030..0x0039) || // 0-9
        (codePoint in 0x0660..0x0669) || // Arabic-Indic digits
        (codePoint in 0x06F0..0x06F9)    // Extended Arabic-Indic digits
    ) {
        return true
    }
    
    // Combining marks (diacritics)
    if ((codePoint in 0x0300..0x036F) || // Combining Diacritical Marks
        (codePoint in 0x1AB0..0x1AFF) || // Combining Diacritical Marks Extended
        (codePoint in 0x1DC0..0x1DFF)    // Combining Diacritical Marks Supplement
    ) {
        return true
    }
    
    return false
}
