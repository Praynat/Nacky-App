package com.example.nacky

object ForbiddenStore {
    @Volatile var words: Set<String> = emptySet()
    @Volatile var lastBlockTs: Long = 0L

    fun shouldThrottle(ms: Long = 2000): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastBlockTs < ms) return true
        lastBlockTs = now
        return false
    }
}
