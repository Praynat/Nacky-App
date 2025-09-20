package com.nacky.app.engine

import com.nacky.app.lexicon.LexiconLoader
import com.nacky.app.text.TextNormalizer
import org.junit.Assert.*
import org.junit.Test

class Step3EngineTest {

    private fun norm(s: String) = TextNormalizer.normalize(s)

    private fun cand(term: String, host: String, severity: String = "low"): Candidate {
        val n = norm(host)
        // Find indices of term in normalized host (first occurrence)
        val idx = n.indexOf(term)
        require(idx >= 0) { "term not in host for test setup" }
        return Candidate(patternId = "p", term = term, severity = severity, startOrig = idx, endOrig = idx + term.length - 1)
    }

    private val lexicon = LexiconLoader.load("en") // includes assistant, look, this, is, good
    private val frLex = LexiconLoader.load("fr")
    private val heLex = LexiconLoader.load("he")

    // a) Exact word -> R1
    @Test fun exactWordBlocks() {
        val engine = Step3Engine(lexicon)
        val n = norm("ass")
        val c = Candidate("p","ass","low",0,2)
        val d = engine.decide(n,c)
        assertEquals(Action.BLOCK, d.action)
        assertEquals("R1", d.reason)
    }

    // b) Prefix / suffix boundary R2
    @Test fun boundaryBlocksPrefix() {
        val engine = Step3Engine(lexicon)
        val n = norm("ass.")
        val c = Candidate("p","ass","low",0,2)
        val d = engine.decide(n,c)
        assertEquals("R2", d.reason)
    }

    @Test fun boundaryBlocksSuffix() {
        val engine = Step3Engine(lexicon)
        val n = norm("(ass")
        val c = Candidate("p","ass","low",1,3)
        val d = engine.decide(n,c)
        assertEquals("R2", d.reason)
    }

    // c) Short host dominated R3
    @Test fun shortHostDominated() {
        val engine = Step3Engine(lexicon)
        val host = norm("asse")
        val c = Candidate("p","ass","low",0,2)
        val d = engine.decide(host,c)
        assertEquals("R3", d.reason)
    }

    // d) Safe long host middle substring R6 (assistant contains ass inside letters)
    @Test fun longHostAllows() {
        val engine = Step3Engine(lexicon)
        val host = norm("compassion") // contains "ass" in middle, not in lexicon
        val idx = host.indexOf("ass")
        val c = Candidate("p","ass","low",idx, idx+2)
        val d = engine.decide(host,c)
    // debug print removed
        assertEquals(Action.ALLOW, d.action)
    }

    // e) Lexicon safe word R5 (FR culotte with term cul)
    @Test fun lexiconSafeFrench() {
        val engine = Step3Engine(frLex)
        val host = norm("culotte")
        val idx = host.indexOf("cul")
        val c = Candidate("p","cul","low",idx, idx+2)
        val d = engine.decide(host,c)
        assertEquals(Action.ALLOW, d.action)
        assertEquals("R5", d.reason)
    }

    // f) Segmentation blocks (lookthisassisgood -> segmentation contains standalone ass)
    @Test fun segmentationBlocks() {
        val engine = Step3Engine(lexicon)
        val host = norm("lookthisassisgood")
        val idx = host.indexOf("ass")
        val c = Candidate("p","ass","low",idx, idx+2)
        val d = engine.decide(host,c)
        assertEquals(Action.BLOCK, d.action)
        assertEquals("R7", d.reason)
    }

    // g) Hebrew sample host expansion & lexicon allow
    @Test fun hebrewLexiconAllow() {
        val engine = Step3Engine(heLex)
        val host = norm("שלום") // candidate term not really present, craft candidate on subset
        // Use first two chars as fake term to simulate candidate inside known Hebrew word
        val term = host.substring(0,2)
        val c = Candidate("p", term, "low",0,1)
        val d = engine.decide(host,c)
        // Because whole host is in lexicon -> R5
        assertEquals("R5", d.reason)
        assertEquals(Action.ALLOW, d.action)
    }

    // h) Severity high blocks when not previously allowed (unknown short host)
    @Test fun severityHighBlocks() {
        val engine = Step3Engine(lexicon)
        val host = norm("assx") // not lexicon; length 4 triggers R3 earlier? Actually R3 triggers ratio 3/4=0.75 -> block R3 before R4; use longer host to bypass R3.
        val altHost = norm("asshost") // length 7 (not lexicon) contains ass at prefix boundary? prefix boundary rule R2 would block. Need host where R2 not triggered: xasshost -> term not prefix/suffix.
        val host2 = norm("xasshost")
        val idx = host2.indexOf("ass")
        val c = Candidate("p","ass","high", idx, idx+2)
        val d = engine.decide(host2,c)
        assertEquals("R4", d.reason)
        assertEquals(Action.BLOCK, d.action)
    }

    // i) Fallback allow (unknown long host with term inside but not triggering any allow earlier)
    @Test fun fallbackAllow() {
        // Disable segmentation and raise minHostLong so R6 won't apply; ensure not boundary (use digit before) to avoid R2.
        val engine = Step3Engine(lexicon, Step3Config(minHostLong = 20, segmentationEnabled = false))
    val host = norm("zaasszzq")
        val idx = host.indexOf("ass")
        val c = Candidate("p","ass","low", idx, idx+2)
        val d = engine.decide(host,c)
        assertEquals(Action.ALLOW, d.action)
        assertEquals("FALLBACK", d.reason)
    }
}
