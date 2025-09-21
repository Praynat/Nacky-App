# Word Detection Inventory (Post Legacy Removal)

Branch: `copilot/fix-422b1d63-746e-4da1-8d0c-d1a31c32d120`
Date: 2025-09-21

**Legacy removed**: ForbiddenStore + sendWordList deprecated in favor of unified pattern pipeline.

## Dart / Flutter Layer

- `lib/core/normalize.dart` – Normalization helper: lowercase, pseudo-NFD decomposition, strip diacritics, collapse whitespace.
- `lib/features/words/words_repo.dart` – Loads seed list (`assets/DefaultWords.txt`), persists user list via SharedPreferences, merges (seed ∪ user) for filter push, normalizes on add/remove.
- `lib/features/words/words_add_screen.dart` – UI to add words; after add sends pattern payload to Android via `AndroidBridge.updatePatternsFull`.
- `lib/features/words/words_manage_screen.dart` – UI for viewing/editing/removing user words; on changes re-sends pattern payload to Android (guardian PIN gate).
- `lib/features/dashboard/dashboard_screen.dart` – On init (via `_pushWordsToAndroid`) sends current pattern payload to Android service.
- `lib/core/platform/android_bridge.dart` – MethodChannel wrapper (`nacky/android`) exposing `updatePatternsFull`, permission queries & requests for Accessibility Service.
- `lib/app/router.dart` – Declares routes for words add/manage screens (navigation wiring only).
- `lib/app/shell.dart` – Navigation entry providing destination for words feature (menu tile).
- `assets/DefaultWords.txt` – Seed forbidden word list shipped with app (raw text, one per line; normalized at load).

## Android (Kotlin) Layer

- `android/app/src/main/kotlin/com/nacky/app/MainActivity.kt` – Defines MethodChannel `nacky/android`; handles `updatePatterns`, calls `PatternRepository.updateFromPayload()`; exposes accessibility permission checks.
- `android/app/src/main/kotlin/com/nacky/app/NackyAccessibilityService.kt` – AccessibilityService using pattern-based detection via LiveTypingDetector for live typing and MonitoringEngine for content scanning; logs "Service connected: pattern pipeline ACTIVE (legacy removed)" on startup.

## Notable Behaviors / Coupling

- Pattern payload updates are triggered from multiple UI points (dashboard init, add screen, manage screen) using structured pattern format via `updatePatternsFull`.
- Single unified pattern pipeline: PatternRepository → Trie → Step2 Variant → Step3 Rules → LiveTypingDetector & MonitoringEngine.
- No legacy flat token matching—all detection uses multi-stage pattern engine with phrase support, normalization variants, and contextual rules.
- Pattern data persisted in PatternRepository with structured JSON payload format including categories and severity levels.

## Hard-Coded / Direct Logic

- Hard-coded seed list asset path: `assets/DefaultWords.txt` in Dart.
- Pattern-based detection using TokenTrie with efficient prefix matching and multi-token phrase support.
- Detection decisions logged with pattern ID and severity (no enforcement actions yet implemented).
- Pattern pipeline stages: Step1 (Trie) → Step2 (Variants) → Step3 (Rules) with configurable debounce settings.

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
