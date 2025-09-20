# Word Detection Inventory (Post V2 Migration)

Branch: `feature/word-detection-v2`
Date: 2025-09-21

## Dart / Flutter Layer

- `lib/core/normalize.dart` – Normalization helper: lowercase, pseudo-NFD decomposition, strip diacritics, collapse whitespace.
- `lib/features/words/words_repo.dart` – Loads seed list (`assets/DefaultWords.txt`), persists user list via SharedPreferences, merges (seed ∪ user) for filter push, normalizes on add/remove.
- `lib/features/words/words_add_screen.dart` – UI to add words; after add pulls merged list and pushes to Android via `AndroidBridge.sendWordList`.
- `lib/features/words/words_manage_screen.dart` – UI for viewing/editing/removing user words; on changes re-sends merged normalized list to Android (guardian PIN gate).
- `lib/features/dashboard/dashboard_screen.dart` – On init (via `_pushWordsToAndroid`) sends current merged word list to Android service.
- `lib/core/platform/android_bridge.dart` – MethodChannel wrapper (`nacky/android`) exposing `sendWordList`, permission queries & requests for Accessibility Service.
- `lib/app/router.dart` – Declares routes for words add/manage screens (navigation wiring only).
- `lib/app/shell.dart` – Navigation entry providing destination for words feature (menu tile).
- `assets/DefaultWords.txt` – Seed forbidden word list shipped with app (raw text, one per line; normalized at load).

## Android (Kotlin) Layer

- `android/app/src/main/kotlin/com/nacky/app/MainActivity.kt` – Defines MethodChannel `nacky/android`; handles `sendWordList`, stores incoming normalized-lowercased set into `ForbiddenStore.words`; exposes accessibility permission checks.
- `android/app/src/main/kotlin/com/nacky/app/ForbiddenStore.kt` – Thread-safe (volatile) holder for current forbidden words and simple throttle timestamp logic (`shouldThrottle`).
- `android/app/src/main/kotlin/com/nacky/app/NackyAccessibilityService.kt` – AccessibilityService performing live text interception & content tree scanning; normalizes input, tokenizes on `[^a-z0-9]+`, matches tokens against `ForbiddenStore.words`, blocks by clearing input / navigating away when matches or threshold (≥3) encountered; duplicates a Kotlin-side normalization function (parallel logic to Dart version).

## Notable Behaviors / Coupling

- Word list push is manual and triggered from multiple UI points (dashboard init, add screen, manage screen); no background sync or diffing—full list resent each time.
- Normalization logic is duplicated (Dart vs Kotlin) with slightly different implementations (Kotlin uses true Unicode NFD via `Normalizer`; Dart uses limited custom decomposition table).
- Matching strategy: exact token match on ASCII-lowercased alphanumerics; non a-z0-9 separators removed; multi-word phrases not supported.
- No incremental updates—Android maintains only latest full set in memory (`Set<String>`); no persistence across process death shown (no storage of words on Android side besides runtime memory).
- Throttling prevents rapid repeated navigation actions (2s window) via `ForbiddenStore.shouldThrottle()`.

## Hard-Coded / Direct Logic

- Hard-coded seed list asset path: `assets/DefaultWords.txt` in Dart.
- Direct matching loop in `NackyAccessibilityService.matchesForbidden` & `countMatches` (linear scan over tokens, O(T) set membership checks; efficient due to HashSet usage).
- Threshold of `>= 3` forbidden tokens in view tree triggers forced navigation (hard-coded constant inside service).
- Throttle window default 2000 ms (hard-coded in `ForbiddenStore.shouldThrottle`).

## V2 Detection Pipeline Status

| Component | Status | Notes |
|-----------|--------|-------|
| Normalization (Dart + Kotlin parity subset) | Done | Kotlin now performs extended Unicode + leet handling for detection; Dart maintains basic normalization for UI & list entry. |
| Pattern Repository & TokenTrie (Step 1) | Done | Supports single & multi-token patterns, efficient prefix traversal. |
| Step 2 Engine (Variant / Recomposition) | Done | Handles spaced letters, leet, separator folding; Aho-Corasick over normalized buffer. |
| Step 3 Rule Engine (R1..R7 + R6B) | Done | Ordered rule evaluation with deterministic reason codes; false-positive reduction (assistant etc.). |
| LiveTypingDetector (Streaming) | Done | Debounce, boundary finalize, suppression of benign allows. |
| MonitoringEngine (Aggregation) | Done | CountMode support + cooldown window logic. |
| Test Suite (32 tests) | Done | All green; covers normalization, tries, step2/3 ordering, live debounce & monitoring aggregation. |

## Remaining / Future Opportunities

1. Persist pattern / lexicon data sets across restarts (currently in-memory only).
2. Expose detection telemetry channel to Flutter layer for user-facing analytics.
3. Consolidate normalization tables into a shared generated artifact to eliminate divergence.
4. Extend multi-script tokenization (CJK segmentation heuristics, RTL script nuances) beyond current scope.
5. Introduce configuration-driven rule enable/disable for experimentation (A/B false positive tuning).

---
Updated after implementing V2 migration.

---
Generated automatically prior to implementing V2 migration.
