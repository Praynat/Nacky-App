# Nacky-App Project Context Snapshot
Last Updated: 2025-09-05  
Maintainer: @Praynat  

This document is a living, modular overview of the current state of Nacky-App.  
You can hand this to another AI assistant so it understands what already exists and what still needs to be built.  
Sections are divided into:
- STABLE (unlikely to change often)
- EVOLVING (you will likely update as you add features)
- PLACEHOLDERS (intentionally empty or partially drafted for future expansion)

Use the “Update Checklist” near the bottom when you return to refresh this file.

---

## 1. High-Level Purpose (STABLE)
Nacky-App is a parental control / digital safety application focused on:
- Detecting forbidden / explicit / harmful words or phrases across the device UI (currently Android via Accessibility Service).
- Managing user-provided forbidden words alongside a curated default list.
- Providing administrative (guardian) gated management screens (PIN protected).
- Expanding toward broader supervision domains (apps, emails, future web/content filtering).

Primary design goals:
1. Strong word/phrase detection coverage (multilingual, multi-token, obfuscation-resilient in future).
2. Minimize false positives via normalization and structured matching (in-progress).
3. Extensible enforcement actions (currently limited; roadmap includes warnings, blocking, reporting).
4. Cross-platform architecture (Flutter UI + native platform services).

---

## 2. Technology Stack (STABLE)
| Layer | Tech |
|-------|------|
| UI / Cross-platform | Flutter (Dart) |
| Android Native Integration | Kotlin (AccessibilityService, MethodChannel bridge) |
| Local Storage | SharedPreferences (user lists & config) |
| Assets | `assets/DefaultWords.txt` (default forbidden list) |
| Build / Native Support | Some C++/CMake present (future extension; not yet central) |
| Normalization | Dart utility + Kotlin extension (lowercase + Unicode NFD diacritic strip) |

---

## 3. Current Functional Modules (EVOLVING)
| Module | Status | Key Files |
|--------|--------|-----------|
| Default word list ingestion | Functional | `assets/DefaultWords.txt`, `lib/features/words/words_repo.dart` |
| User word management (add/edit/remove) | Functional | `words_add_screen.dart`, `words_manage_screen.dart` |
| Word list normalization & merging | Functional (basic) | `words_repo.dart`, Android `MainActivity.kt` |
| Guardian PIN gating for word management | Functional | `lib/features/guardian/pin_dialog.dart` (implied) |
| Accessibility-based text scanning | Functional (baseline) | `NackyAccessibilityService.kt` |
| Token-based exact word detection | Functional (single-token only) | `NackyAccessibilityService.kt` |
| Multi-word phrase detection | NOT IMPLEMENTED (gap) | (planned) |
| Obfuscation handling (leetspeak/spaces) | NOT IMPLEMENTED | (planned) |
| Multi-script retention (non a–z0–9) | PARTIAL (lost due to token regex) | `NackyAccessibilityService.kt` |
| App blocking feature (UI stub) | UI present, logic incomplete | `apps_screen.dart` |
| Email list management (structure) | Functional (CRUD) | `emails_repo.dart` |
| Enforcement actions (blocking, warnings) | Not implemented (only counting) | (planned) |
| Logging / analytics of detections | Not implemented | (planned) |
| Word severity / categorization | Not implemented | (planned) |

---

## 4. Data Assets & Lists (STABLE/EVOLVING)
- Default Words File: Contains sexual, slur, violent, and explicit content phrases.
- Format: Plain text, one phrase/word per line; includes multi-word phrases.
- User List: Stored in SharedPreferences under key `words_user_v1`.
- Normalization applied:
  - Lowercase
  - Unicode NFD → strip combining marks
- Limitation: Multi-word phrases currently stored but not matched as phrases in the detection engine.

---

## 5. Normalization Pipeline (CURRENT STATE) (EVOLVING)
Implemented:
- Dart: `normalizeWord()` (in `core/normalize.dart` implicitly).
- Kotlin: `String.normalizeWord()` (lowercase + NFD + remove `\p{Mn}` marks).
Missing / Planned:
- Unicode word boundary aware tokenization.
- Optional leetspeak mapping (`0→o`, `@→a`, etc.) with toggle.
- Homoglyph / script confusable reduction.
- Handling of spacing / punctuation inside phrases (“b l o w”, “bl*ow”).

