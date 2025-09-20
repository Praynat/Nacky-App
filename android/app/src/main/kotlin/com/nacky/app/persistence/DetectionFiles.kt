package com.nacky.app.persistence

import android.content.Context
import java.io.File

/** Centralized file path helpers for detection persistence. */
object DetectionFiles {
    private const val DIR_NAME = "detection"
    private const val PATTERNS_FILE = "patterns.json"
    private const val SETTINGS_FILE = "detection_settings.json"

    // Context based helpers
    fun detectionDir(ctx: Context): File = File(ctx.filesDir, DIR_NAME)
    fun patternsFile(ctx: Context): File = File(detectionDir(ctx), PATTERNS_FILE)
    fun settingsFile(ctx: Context): File = File(detectionDir(ctx), SETTINGS_FILE)

    // Root directory (test) helpers
    fun detectionDir(rootDir: File): File = File(rootDir, DIR_NAME)
    fun patternsFile(rootDir: File): File = File(detectionDir(rootDir), PATTERNS_FILE)
    fun settingsFile(rootDir: File): File = File(detectionDir(rootDir), SETTINGS_FILE)

    internal fun ensureDir(dir: File): Boolean {
        if (dir.exists()) return dir.isDirectory
        return dir.mkdirs()
    }
}
