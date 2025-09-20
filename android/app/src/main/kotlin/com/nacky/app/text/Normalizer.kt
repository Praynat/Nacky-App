package com.nacky.app.text

import java.text.Normalizer
import java.util.Locale

/** Configuration flags for normalization. */
data class NormalizerConfig(
    val stripHebrewNiqqud: Boolean = true,
    val enableLeetSubstitutions: Boolean = true,
)

/** Normalization utilities shared by detection engines. */
object TextNormalizer {
    private val leetMap = mapOf(
        '@' to 'a',
        '$' to 's',
        '0' to 'o',
        '3' to 'e',
        '1' to 'i'
    )

    // Hebrew combining marks (niqqud) range: 0591–05C7 (subset) but core vowel points 05B0–05BC, 05C1,05C2,05C4,05C5,05C7
    private val hebrewCombiningRegex = Regex("[\u0591-\u05C7]")
    private val combiningMarks = Regex("\\p{Mn}+")
    private val whitespaceLike = Regex("\\s+")

    fun normalize(input: String, cfg: NormalizerConfig = NormalizerConfig()): String {
        if (input.isEmpty()) return input
        // Log config once (could be refined with a static flag) - lightweight guard
        if (!loggedConfig) {
            try {
                android.util.Log.i(
                    "Nacky",
                    "TextNormalizer config stripHebrewNiqqud=${cfg.stripHebrewNiqqud} enableLeet=${cfg.enableLeetSubstitutions}"
                )
            } catch (_: Throwable) {
                // In local JVM unit tests android.util.Log methods throw 'not mocked'; ignore.
            } finally {
                loggedConfig = true
            }
        }
        var s = input.trim().lowercase(Locale.ROOT)
        // Full Unicode NFD
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
        // Remove Latin (and other) combining marks
        s = s.replace(combiningMarks, "")
        if (cfg.stripHebrewNiqqud) {
            s = s.replace(hebrewCombiningRegex, "")
        }
        if (cfg.enableLeetSubstitutions) {
            val b = StringBuilder(s.length)
            for (idx in s.indices) {
                val ch = s[idx]
                val sub = leetMap[ch]
                if (sub != null) {
                    if (ch.isDigit()) {
                        // Only substitute digit-form leet if adjacent to a letter (part of a word),
                        // so pure numbers like 123 remain numeric tokens.
                        val prevLetter = idx > 0 && s[idx - 1].isLetter()
                        val nextLetter = idx + 1 < s.length && s[idx + 1].isLetter()
                        if (prevLetter || nextLetter) {
                            b.append(sub)
                        } else {
                            b.append(ch)
                        }
                    } else {
                        b.append(sub)
                    }
                } else {
                    b.append(ch)
                }
            }
            s = b.toString()
        }
        // Collapse spaces & similar separators
        s = s.replace(whitespaceLike, " ").trim()
        return s
    }

    fun isHebrewLetter(ch: Char): Boolean = ch.code in 0x0590..0x05FF && ch.isLetter()

    /** Returns true if both chars are letters and belong to the same broad script (Hebrew vs other). */
    fun isLetterSameScript(a: Char, b: Char): Boolean {
        if (!a.isLetter() || !b.isLetter()) return false
        val aHeb = isHebrewLetter(a)
        val bHeb = isHebrewLetter(b)
        return aHeb == bHeb
    }
}

private var loggedConfig = false
