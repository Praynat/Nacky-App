# Nacky-App Combined Roadmap
Last Updated: 2025-09-05
Owner: @Praynat

STRUCTURE:
PART 1: Broad Roadmap (by UI Page + Core Systems) — High-level comparison Old vs Current vs Missing.  
PART 2: Active Expanded Track (detailed steps) — Only one active focus (now: Word Filter Core).  
Parking / Deferred: Items not yet scheduled.  
Change Log: History of roadmap updates.

==================================================
PART 1. BROAD ROADMAP (HIGH-LEVEL COMPARISON)

Legend:
OLD = ExplicitWordMonitor (legacy Windows app)  
CURRENT = Nacky (Flutter + Android Accessibility)  
MISSING = Gap to close

Dashboard Page
- OLD: None (basic WPF main window hosting page).
- CURRENT: Dashboard with quick actions (words, apps, emails).
- MISSING: Summary metrics (detections today), alerts panel, guardian status.

Words Management Page
- OLD: Add word, list default + user, simple duplication allowed, no metadata.
- CURRENT: Add user words (user list separate), basic normalization, PIN gating.
- MISSING: Phrase awareness in UI, severity tagging, whitelist UI, analytics (usage counts).

Detection Engine (Core)
- OLD: Keyboard buffer + EndsWith + delayed regex; aggressive process kill; simple proxy filter.
- CURRENT: Accessibility snapshot → single-token membership; no phrase detection; no enforcement.
- MISSING: Multi-token phrases, Unicode tokenization, structured events, severity, whitelist, performance plan.

Enforcement
- OLD: Kill process immediately.
- CURRENT: None.
- MISSING: Graduated actions (notify / blur / block), cooldown logic, guardian alert channel.

Web / Network Filtering
- OLD: Proxy substring scanning.
- CURRENT: None.
- MISSING: Local VPN / proxy text stream → unified matcher.

Logging / Analytics
- OLD: None (implicit only).
- CURRENT: None.
- MISSING: JSON event log, severity counts, export, retention policy.

Security / Guardian
- OLD: Plain password for closing app.
- CURRENT: PIN gating for word management.
- MISSING: Secure PIN storage, audit trail, tamper resistance.

Internationalization
- OLD: Basic ASCII; regex word boundaries.
- CURRENT: Diacritic strip; non-Latin lost in token split.
- MISSING: Unicode tokenization, multi-script retention, later transliteration.

Obfuscation Handling
- OLD: None.
- CURRENT: None.
- MISSING: Leetspeak, spaced letters, symbol noise tolerance (phased).

Apps Management
- OLD: N/A (generic kill).
- CURRENT: App block UI stub.
- MISSING: Policy binding (block after X detections), enforced state.

Emails List
- OLD: N/A.
- CURRENT: CRUD list.
- MISSING: Integration into supervision policy (e.g., allowed contacts).

Privacy
- OLD: No logging.
- CURRENT: No logging.
- MISSING: Hashing strategy, export controls, consent boundaries.

Future Candidates
- Context scoring
- Adaptive thresholds
- Remote guardian dashboard
- Cloud sync (optional)
- ML-assisted risk classification (long-term)

==================================================
PART 2. ACTIVE EXPANDED TRACK: WORD FILTER CORE

Goal:
Upgrade from single-token naive detection to Unicode-aware multi-token detection with structured events, severity scaffold, whitelist, and a performance migration plan.

WF-0 (Baseline Analysis)
Status: DONE:2025-09-05
Result: Gaps identified (phrases, Unicode, logging, severity, whitelist, perf planning).

WF-1 (Unicode Tokenization)
Status: OPEN
Tasks:
  1. Implement tokenizeUnicode() (letters + combining marks + digits).
  2. Replace ASCII regex split in Android service (+ Dart if used).
  3. Add tests: “canción”, “Über”, Hebrew sample, mixed script, digits inline.
  4. Confirm legacy single-word matches still succeed.
Acceptance Criteria:
  - ≥5 tests pass.
  - Non-Latin tokens appear in debug output.
  - No regression on previous words.

WF-2 (Phrase Ingestion)
Status: OPEN
Depends: WF-1
Tasks:
  1. Normalize + tokenize into Pattern(tokens).
  2. Pattern ID = tokens joined by U+2063.
  3. Deduplicate (case-insensitive post-normalization).
  4. Test “reverse cowgirl” → ["reverse","cowgirl"].
Acceptance Criteria:
  - Multi-token patterns present.
  - Duplicates collapsed.

WF-3 (Brute Force Matcher)
Status: OPEN
Depends: WF-2
Tasks:
  1. Sliding window match phrases + single tokens in one pass.
  2. Test phrase + single word overlap scenario.
  3. Timing (<10 ms, ~150 tokens, ~500 patterns) recorded.
Acceptance Criteria:
  - Overlap test passes.
  - Timing comment/log saved.

WF-4 (Detection Events & Logging)
Status: OPEN
Depends: WF-3
Tasks:
  1. JSON lines: {ts, patternId, tokens, isPhrase, severity}.
  2. File rollover (~2 MB).
  3. Guardian UI: last 10 events.
  4. Test: two synthetic events → two JSON lines.
Acceptance Criteria:
  - Valid JSON lines written.
  - UI updates correctly.

WF-5 (Severity Scaffold)
Status: OPEN
Depends: WF-4
Tasks:
  1. Add enum severity (default MEDIUM).
  2. Store in pattern + emit in events.
Acceptance Criteria:
  - Event JSON includes severity.
  - No severity null errors.

WF-6 (Whitelist)
Status: OPEN
Depends: WF-5
Tasks:
  1. Persisted whitelist (single token or full phrase sequence).
  2. UI add/remove.
  3. Skip logging if match fully whitelisted.
  4. Test suppression after addition.
Acceptance Criteria:
  - Survives restart.
  - Suppression test passes.

WF-7 (Performance Plan – Documentation Only)
Status: OPEN
Depends: WF-6
Tasks:
  1. perf-baseline.md with measured times.
  2. ADR: Aho-Corasick token sequence migration path.
Acceptance Criteria:
  - Baseline metrics recorded.
  - ADR committed.

Parking / Deferred (Track-Specific)
- Leetspeak mapping
- Spaced-letter normalization
- Approximate (edit-distance) detection
- Hash-only event token form
- Enforcement action framework
- Context scoring layer

==================================================
PARKING / DEFERRED (GLOBAL)
- Transliteration / homoglyph defense
- Remote guardian sync
- Export analytics format
- Adaptive thresholds
- Risk escalation heuristics

==================================================
CHANGE LOG
2025-09-05: Initial combined roadmap created.

==================================================
INSTRUCTIONS LINK
See docs/SPACE_INSTRUCTIONS.md for operational rules.

END.
