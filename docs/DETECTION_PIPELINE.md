# Detection Pipeline Architecture

**Date**: September 21, 2025  
**Legacy Removal**: ForbiddenStore + sendWordList deprecated  

## Overview

The Nacky app uses a unified pattern-based detection pipeline for real-time content monitoring. The legacy flat word blocking system has been completely removed in favor of this multi-stage approach.

## Pipeline Architecture

```
Flutter (Dart)                     Android (Kotlin)
┌─────────────────┐                ┌──────────────────────┐
│ WordsRepo       │                │ PatternRepository    │
│ - Default words │  updatePatterns │ - Structured payload │
│ - User words    │ ──────────────► │ - JSON parsing       │
│ - buildPatterns │                │ - Validation         │
└─────────────────┘                └──────────────────────┘
                                                │
                                                ▼
                                   ┌──────────────────────┐
                                   │ Step 1: TokenTrie    │
                                   │ - Multi-token match  │
                                   │ - Prefix traversal   │
                                   │ - Pattern lookup     │
                                   └──────────────────────┘
                                                │
                                                ▼
                                   ┌──────────────────────┐
                                   │ Step 2: Variants     │
                                   │ - Leet speak (a→@)   │
                                   │ - Spacing (b l o w)  │
                                   │ - Separators (bl*ow) │
                                   └──────────────────────┘
                                                │
                                                ▼
                                   ┌──────────────────────┐
                                   │ Step 3: Rules        │
                                   │ - Context validation │
                                   │ - False pos. reduce  │
                                   │ - Severity mapping   │
                                   └──────────────────────┘
                                                │
                                   ┌────────────┴────────────┐
                                   ▼                         ▼
                        ┌─────────────────┐    ┌─────────────────┐
                        │ LiveTypingDetect│    │ MonitoringEngine│
                        │ - TYPE_TEXT_CHG │    │ - TYPE_WIN_CONT │
                        │ - Debounce      │    │ - Aggregation   │
                        │ - Focus events  │    │ - Cooldown      │
                        └─────────────────┘    └─────────────────┘
```

## Data Flow

### 1. Pattern Payload Creation (Flutter)
- `WordsRepo.buildPatterns()` converts word lists to structured patterns
- `patternsToPayload()` creates JSON with metadata (version, categories, severities)
- Triggered from: dashboard init, word add/edit/delete actions

### 2. Pattern Repository (Android)
- `PatternRepository.updateFromPayload()` parses incoming JSON
- Validates structure and converts to internal pattern objects
- Builds optimized data structures for detection engines

### 3. Detection Engines
**TokenTrie (Step 1)**:
- Efficient prefix matching for single and multi-token patterns
- O(log n) lookup time for pattern identification

**Variant Engine (Step 2)**:
- Handles text obfuscation (leet speak, spacing, separators)
- Aho-Corasick algorithm over normalized buffer

**Rule Engine (Step 3)**:
- Context-aware filtering (R1-R7 + R6B rules)
- False positive reduction ("assistant" vs explicit content)
- Deterministic reason codes for debugging

### 4. Live Detection
**LiveTypingDetector**:
- Monitors `TYPE_VIEW_TEXT_CHANGED` events
- Configurable debounce (default: boundary finalization)
- Focus change handling for input context

**MonitoringEngine**:
- Processes `TYPE_WINDOW_CONTENT_CHANGED` events  
- Content aggregation with cooldown windows
- Count-based triggering logic

## Configuration

### Debounce Settings
- Live typing: boundary-based finalization
- Monitoring: cooldown windows prevent spam
- Configurable via `LiveSettings` object

### Pattern Format
```json
{
  "version": 1,
  "patterns": [
    {
      "id": "pattern_id",
      "category": "category_name", 
      "severity": "low|medium|high",
      "tokensOrPhrases": ["single_token", "multi word phrase"]
    }
  ],
  "meta": {
    "source": "dashboard|words_add_screen|words_manage_screen",
    "reason": "initial_push|add_word|edit_word|delete_word",
    "items_total": 123
  }
}
```

## Event Logging

All detection events are logged with pattern ID and severity:
```
MON hit src=com.app.name pat=pattern_123 sev=medium
```

Service startup confirms active pipeline:
```
Service connected: pattern pipeline ACTIVE (legacy removed)
```

## Removed Components

- **ForbiddenStore.kt**: Flat word set storage
- **sendWordList**: Legacy method channel 
- **handleTyping/handleContent**: Immediate blocking logic
- **matchesForbidden/countMatches**: Token-based scanning
- **Throttle logic**: Simple time-based blocking prevention

## Future Enhancements

1. **Enforcement Actions**: Soft blocking, warnings, navigation
2. **Adaptive Debounce**: Dynamic timing based on user behavior  
3. **Telemetry**: Detection outcome analytics for tuning
4. **Configuration UI**: Rule enable/disable toggles
5. **Persistence**: Pattern cache across app restarts

---

*Generated after legacy removal on September 21, 2025*