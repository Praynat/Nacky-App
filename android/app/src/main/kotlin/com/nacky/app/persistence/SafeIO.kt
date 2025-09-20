package com.nacky.app.persistence

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset

/** Minimal atomic write utility: write temp, fsync, rename. */
object SafeIO {
    private val UTF8: Charset = Charsets.UTF_8

    fun writeAtomic(target: File, content: String) {
        val parent = target.parentFile
        if (parent == null || (parent.exists() && !parent.isDirectory)) {
            try { Log.w("Nacky", "SafeIO write aborted: parent invalid for ${target.name}") } catch (_: Throwable) { println("[SafeIO] write abort ${target.name}") }
            return
        }
        if (parent != null && !parent.exists()) parent.mkdirs()
        val tmp = File(parent, target.name + ".tmp")
        try {
            FileOutputStream(tmp).use { fos ->
                val bytes = content.toByteArray(UTF8)
                fos.write(bytes)
                try { fos.fd.sync() } catch (_: Throwable) { }
            }
            if (!tmp.renameTo(target)) {
                // Fallback overwrite strategy
                target.delete()
                if (!tmp.renameTo(target)) {
                    // Last resort: direct write
                    try { target.writeText(content, UTF8) } catch (_: Throwable) {}
                }
            }
        } catch (e: Throwable) {
            try { Log.w("Nacky", "SafeIO write failed ${target.name}: ${e.message}") } catch (_: Throwable) { println("[SafeIO] write failed ${target.name}: ${e.message}") }
            try { tmp.delete() } catch (_: Throwable) {}
        }
    }

    fun readText(file: File): String? {
        return try {
            if (!file.exists() || !file.isFile) return null
            file.readText(UTF8)
        } catch (e: Throwable) {
            try { Log.w("Nacky", "SafeIO read failed ${file.name}: ${e.message}") } catch (_: Throwable) { println("[SafeIO] read failed ${file.name}: ${e.message}") }
            null
        }
    }
}
