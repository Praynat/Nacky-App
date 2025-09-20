package com.nacky.app.persistence

import android.content.Context
import android.util.Log
import com.nacky.app.DetectionSettings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/** Persistence for detection settings. */
object SettingsStore {
    // Shared Json instance (lenient & ignoring unknown keys for forward compatibility)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun save(ctx: Context, settings: DetectionSettings) = saveToRoot(DetectionFiles.detectionDir(ctx), settings)
    fun load(ctx: Context): DetectionSettings? = loadFromRoot(DetectionFiles.detectionDir(ctx))

    // In-memory serialization helpers for JVM unit tests
    fun serialize(settings: DetectionSettings): String = json.encodeToString(settings)
    fun deserialize(text: String): DetectionSettings? = try { json.decodeFromString<DetectionSettings>(text).normalized() } catch (_: Throwable) { null }

    fun saveToRoot(rootDir: File, settings: DetectionSettings) {
        try {
            if (!DetectionFiles.ensureDir(rootDir)) return
            val file = File(rootDir, "detection_settings.json")
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            val encoded = serialize(settings)
            try { file.writeText(encoded) } catch (_: Throwable) { SafeIO.writeAtomic(file, encoded) }
            if (!file.exists()) println("[SettingsStore][TEST] file not created: ${file.absolutePath}")
        } catch (e: Throwable) {
            try { Log.w("Nacky", "SettingsStore save failed: ${e.message}") } catch (_: Throwable) { println("[SettingsStore] save failed: ${e.message}") }
        }
    }

    fun loadFromRoot(rootDir: File): DetectionSettings? {
        val file = File(rootDir, "detection_settings.json")
        val txt = SafeIO.readText(file) ?: return null
        return try {
            deserialize(txt)
        } catch (e: Throwable) {
            try { Log.w("Nacky", "SettingsStore load failed: ${e.message}") } catch (_: Throwable) { println("[SettingsStore] load failed: ${e.message}") }
            null
        }
    }
}
