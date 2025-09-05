String normalizeWord(String input) {
  final s = input.trim().toLowerCase();
  const accents = 'àâäáãåçèéêëìíîïñòóôöõùúûüýÿ';
  const plain   = 'aaaaaaceeeeiiiinooooouuuuyy';
  final map = {for (int i = 0; i < accents.length; i++) accents[i]: plain[i]};
  final sb = StringBuffer();
  for (final r in s.runes) {
    final ch = String.fromCharCode(r);
    final idx = accents.indexOf(ch);
    sb.write(idx >= 0 ? plain[idx] : ch);
  }
  return sb.toString();
}

List<String> tokenizeUnicode(String text) {
  // Unicode-aware tokenization: split on non-letter, non-mark, non-digit
  // Using RegExp with Unicode property escapes
  final tokens = text.split(RegExp(r'[^\p{L}\p{M}\p{N}]+', unicode: true))
      .where((token) => token.isNotEmpty)
      .toList();
  return tokens;
}
