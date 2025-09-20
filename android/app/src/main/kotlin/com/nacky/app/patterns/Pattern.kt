package com.nacky.app.patterns

import kotlinx.serialization.Serializable

@Serializable
data class Pattern(
    val id: String,
    val category: String,
    val severity: String, // "low" | "medium" | "high"
    val tokensOrPhrases: List<String>
)

@Serializable
data class PatternsPayload(
    val version: Int? = null,
    val patterns: List<Pattern> = emptyList(),
    val meta: Map<String, String?>? = null,
)
