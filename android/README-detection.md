# Nacky Detection Pipeline (Android)

This document gives a concise technical overview ("one page") of the word / phrase detection stack implemented in the Android module.

## 1. Conceptual Pipeline (Streaming Typing)
```
Raw keystrokes / accessibility text events
  | (Normalization: Unicode NFD, case-fold, leet / homoglyph mapping, diacritics strip)
  v
Tokenizer (script + word boundary aware) -> token stream
  v
Step 1  (Token / Phrase Candidate Collection)
  - Fast prefix & phrase lookup via TokenTrie (single words + multi‑token patterns)
  - Produces raw matches referencing pattern metadata (id, severity, category, source)
  v
Step 2  (Surface Variants & Recomposition)
  - Handles separators (spaces, punctuation, zero-width), leet substitutions, spaced letters
  - Aho‑Corasick style traversal over a recomposed normalized buffer
  - Emits normalized candidate hits with positional + pattern context
  v
Step 3  (Rule Engine → Decision(action, reason, host, term, severity))
  - Applies ordered rules R1..R7 (+ R6B) to produce a single decisive outcome per candidate
  v
Emission Layer (LiveTypingDetector)
  - Debounce & boundary finalize
  - Suppression of benign ALLOW decisions (R6, R6B, selective R5) to reduce UI noise
  v
Monitoring Aggregator (MonitoringEngine)
  - Counts detections according to CountMode & cooldown thresholds
```

## 2. Step 3 Rule Set & Ordering
The Step3Engine processes each candidate once, evaluating rules in strict order until one fires:

| Order | Code | Intent | Effect |
|-------|------|--------|--------|
| 1 | R2 | Explicit phrase allow / whitelist | ALLOW |
| 2 | R1 | Explicit phrase block (highest intent) | BLOCK |
| 3 | R3 | Word allow (lexicon SAFE single terms) | ALLOW |
| 4 | R5 | Lexicon allow (safe host containing risky fragment) | ALLOW |
| 5 | R6B | Partial host allow (risky term only at prefix/suffix with surrounding letters on other side) | ALLOW |
| 6 | R4 | Severity escalation (e.g., high severity pattern) | BLOCK |
| 7 | R7 | Segmentation / composition artifacts (fallback refine) | BLOCK or ALLOW (context) |
| 8 | R6 | Long benign host allow (contains risky fragment but clearly different word) | ALLOW |
| 9 | FALLBACK | No prior rule matched | BLOCK (conservative) |

Notes:
- R6B was introduced to reduce false positives like "assistant" triggering on substring "ass" while preserving high‑severity true positives elsewhere.
- Ordering ensures permissive context (R5/R6B) can neutralize harsh severity (R4) when the broader token proves benign.
- Live emission layer may suppress certain ALLOW outcomes (R6, R6B, and some R5) to avoid clutter while the internal reasoning is still testable.

## 3. Decision Object (Simplified)
```
Decision(
  action: ALLOW | BLOCK,
  reason: R1|R2|R3|R4|R5|R6|R6B|R7|FALLBACK,
  host:   String?  // full normalized token/phrase container
  term:   String   // matched risky canonical term
  severity: low|medium|high
)
```

## 4. Monitoring vs Live Detection
- LiveTypingDetector: Real-time incremental evaluation while user types; applies debounce and boundary heuristics. Suppresses benign allows to surface only actionable or interesting detections downstream (e.g., UI, guardian module).
- MonitoringEngine: Consumes final Detection events; applies CountMode and cooldown to aggregate / rate-limit notifications.
  - CountMode.ALL_MATCHES: every emitted detection counts
  - CountMode.UNIQUE_PER_SNAPSHOT (example): distinct pattern ids per window
  - Cooldown prevents alert storms by enforcing a min interval between identical pattern detections.

## 5. Step 2 Surface Variant Handling
Key behaviors enabling robust matching:
- Collapses sequences like `a s s` → `ass` (spaced letters)
- Maps leet / homoglyph characters (`@`→`a`, `1`→`l` or `i` contextually)
- Ignores benign separators (dash, underscore, dots) inside candidate windows
- Maintains offset mapping so final decisions can reference original positions if extended later.

## 6. Suppression Layer Rationale
Certain ALLOW decisions are explanatory (why something was not blocked) but create noise in a streaming UI. Suppression rules remove low-signal allows while retaining BLOCKS and critical ALLOW (e.g., explicit whitelists) for audit.

## 7. Testing Overview
Representative test suites:
- Step2EngineTest: Separator & variant recomposition
- Step3EngineTest: Rule ordering correctness & reason codes
- LiveTypingDetectorTest: Debounce, boundary finalize, false positive suppression ("assistant", spaced letters, phrases)
- MonitoringEngineTest: Counting & cooldown semantics
- TokenTrieTest, TextFoundationTest: Core normalization & trie behavior

## 8. Extending the System
When adding a new rule:
1. Insert at the correct semantic priority (update this doc & Step3EngineTest expectations).
2. Provide unit tests asserting both the decision.action and reason ordering.
3. Consider whether Live suppression should hide the new ALLOW reason.

---
_Last updated: 2025-09-21_