---

## 6. Word Detection Engine (CURRENT BASELINE) (EVOLVING)
Current Flow (Android):
1. Flutter gathers combined list (default + user) → sends to Android via `sendWordList`.
2. Kotlin stores set in `ForbiddenStore.words` (lowercased).
3. Accessibility traversal (`dfs`) collects text and content descriptions.
4. Text normalized → split by regex `[^a-z0-9]+` → tokens.
5. For each token: membership check in set.
6. A simple counter of matches returned (no enforcement logic integrated).

Gaps vs Target:
- Phrases not recognized (e.g., “reverse cowgirl” split into tokens “reverse”, “cowgirl”).
- Non-Latin scripts effectively discarded (split regex excludes them).
- No contextual or multi-hit scoring.
- No sliding window / sequence automaton.

---

## 7. UI Feature Overview (STABLE/EVOLVING)
Screens:
- Dashboard: Quick actions (add forbidden word, block app, add email).
- Add Forbidden Word (`words_add_screen.dart`): Text input + add; shows helper text about management.
- Manage Words (`words_manage_screen.dart`): List (user-added only), edit/delete (PIN gated).
- Manage Apps (`apps_screen.dart`): Filterable list; ability to “block” (implementation pending).
- Manage Emails (`emails_repo.dart` + related screens): CRUD similar to words.

Navigation: Using `go_router` style routes (implied by `context.push('/words')` etc.).

Guardian Controls:
- PIN gating invoked before editing/removing sensitive resources.

---

## 8. Android Accessibility Service (DETAIL) (EVOLVING)
File: `NackyAccessibilityService.kt`
Responsibilities:
- Traverse node tree (depth-first).
- Aggregate `text` + `contentDescription`.
- Normalize segment + tokenize.
- Count token matches (currently both `matchesForbidden()` and `countMatches()` use membership logic).
Constraints:
- Real-time keystroke-level intent (pre-commit) not visible (only committed UI).
Planned Evolution:
- Introduce multi-token sliding sequence detection.
- Add enforcement callback (e.g., overlay blur / block).
- Rate limiting & event classification.

---

## 9. Persistence & Configuration (STABLE/EVOLVING)
- SharedPreferences used for:
  - User word list
  - (Emails, blocked apps lists similarly)
- Potential expansions:
  - Structured JSON for pattern metadata (severity, category).
  - Migration version keys (e.g., `words_user_v2` when schema changes).
  - Local encrypted storage for sensitive logs (if added).

---

## 10. Security & Privacy (BASELINE) (EVOLVING)
Current:
- Minimal sensitive logging (effectively none).
- PIN required for modifications.

Planned:
- Hash-only detection logs (avoid raw text).
- Guardian audit history (rotating).
- Opt-in telemetry boundary (explicit consent).
- Export/import with checksum.

---

## 11. Current Limitations (CONSOLIDATED) (EVOLVING)
1. Multi-word phrase detection absent.
2. No enforcement actions (only detection potential).
3. Tokenization excludes non-Latin scripts.
4. No severity classification or thresholds.
5. No obfuscation resilience (leetspeak/spaces/punctuation).
6. No false-positive mitigation (whitelists, context scoring).
7. No logging/audit pipeline.
8. No network/web content filtering.
9. No incremental automaton structure (performance may degrade later).
10. No unified pattern metadata format.

---

## 12. Immediate Next Priority Recommendations (EVOLVING)
(Ordered for maximal leverage)

1. Replace token splitting regex with Unicode-aware segmentation.
2. Introduce pattern compiler (Dart) that:
   - Tokenizes each phrase
   - Marks phrase length
   - Emits a JSON metadata blob (future severity).
3. On Android: Build token-sequence matcher (Aho-Corasick or trie with failure links).
4. Add phrase detection before adding obfuscation complexity.
5. Add severity scaffolding (default all “medium”; allow overrides later).
6. Implement simple enforcement stub (log + optional notification).
7. Add internal debug view (counts, last N detections) hidden behind PIN.

---

