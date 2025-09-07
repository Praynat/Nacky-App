/// Normalizes text for consistent processing across languages and platforms.
/// 
/// This function:
/// - Converts to lowercase
/// - Removes accent marks (é → e) using Unicode NFD normalization
/// - Squashes repeated separators (multiple spaces → single space)
/// - Preserves all scripts (Hebrew, Arabic, etc.)
/// 
/// Examples:
/// - "Crème brûlée!" → "creme brulee!"
/// - "HELLO   WORLD" → "hello world"
/// - "עברית123" → "עברית123"
String normalizeWord(String input) {
  if (input.isEmpty) return input;
  
  // Step 1: Trim and lowercase
  String s = input.trim().toLowerCase();
  
  // Step 2: Unicode NFD normalization to decompose characters
  // In Dart, we need to manually handle common cases since there's no built-in NFD
  s = _decomposeUnicode(s);
  
  // Step 3: Remove combining marks (diacritics)
  s = s.replaceAll(RegExp(r'[\u0300-\u036f]'), ''); // Remove combining diacritical marks
  
  // Step 4: Squash repeated separators (spaces, tabs, etc.)
  s = s.replaceAll(RegExp(r'\s+'), ' ').trim();
  
  return s;
}

/// Decomposes common Unicode characters to base + combining marks
/// This is a simplified version of NFD normalization for common cases
String _decomposeUnicode(String input) {
  // Map of precomposed characters to their decomposed equivalents
  const decompositions = {
    // Latin letters with diacritics
    'à': 'a\u0300', 'á': 'a\u0301', 'â': 'a\u0302', 'ã': 'a\u0303', 
    'ä': 'a\u0308', 'å': 'a\u030a', 'ç': 'c\u0327', 'è': 'e\u0300',
    'é': 'e\u0301', 'ê': 'e\u0302', 'ë': 'e\u0308', 'ì': 'i\u0300',
    'í': 'i\u0301', 'î': 'i\u0302', 'ï': 'i\u0308', 'ñ': 'n\u0303',
    'ò': 'o\u0300', 'ó': 'o\u0301', 'ô': 'o\u0302', 'õ': 'o\u0303',
    'ö': 'o\u0308', 'ù': 'u\u0300', 'ú': 'u\u0301', 'û': 'u\u0302',
    'ü': 'u\u0308', 'ý': 'y\u0301', 'ÿ': 'y\u0308',
  };
  
  final buffer = StringBuffer();
  for (final rune in input.runes) {
    final char = String.fromCharCode(rune);
    buffer.write(decompositions[char] ?? char);
  }
  
  return buffer.toString();
}
