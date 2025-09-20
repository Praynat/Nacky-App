# feat(android): wire persistence for patterns & detection settings

## Summary
This PR introduces robust persistence for detection patterns and detection settings on Android. On accessibility service startup (`onServiceConnected`):
- Loads previously persisted detection settings via `SettingsStore.load(...)` into `DetectionSettingsStore`.
- Loads previously persisted pattern snapshot via `PatternsStore.load(...)` (only if the in-memory repository is still empty to avoid overwriting a live push), then feeds it through `PatternRepository.updateFromPayload(...)`.
- Rebuilds monitoring and live engines after restoration so in-memory tries reflect persisted data.
- Applies restored runtime settings to engines (`Engines.updateMonitoringSettings` / `Engines.updateLiveSettings`).

## Persistence Behavior
- Runtime updates (Flutter MethodChannel calls) trigger save of settings and patterns immediately after successful parse.
- Files written under app `filesDir/detection/` using atomic / safe write path (temp write + fallback):
  - `detection_settings.json`
  - `patterns.json`
- Serialization now uses `kotlinx-serialization` (fully JVM + Android compatible); no `org.json` usage remains in persistence layer.
- Pattern snapshot schema keeps: `version`, `patterns[]` (id, category, severity, tokensOrPhrases), `meta` (string map). No runtime user text is persisted.

## Safety / Privacy
- No raw snapshot UI text or live typing buffers are ever written to disk.
- Logging on service connect is concise: flags + counts + version only.
- Deserialization is tolerant (ignore unknown keys) and returns null on corruption without crashing the service.

## Tests
- All unit tests pass (`testDebugUnitTest`).
- Added round‑trip tests for settings & patterns with serialization helpers.
- Corrupted JSON deserialization gracefully returns null.

## Implementation Notes
- `DetectionSettings`, `Pattern`, `PatternsPayload`, and `PatternsStore.Snapshot` annotated with `@Serializable`.
- Meta map narrowed to `Map<String, String?>` for predictable JSON schema.
- `onServiceConnected` forces lazy engine build after loading persistence, then applies runtime settings.
- Signature-based rebuild avoidance: a hash signature (patterns hash + key settings fields) is stored; subsequent service connections skip expensive trie/engine rebuild if unchanged, reducing startup latency.
- Case normalization handled outside persistence (tests updated to be case-insensitive for category/severity expectations).

## Follow Ups (Optional)
- Add integration test simulating service restart (would need abstraction for context/filesDir in tests).
- Document JSON schema in a dedicated `docs/persistence.md` (if desired).
- Consider checksum or version bump strategy for future migrations.

---
All green and ready for review.