## 13. Forward Roadmap (PHASED) (EVOLVING)
| Phase | Title | Core Additions |
|-------|-------|----------------|
| 0 | Phrase Baseline | Multi-token detection, Unicode tokens |
| 1 | Structured Patterns | Severity metadata, category taxonomy |
| 2 | Enforcement | Overlay / block actions, cooldown logic |
| 3 | Obfuscation Resilience | Leetspeak, spaced letters, minor edit distance |
| 4 | Context Intelligence | Co-occurrence scoring, risk thresholds |
| 5 | Network & Clipboard | VPN text scanning, optional clipboard monitor |
| 6 | Analytics & Reports | Aggregated stats, export, guardian dashboard |
| 7 | Internationalization+ | Multi-script expansions, transliteration modules |

---

## 14. Proposed Pattern Metadata (PLACEHOLDER)
```jsonc
{
  "version": 1,
  "patterns": [
    {
      "raw": "reverse cowgirl",
      "tokens": ["reverse", "cowgirl"],
      "severity": "high",
      "category": "sexual",
      "flags": []
    }
  ]
}
```
(To implement in later phase.)

---

## 15. Glossary (STABLE/EVOLVING)
- Pattern: A forbidden word or multi-word phrase (normalized+tokenized).
- Token: A normalized lexical unit derived from raw UI text.
- Phrase Pattern: A pattern with token length > 1.
- Enforcement: Action taken when detection exceeds threshold (planned).
- Normalization: Lowercase + diacritic removal + (future) leetspeak mapping.

---

## 16. Quick File Reference (STABLE/EASY TO EXTEND)
| File | Purpose |
|------|---------|
| `assets/DefaultWords.txt` | Seed list of forbidden words/phrases |
| `lib/features/words/words_repo.dart` | Load + normalize + merge word lists |
| `lib/features/words/words_add_screen.dart` | Add user word UI |
| `lib/features/words/words_manage_screen.dart` | Manage (edit/delete) user words |
| `lib/features/dashboard/dashboard_screen.dart` | Main dashboard quick actions |
| `lib/features/apps/apps_screen.dart` | App blocking UI (logic pending) |
| `lib/features/emails/emails_repo.dart` | Email list persistence |
| `android/app/src/main/.../MainActivity.kt` | Flutter ↔ Android bridge; receives words |
| `android/app/src/main/.../NackyAccessibilityService.kt` | Accessibility scanning & simple detection |
| `core/normalize.dart` (implied) | Dart normalization utility |
| (Future) `lib/patterns/pattern_compiler.dart` | Planned pattern metadata builder |
| (Future) `android/.../PatternMatcher.kt` | Planned trie-based phrase matcher |

---

## 17. Update Checklist Template (REUSE THIS WHEN UPDATING) (STABLE)
When you return to update this file, search for “[UPDATE ME]” tags you may add, and follow this checklist:

- [ ] Updated “Last Updated” date.
- [ ] Adjusted Module Status Table (Section 3).
- [ ] Added/removal of files in Quick File Reference.
- [ ] Updated Limitations list if resolved.
- [ ] Moved completed roadmap items upward & marked as “DONE (date)”.
- [ ] Added new planned phases if scope expanded.
- [ ] (Optional) Snapshot diff of detection coverage metrics.

---

## 18. Rapid “What’s Implemented vs Not” Summary (CHEAT SHEET) (EVOLVING)
Implemented Core: Seed + user words, normalization (basic), token detection (single-token), PIN gating, UI CRUD (words/emails), baseline accessibility scanning.
Missing Core: Phrase detection, enforcement, multi-script retention, obfuscation handling, severity scoring, analytics, network filtering.

---

## 19. Open Design Questions (PLACEHOLDER)
(Add concrete questions here for future assistant sessions, e.g.):
- Best structure for cross-platform automaton sharing?
- Where to store severity metadata (sidecar JSON vs inline DSL)?
- Approach for partial obfuscation without exploding false positives?

---

## 20. Changelog (EVOLVING)
| Date | Change | Notes |
|------|--------|-------|
| 2025-09-05 | Initial snapshot created | Baseline extracted from repos |

---

## 21. Next Suggested Concrete Actions (ACTIONABLE) (EVOLVING)
1. Implement Unicode tokenization + preserve non-Latin (Section 12 #1).
2. Draft pattern compiler (Dart) that outputs token arrays (Section 14).
3. Kotlin phrase matcher skeleton (ready for multi-token).
4. Add phrase detection test fixtures (short UI strings).
5. Add debugging overlay listing last 10 matches (guardian view only).

---

(End of Snapshot)