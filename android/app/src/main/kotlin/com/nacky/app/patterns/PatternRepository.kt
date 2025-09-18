package com.nacky.app.patterns

import android.util.Log
import org.json.JSONObject
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicReference

/** Thread-safe in-memory repository for compiled detection patterns. */
object PatternRepository {
    private val patternsRef = AtomicReference<List<Pattern>>(emptyList())
    private val lastMetaRef = AtomicReference<Map<String, Any?>>(emptyMap())
    private val versionRef = AtomicReference<Int?>(null)

    data class UpdateResult(
        val ok: Boolean,
        val patternCount: Int,
        val itemTotal: Int,
        val categories: Map<String, Int>,
        val severities: Map<String, Int>,
        val error: String? = null,
    )

    fun all(): List<Pattern> = patternsRef.get()

    fun countByCategory(): Map<String, Int> = all().groupingBy { it.category }.eachCount()

    fun countBySeverity(): Map<String, Int> = all().groupingBy { it.severity }.eachCount()

    /** Accepts either a Map<String, Any?> (StandardMessageCodec) or JSON String. */
    fun updateFromPayload(obj: Any?): UpdateResult {
        return try {
            val payload = when (obj) {
                is Map<*, *> -> fromMap(obj as Map<String, Any?>)
                is String -> fromJson(obj)
                else -> throw IllegalArgumentException("Unsupported payload type: ${obj?.javaClass}")
            }
            patternsRef.set(payload.patterns)
            versionRef.set(payload.version)
            lastMetaRef.set(payload.meta ?: emptyMap())

            val itemTotal = payload.patterns.sumOf { it.tokensOrPhrases.size }
            val categories = payload.patterns.groupingBy { it.category }.eachCount()
            val severities = payload.patterns.groupingBy { it.severity }.eachCount()

            try {
                Log.i(
                    "Nacky",
                    "PatternRepository update: patterns=${payload.patterns.size}, items=$itemTotal, categories=$categories, severities=$severities"
                )
            } catch (_: Throwable) { /* ignore in unit tests */ }
            UpdateResult(true, payload.patterns.size, itemTotal, categories, severities, null)
        } catch (e: Exception) {
            try {
                Log.e("Nacky", "PatternRepository update failed: ${e.message}", e)
            } catch (_: Throwable) { /* ignore in unit tests */ }
            UpdateResult(false, 0, 0, emptyMap(), emptyMap(), e.message)
        }
    }

        /** Pre-tokenize pattern entries into PhraseEntry (engine adapter layer). */
        fun pretokenizedEntries(
            normalizer: (String) -> String,
            tokenizer: (String) -> List<String>,
        ): List<com.nacky.app.engine.PhraseEntry> {
            val out = mutableListOf<com.nacky.app.engine.PhraseEntry>()
            val seen = HashSet<List<String>>()
            for (p in all()) {
                for (raw in p.tokensOrPhrases) {
                    val norm = normalizer(raw)
                    if (norm.isBlank()) continue
                    val toks = tokenizer(norm).filter { it.isNotBlank() }
                    if (toks.isEmpty()) continue
                    if (seen.add(toks)) {
                        out.add(
                            com.nacky.app.engine.PhraseEntry(
                                patternId = p.id,
                                category = p.category,
                                severity = p.severity,
                                tokenSequence = toks
                            )
                        )
                    }
                }
            }
            return out
        }

    private fun fromMap(map: Map<String, Any?>): PatternsPayload {
        val version = (map["version"] as? Number)?.toInt()
        val patternsRaw = map["patterns"]
            ?: throw IllegalArgumentException("Missing 'patterns'")
        val list = mutableListOf<Pattern>()
        if (patternsRaw is List<*>) {
            for (p in patternsRaw) {
                if (p !is Map<*, *>) continue
                val id = p["id"]?.toString() ?: continue
                val category = p["category"]?.toString() ?: "default"
                val severity = p["severity"]?.toString() ?: "medium"
                val itemsAny = p["tokensOrPhrases"]
                val items = if (itemsAny is List<*>) itemsAny.mapNotNull { it?.toString() } else emptyList()
                list.add(Pattern(id, category.lowercase(), severity.lowercase(), items.map { it.lowercase() }))
            }
        } else throw IllegalArgumentException("'patterns' not a list")
        val meta = map["meta"] as? Map<String, Any?>
        return PatternsPayload(version = version, patterns = list, meta = meta)
    }

    private fun fromJson(json: String): PatternsPayload {
        val obj = JSONObject(json)
        val version = if (obj.has("version")) obj.getInt("version") else null
        val arr = obj.getJSONArray("patterns")
        val list = mutableListOf<Pattern>()
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val id = p.optString("id")
            val category = p.optString("category", "default")
            val severity = p.optString("severity", "medium")
            val itemsAny = p.opt("tokensOrPhrases")
            val items = when (itemsAny) {
                is JSONArray -> (0 until itemsAny.length()).map { idx -> itemsAny.getString(idx) }
                else -> emptyList()
            }
            list.add(Pattern(id, category.lowercase(), severity.lowercase(), items.map { it.lowercase() }))
        }
        val metaObj = obj.optJSONObject("meta")
        val meta: Map<String, Any?>? = metaObj?.let { m ->
            val keys = m.keys()
            val map = mutableMapOf<String, Any?>()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = m.opt(k)
            }
            map
        }
        return PatternsPayload(version = version, patterns = list, meta = meta)
    }
}
