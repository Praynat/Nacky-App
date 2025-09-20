package com.nacky.app

import com.nacky.app.engine.TokenTrie
import com.nacky.app.engine.Step2Engine
import com.nacky.app.engine.Step2Settings
import com.nacky.app.engine.Step3Engine
import com.nacky.app.engine.Step3Config
import com.nacky.app.lexicon.LexiconLoader
import com.nacky.app.live.LiveSettings
import com.nacky.app.live.LiveTypingDetector
import com.nacky.app.monitoring.CountMode
import com.nacky.app.monitoring.MonitoringEngine
import com.nacky.app.monitoring.MonitoringSettings
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.text.TextNormalizer
import com.nacky.app.text.Tokenizer
import java.util.concurrent.atomic.AtomicReference

/** Factory for building engines (monitoring, etc.). */
object Engines {
    private val monitoringRef = AtomicReference<MonitoringEngine?>(null)
    private val liveDetectorRef = AtomicReference<LiveTypingDetector?>(null)
    // Signature of last applied pattern+settings state to avoid unnecessary rebuilds.
    private val lastAppliedSignature = AtomicReference<Int?>(null)

    /** Compute stable signature from pattern repo + settings subset. */
    fun computeSignature(settings: DetectionSettings? = null): Int {
        val s = settings ?: try { DetectionSettingsStore.current.get() } catch (_: Throwable) { null }
        val patterns = PatternRepository.all()
        val version = try {
            // pattern version not exposed directly; derive via categories+ids hash for stability
            patterns.hashCode()
        } catch (_: Throwable) { 0 }
        val patternCount = patterns.size
        // Only include fields that affect engine structure or runtime filtering semantics.
        val key = listOf(
            version.toString(),
            patternCount.toString(),
            s?.minOccurrences?.toString() ?: "",
            s?.windowSeconds?.toString() ?: "",
            s?.cooldownMs?.toString() ?: "",
            s?.countMode ?: "",
            s?.blockHighSeverityOnly?.toString() ?: "",
            s?.debounceMs?.toString() ?: "",
            s?.liveEnabled?.toString() ?: "",
            s?.monitoringEnabled?.toString() ?: "",
        ).joinToString("|")
        return key.hashCode()
    }

    /** Returns true if signature changed (and updates stored signature). */
    fun shouldRebuild(signature: Int): Boolean {
        val prev = lastAppliedSignature.get()
        if (prev == signature) return false
        lastAppliedSignature.set(signature)
        return true
    }

    fun buildMonitoring(): MonitoringEngine {
        val normalizer: (String) -> String = TextNormalizer::normalize
        val tokenizer: (String) -> List<String> = { s: String -> Tokenizer.tokenizeUnicode(s).map { it.text } }
        val entries = PatternRepository.pretokenizedEntries(normalizer, tokenizer)
        val trie = TokenTrie.build(entries)
        val engine = MonitoringEngine(
            trie = trie,
            normalizer = normalizer,
            tokenizer = tokenizer,
            settings = MonitoringSettings(
                minOccurrences = 3,
                windowSeconds = 300,
                cooldownMs = 10_000,
                countMode = CountMode.UNIQUE_PER_SNAPSHOT
            )
        )
        monitoringRef.set(engine)
        return engine
    }

    fun buildLive(): Triple<TokenTrie, Step2Engine, Step3Engine> {
        val normalizer: (String) -> String = TextNormalizer::normalize
        val tokenizer: (String) -> List<String> = { s: String -> Tokenizer.tokenizeUnicode(s).map { it.text } }
        val entries = PatternRepository.pretokenizedEntries(normalizer, tokenizer)
        val trie = TokenTrie.build(entries)
        val terms = PatternRepository.singleWordTerms(normalizer, tokenizer)
        val step2 = Step2Engine(terms, Step2Settings())
        val locale = try { android.content.res.Resources.getSystem().configuration.locales[0].language } catch (_: Throwable) { "en" }
        val lexicon = LexiconLoader.load(locale)
        val step3 = Step3Engine(lexicon, Step3Config())
        return Triple(trie, step2, step3)
    }

    fun registerLiveDetector(detector: LiveTypingDetector) { liveDetectorRef.set(detector) }

    fun updateLiveSettings(new: LiveSettings) { liveDetectorRef.get()?.updateSettings(new) }
    fun updateMonitoringSettings(new: MonitoringSettings) { monitoringRef.get()?.updateSettings(new) }
}
