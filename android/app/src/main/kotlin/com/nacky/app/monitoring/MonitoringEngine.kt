
package com.nacky.app.monitoring

import com.nacky.app.engine.Match
import com.nacky.app.engine.TokenTrie
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.text.TextNormalizer
import com.nacky.app.text.Tokenizer
import java.util.ArrayDeque
import kotlin.math.max

enum class CountMode { UNIQUE_PER_SNAPSHOT, ALL_MATCHES }

/** Monitoring configuration (temporary — TODO: wire to central settings). */
data class MonitoringSettings(
    val minOccurrences: Int = 3,
    val windowSeconds: Long = 300,
    val cooldownMs: Long = 10_000,
    val countMode: CountMode = CountMode.UNIQUE_PER_SNAPSHOT,
    val debugHook: ((patternId: String, sourceId: String, count: Int, nowMs: Long, triggered: Boolean) -> Unit)? = null,
)

data class Detection(
    val patternId: String,
    val category: String?,
    val severity: String?,
    val sourceId: String,
    val tsMs: Long,
)

/**
 * MonitoringEngine maintains rolling counts per (patternId, sourceId) and produces a detection
 * when occurrences within the sliding window first reach >= minOccurrences and cooldown passed.
 */
class MonitoringEngine(
    private val trie: TokenTrie,
    private val normalizer: (String) -> String,
    private val tokenizer: (String) -> List<String>,
    val settings: MonitoringSettings = MonitoringSettings(),
) {
    private data class Key(val patternId: String, val sourceId: String)

    private val buckets = mutableMapOf<Key, ArrayDeque<Long>>()
    private val lastAction = mutableMapOf<Key, Long>()

    fun processSnapshot(sourceId: String, rawText: String, nowMs: Long): List<Detection> {
        if (rawText.isBlank()) return emptyList()
        val norm = normalizer(rawText)
        if (norm.isBlank()) return emptyList()
        val tokenStrings = tokenizer(norm)
        if (tokenStrings.isEmpty()) return emptyList()

        val matches: List<Match> = trie.match(tokenStrings)
        if (matches.isEmpty()) return emptyList()

        val detections = mutableListOf<Detection>()
        val windowStart = nowMs - settings.windowSeconds * 1000

        // Group matches by patternId so we can enforce at most one detection per pattern per call.
        val matchesByPattern = matches.groupBy { it.patternId }

        for ((patternId, patternMatches) in matchesByPattern) {
            val key = Key(patternId, sourceId)
            val dq = buckets.getOrPut(key) { ArrayDeque() }

            // Remove expired timestamps first.
            while (dq.isNotEmpty() && dq.first() < windowStart) dq.removeFirst()

            val last = lastAction[key]
            val inCooldown = last != null && nowMs - last < settings.cooldownMs
            if (inCooldown) {
                // Ignore occurrences during cooldown period for fresh counting semantics.
                settings.debugHook?.invoke(patternId, sourceId, dq.size, nowMs, false)
                continue
            }

            val increments = when (settings.countMode) {
                CountMode.UNIQUE_PER_SNAPSHOT -> 1
                CountMode.ALL_MATCHES -> patternMatches.size
            }
            repeat(increments) { dq.addLast(nowMs) }

            val count = dq.size
            var triggered = false
            if (count >= settings.minOccurrences) {
                val rep = patternMatches.first()
                detections.add(
                    Detection(
                        patternId = rep.patternId,
                        category = rep.category,
                        severity = rep.severity,
                        sourceId = sourceId,
                        tsMs = nowMs,
                    )
                )
                lastAction[key] = nowMs
                triggered = true
                // Start fresh series after detection (do not count the detection snapshot towards next series).
                dq.clear()
            }
            settings.debugHook?.invoke(patternId, sourceId, count, nowMs, triggered)
        }
        return detections
    }
}

/** Factory object for building engines (placeholder until DI). */
object Engines {
    fun buildMonitoring(settings: MonitoringSettings = MonitoringSettings()): MonitoringEngine {
        val entries = PatternRepository.pretokenizedEntries(
            normalizer = { s -> TextNormalizer.normalize(s) },
            tokenizer = { s -> Tokenizer.tokenizeUnicode(s).map { it.text } }
        )
        val trie = TokenTrie.build(entries)
        return MonitoringEngine(
            trie = trie,
            normalizer = { s -> TextNormalizer.normalize(s) },
            tokenizer = { s -> Tokenizer.tokenizeUnicode(s).map { it.text } },
            settings = settings,
        )
    }
}
