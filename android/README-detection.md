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

## 9. Android Persistence
The Android module persists both detection patterns and detection settings so they survive process death and service restarts.

### File Locations
All persistence lives under the app private storage directory:
```
<filesDir>/detection/
  ├─ patterns.json
  └─ detection_settings.json
```
Files are written atomically (temp write + rename fallback) via a SafeIO helper to minimize partial write risk.

### Serialization
Implemented with `kotlinx-serialization` (no reflection, fast, stable across JVM & Android unit tests). Unknown keys are ignored for forward compatibility.

#### detection_settings.json schema
```jsonc
{
  "liveEnabled": Boolean,
  "monitoringEnabled": Boolean,
  "minOccurrences": Int,
  "windowSeconds": Long,
  "cooldownMs": Long,
  "countMode": "ALL_MATCHES" | "UNIQUE_PER_SNAPSHOT",
  "blockHighSeverityOnly": Boolean,
  "debounceMs": Long
}
```

#### patterns.json schema
```jsonc
{
  "version": Int?,            // optional version marker for future migrations
  "patterns": [
    {
      "id": String,
      "category": String,     // stored as provided (matching normalization happens at runtime)
      "severity": String,     // low|medium|high
      "tokensOrPhrases": [ String, ... ]
    }
  ],
  "meta": {                   // optional metadata map (string -> string)
     "source": String?,
     // ... other string keys allowed, unknown keys ignored when reading later
  }
}
```

### Load Sequence (Service Boot)
On `AccessibilityService.onServiceConnected`:
1. Load `detection_settings.json` (if present) into `DetectionSettingsStore`.
2. Load `patterns.json` only if the in-memory repository is still empty (avoids overwriting a just-pushed live update).
3. Rebuild monitoring & live engines so token tries reflect restored patterns.
4. Apply settings to `MonitoringEngine` & `LiveTypingDetector` (debounce, thresholds, modes).

### Privacy & Safety
- No raw user / snapshot text is ever persisted. Only pattern definitions and numeric / boolean thresholds are written.
- Logs at boot summarize counts & flags (pattern count, version) without reproducing sensitive content.
- Corrupted files fail gracefully: deserialization returns null and the system proceeds with defaults or waits for a new push from Flutter.

## 10. Troubleshooting Persistence
| Scenario | Symptom | Behavior | Action |
|----------|---------|----------|--------|
| Missing files (first run / cleared storage) | No patterns loaded at boot | Engines build with empty tries until a push arrives | Push patterns from Flutter; optional: verify `patterns.json` created afterward |
| Corrupted `patterns.json` | Load returns null | Patterns remain empty; log line shows `patternsLoaded=false` | Trigger a resend from Flutter; corruption is not fatal |
| Corrupted `detection_settings.json` | Settings revert to defaults | Monitoring / live may use conservative defaults | Resend settings payload; file will be overwritten |
| Stale patterns after update | Old matches still appear | Live engines built before update | Ensure update call triggers persistence + consider forcing service reconnect or add explicit engine rebuild API |
| Unexpected pattern count at boot | Log shows fewer entries | Possibly repo already populated before persistence load or previous save failed | Check that update path called `PatternsStore.save`; re‑push patterns |

### Quick Checks
1. Inspect directory via `adb shell ls /data/data/<appId>/files/detection`.
2. Cat the JSON (do NOT share externally) to verify schema keys only.
3. If missing or corrupt, push patterns/settings again from Flutter UI.

---
_Last updated: 2025-09-21_

## 11. Startup Optimization: Signature-Based Rebuild Skipping
To reduce cold-start overhead, the service computes a lightweight integer signature on connect:

Signature components:
- Hash of current pattern list (ids + token sequences)
- Key detection settings influencing engine structure / behavior: `minOccurrences`, `windowSeconds`, `cooldownMs`, `countMode`, `blockHighSeverityOnly`, `debounceMs`, `liveEnabled`, `monitoringEnabled`, plus pattern count.

If the newly computed signature matches the previously applied one, expensive trie / engine rebuilds are skipped. This avoids redundant allocation and hashing work on process restarts where no functional change occurred (e.g., only meta timestamps differed). A unit test (`EnginesSignatureTest`) asserts that modifying only non‑signature metadata (like a timestamp in `meta`) does not trigger a rebuild.

When patterns or relevant settings change, the signature changes and rebuild occurs automatically, ensuring correctness is preserved while optimizing the steady state.
