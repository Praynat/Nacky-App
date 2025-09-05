import 'package:flutter_test/flutter_test.dart';
import '../lib/core/normalize.dart';

void main() {
  group('Unicode Tokenization Tests', () {
    test('should tokenize Spanish text with diacritics', () {
      final tokens = tokenizeUnicode('canción');
      expect(tokens, equals(['canción']));
    });

    test('should tokenize German text with umlauts', () {
      final tokens = tokenizeUnicode('Über');
      expect(tokens, equals(['Über']));
    });

    test('should tokenize Hebrew text', () {
      final tokens = tokenizeUnicode('שלום עולם');
      expect(tokens, equals(['שלום', 'עולם']));
    });

    test('should tokenize mixed script text', () {
      final tokens = tokenizeUnicode('hello123世界');
      expect(tokens, equals(['hello123世界']));
    });

    test('should handle text with digits inline', () {
      final tokens = tokenizeUnicode('test123word');
      expect(tokens, equals(['test123word']));
    });

    test('should maintain legacy ASCII tokenization behavior', () {
      final tokens = tokenizeUnicode('bad word');
      expect(tokens, equals(['bad', 'word']));
    });

    test('should handle empty and whitespace-only text', () {
      expect(tokenizeUnicode(''), equals([]));
      expect(tokenizeUnicode('   '), equals([]));
      expect(tokenizeUnicode('\t\n'), equals([]));
    });

    test('should handle punctuation boundaries correctly', () {
      final tokens = tokenizeUnicode('hello, world! test.');
      expect(tokens, equals(['hello', 'world', 'test']));
    });

    test('should preserve Unicode combining marks', () {
      // Test with combining diacritical marks
      final tokens = tokenizeUnicode('café');
      expect(tokens.isNotEmpty, true);
      expect(tokens.first.contains('é'), true);
    });

    test('should handle Arabic text', () {
      final tokens = tokenizeUnicode('مرحبا بالعالم');
      expect(tokens, equals(['مرحبا', 'بالعالم']));
    });
  });

  group('Legacy normalizeWord tests', () {
    test('should still normalize accented characters', () {
      expect(normalizeWord('café'), equals('cafe'));
      expect(normalizeWord('naïve'), equals('naive'));
      expect(normalizeWord('piñata'), equals('pinata'));
    });
  });
}