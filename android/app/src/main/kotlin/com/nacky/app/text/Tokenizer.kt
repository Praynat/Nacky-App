package com.nacky.app.text

/** Script classification (extend as needed). */
enum class Script { LATIN, HEBREW, DIGIT, OTHER }

data class Token(
    val text: String,
    val start: Int,
    val end: Int, // exclusive
    val script: Script,
)

object Tokenizer {

    fun tokenizeUnicode(normalized: String): List<Token> {
        if (normalized.isEmpty()) return emptyList()
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < normalized.length) {
            val ch = normalized[i]
            if (ch.isLetterOrDigit()) {
                val start = i
                var script = classify(ch)
                i++
                while (i < normalized.length) {
                    val c = normalized[i]
                    if (!c.isLetterOrDigit()) break
                    // Continue same token if same broad script category OR both digits
                    val sc = classify(c)
                    if (script == Script.DIGIT && sc == Script.DIGIT) {
                        i++
                        continue
                    }
                    if (script != Script.DIGIT && sc != Script.DIGIT && compatibleScripts(script, sc)) {
                        i++
                        continue
                    }
                    break
                }
                val raw = normalized.substring(start, i)
                // Optional camelCase splitting for Latin only
                if (script == Script.LATIN && raw.indexOfFirst { it.isUpperCase() } != -1) {
                    tokens.addAll(splitCamelRaw(raw, start))
                } else {
                    tokens.add(Token(raw.lowercase(), start, i, script))
                }
            } else {
                i++
            }
        }
        return tokens
    }

    private fun classify(c: Char): Script = when {
        c.isDigit() -> Script.DIGIT
        TextNormalizer.isHebrewLetter(c) -> Script.HEBREW
        c.code in 0x0041..0x024F -> Script.LATIN // basic + extended Latin ranges (approx)
        else -> Script.OTHER
    }

    private fun compatibleScripts(a: Script, b: Script): Boolean = a == b

    private fun splitCamelRaw(raw: String, offset: Int): List<Token> {
        // Already lowercase by normalizer; for safety the input here may contain lowercase only.
        // If uppercase letters exist (rare after normalization) split boundaries.
        val out = mutableListOf<Token>()
        var start = 0
        for (i in 1 until raw.length) {
            val prev = raw[i - 1]
            val curr = raw[i]
            if (prev.isLowerCase() && curr.isUpperCase()) {
                val piece = raw.substring(start, i).lowercase()
                out.add(Token(piece, offset + start, offset + i, Script.LATIN))
                start = i
            }
        }
        if (start < raw.length) {
            out.add(Token(raw.substring(start).lowercase(), offset + start, offset + raw.length, Script.LATIN))
        }
        return out
    }
}
