package com.nacky.app.patterns

data class Pattern(
    val id: String,
    val category: String,
    val severity: String, // "low" | "medium" | "high"
    val tokensOrPhrases: List<String>
)

data class PatternsPayload(
    val version: Int? = null,
    val patterns: List<Pattern> = emptyList(),
    val meta: Map<String, Any?>? = null,
)
