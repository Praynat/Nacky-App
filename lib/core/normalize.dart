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
