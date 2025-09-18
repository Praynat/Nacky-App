/// Pattern data model for forbidden detection V2.
/// Each pattern groups one or more tokens or multi-word phrases
/// under a category + severity, with a stable id used for telemetry.
class Pattern {
  final String id;           // unique identifier (e.g. cat:slug or hash)
  final String category;     // domain grouping (e.g. "sexual", "violence")
  final String severity;     // low | medium | high (blocking policy tier)
  final List<String> tokensOrPhrases; // normalized tokens/phrases

  const Pattern({
    required this.id,
    required this.category,
    required this.severity,
    required this.tokensOrPhrases,
  });

  Map<String, dynamic> toMap() => {
    'id': id,
    'category': category,
    'severity': severity,
    'items': tokensOrPhrases,
  };
}

/// Serialize list to a payload accepted by updatePatterns.
Map<String, dynamic> patternsToPayload(List<Pattern> patterns) => {
  'version': 1,
  'patterns': patterns.map((p) => p.toMap()).toList(),
  'count': patterns.length,
};
