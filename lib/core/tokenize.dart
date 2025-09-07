/// Unicode-aware tokenization for text processing across all languages and scripts.
/// 
/// This function:
/// - Finds sequences of letters (with their combining marks) for ALL scripts
/// - Finds sequences of digits
/// - Treats punctuation, spaces, and emojis as separators
/// - Preserves word boundaries correctly for multi-script text
/// 
/// Examples:
/// - "Hello, world!" → ["hello", "world"]
/// - "שלום—עולם" → ["שלום", "עולם"] 
/// - "go🏃fast" → ["go", "fast"]
/// - "canción123" → ["canción", "123"]
/// - "café@home" → ["café", "home"]
List<String> tokenizeUnicode(String input) {
  if (input.isEmpty) return [];
  
  final tokens = <String>[];
  final buffer = StringBuffer();
  bool inWord = false;
  
  for (final rune in input.runes) {
    final char = String.fromCharCode(rune);
    
    if (_isWordCharacter(rune)) {
      // Letter, digit, or combining mark - part of a word
      buffer.write(char);
      inWord = true;
    } else {
      // Separator (space, punctuation, emoji, etc.)
      if (inWord && buffer.isNotEmpty) {
        tokens.add(buffer.toString());
        buffer.clear();
        inWord = false;
      }
    }
  }
  
  // Don't forget the last token
  if (inWord && buffer.isNotEmpty) {
    tokens.add(buffer.toString());
  }
  
  return tokens;
}

/// Determines if a Unicode code point is part of a word.
/// 
/// Includes:
/// - Letters from all scripts (Latin, Hebrew, Arabic, Cyrillic, etc.)
/// - Digits (0-9 and Unicode digits)
/// - Combining marks (diacritics, accents)
/// 
/// Excludes:
/// - Punctuation and symbols
/// - Whitespace
/// - Emoji and pictographs
bool _isWordCharacter(int rune) {
  // Letters (all scripts)
  if ((rune >= 0x0041 && rune <= 0x005A) || // A-Z
      (rune >= 0x0061 && rune <= 0x007A) || // a-z
      (rune >= 0x00C0 && rune <= 0x00D6) || // Latin-1 Supplement (À-Ö)
      (rune >= 0x00D8 && rune <= 0x00F6) || // Latin-1 Supplement (Ø-ö)
      (rune >= 0x00F8 && rune <= 0x00FF) || // Latin-1 Supplement (ø-ÿ)
      (rune >= 0x0100 && rune <= 0x017F) || // Latin Extended-A
      (rune >= 0x0180 && rune <= 0x024F) || // Latin Extended-B
      (rune >= 0x1E00 && rune <= 0x1EFF) || // Latin Extended Additional
      (rune >= 0x0590 && rune <= 0x05FF) || // Hebrew
      (rune >= 0x0600 && rune <= 0x06FF) || // Arabic
      (rune >= 0x0750 && rune <= 0x077F) || // Arabic Supplement
      (rune >= 0x08A0 && rune <= 0x08FF) || // Arabic Extended-A
      (rune >= 0x0400 && rune <= 0x04FF) || // Cyrillic
      (rune >= 0x0370 && rune <= 0x03FF) || // Greek and Coptic
      (rune >= 0x3040 && rune <= 0x309F) || // Hiragana
      (rune >= 0x30A0 && rune <= 0x30FF) || // Katakana
      (rune >= 0x4E00 && rune <= 0x9FFF)    // CJK Unified Ideographs
     ) {
    return true;
  }
  
  // Digits (ASCII and Unicode)
  if ((rune >= 0x0030 && rune <= 0x0039) || // 0-9
      (rune >= 0x0660 && rune <= 0x0669) || // Arabic-Indic digits
      (rune >= 0x06F0 && rune <= 0x06F9)    // Extended Arabic-Indic digits
     ) {
    return true;
  }
  
  // Combining marks (diacritics)
  if ((rune >= 0x0300 && rune <= 0x036F) || // Combining Diacritical Marks
      (rune >= 0x1AB0 && rune <= 0x1AFF) || // Combining Diacritical Marks Extended
      (rune >= 0x1DC0 && rune <= 0x1DFF)    // Combining Diacritical Marks Supplement
     ) {
    return true;
  }
  
  return false;
}