package com.nacky.app.persistence

import android.content.Context
import android.util.Log
import com.nacky.app.patterns.Pattern
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/** Persistence for pattern definitions (no runtime user text). */
object PatternsStore {
    @Serializable
    data class Snapshot(val version: Int? = null, val patterns: List<Pattern> = emptyList(), val meta: Map<String, String?>? = null)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // In-memory serialization helpers for JVM unit tests
    fun serialize(snapshot: Snapshot): String = json.encodeToString(snapshot)
    fun deserialize(text: String): Snapshot? = try { json.decodeFromString<Snapshot>(text) } catch (_: Throwable) { null }

    fun save(ctx: Context, snapshot: Snapshot) = saveToRoot(DetectionFiles.detectionDir(ctx), snapshot)
    fun load(ctx: Context): Snapshot? = loadFromRoot(DetectionFiles.detectionDir(ctx))

    fun saveToRoot(rootDir: File, snapshot: Snapshot) {
        try {
            if (!DetectionFiles.ensureDir(rootDir)) return
            val file = File(rootDir, "patterns.json")
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            val encoded = serialize(snapshot)
            // Direct write (tests run on JVM without Android fs constraints)
            try { file.writeText(encoded) } catch (_: Throwable) { SafeIO.writeAtomic(file, encoded) }
            if (!file.exists()) println("[PatternsStore][TEST] file not created: ${file.absolutePath}")
        } catch (e: Throwable) {
            try { Log.w("Nacky", "PatternsStore save failed: ${e.message}") } catch (_: Throwable) { println("[PatternsStore] save failed: ${e.message}") }
        }
    }

    fun loadFromRoot(rootDir: File): Snapshot? {
        val file = File(rootDir, "patterns.json")
        val txt = SafeIO.readText(file) ?: return null
        return try {
            deserialize(txt)
        } catch (e: Throwable) {
            try { Log.w("Nacky", "PatternsStore load failed: ${e.message}") } catch (_: Throwable) { println("[PatternsStore] load failed: ${e.message}") }
            null
        }
    }
}
