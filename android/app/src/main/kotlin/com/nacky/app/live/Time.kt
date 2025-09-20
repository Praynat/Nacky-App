package com.nacky.app.live

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/** Provides current monotonic time (ms). */
interface NowProvider { fun nowMs(): Long }

class RealNowProvider : NowProvider { override fun nowMs(): Long = SystemClock.elapsedRealtime() }

/** Simple debounce scheduler abstraction to allow deterministic tests. */
interface DebounceScheduler {
    fun schedule(delayMs: Long, run: () -> Unit)
    fun cancelAll()
}

/** Production scheduler backed by Android Handler (main looper). */
class RealDebounceScheduler : DebounceScheduler {
    private val handler: Handler? = try { Handler(Looper.getMainLooper()) } catch (_: Throwable) { null }
    private val tasks = mutableListOf<Runnable>()
    override fun schedule(delayMs: Long, run: () -> Unit) {
        val h = handler ?: return
        var ref: Runnable? = null
        val r = Runnable {
            // Remove this runnable from our tracking list before executing to avoid leaks.
            tasks.remove(ref)
            run()
        }.also { created -> ref = created }
        tasks.add(r)
        h.postDelayed(r, delayMs)
    }

    override fun cancelAll() {
        val h = handler ?: return
        for (r in tasks) h.removeCallbacks(r)
        tasks.clear()
    }
}

/** Test scheduler stores tasks; only runs when runAll() called. */
class TestDebounceScheduler : DebounceScheduler {
    private data class Pending(val delay: Long, val run: () -> Unit)
    private val pending = mutableListOf<Pending>()
    override fun schedule(delayMs: Long, run: () -> Unit) { pending.add(Pending(delayMs, run)) }
    override fun cancelAll() { pending.clear() }
    fun runAll() {
        val snapshot = pending.toList()
        pending.clear()
        snapshot.forEach { it.run() }
    }
    fun hasTasks(): Boolean = pending.isNotEmpty()
}

/** Mutable test clock. */
class TestNowProvider(start: Long = 0L) : NowProvider {
    private var now = start
    override fun nowMs(): Long = now
    fun advance(delta: Long) { now += delta }
}