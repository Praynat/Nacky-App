package com.nacky.app

import com.nacky.app.engine.TokenTrie
import com.nacky.app.engine.Step2Engine
import com.nacky.app.engine.Step2Settings
import com.nacky.app.engine.Step3Engine
import com.nacky.app.engine.Step3Config
import com.nacky.app.lexicon.LexiconLoader
import com.nacky.app.monitoring.CountMode
import com.nacky.app.monitoring.MonitoringEngine
import com.nacky.app.monitoring.MonitoringSettings
import com.nacky.app.patterns.PatternRepository
import com.nacky.app.text.TextNormalizer
import com.nacky.app.text.Tokenizer

/** Factory for building engines (monitoring, etc.). */
object Engines {
    fun buildMonitoring(): MonitoringEngine {
        val normalizer: (String) -> String = TextNormalizer::normalize
        val tokenizer: (String) -> List<String> = { s: String -> Tokenizer.tokenizeUnicode(s).map { it.text } }
        val entries = PatternRepository.pretokenizedEntries(normalizer, tokenizer)
        val trie = TokenTrie.build(entries)
        return MonitoringEngine(
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
}
