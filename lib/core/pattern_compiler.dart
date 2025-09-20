import 'pattern.dart';
import 'normalize.dart';

/// Responsible for turning raw word lists (seed + user) into structured
/// Pattern objects. For now we produce a single umbrella pattern grouping
/// all words; future iterations may cluster by language, semantic category
/// or detection strategy.
class PatternCompiler {
  const PatternCompiler();

  /// Build naive patterns: one pattern containing all tokens.
  List<Pattern> compileSingleGroup(List<String> words) {
    // Ensure uniqueness & sorted for determinism
    final set = <String>{};
    for (final w in words) {
      final n = normalizeWord(w);
      if (n.isNotEmpty) set.add(n);
    }
    final list = set.toList()..sort();
    return [
      Pattern(
        id: 'default:all',
        category: 'default',
        severity: 'medium',
        tokensOrPhrases: list,
      ),
    ];
  }
}
