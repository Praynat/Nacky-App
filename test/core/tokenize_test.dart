import 'package:flutter_test/flutter_test.dart';
import 'package:nacky/core/tokenize.dart';

void main() {
  group('Unicode Tokenization Tests', () {
    test('basic English tokenization', () {
      expect(tokenizeUnicode('Hello, world!'), ['Hello', 'world']);
      expect(tokenizeUnicode('one two three'), ['one', 'two', 'three']);
    });

    test('empty and whitespace input', () {
      expect(tokenizeUnicode(''), []);
      expect(tokenizeUnicode('   '), []);
      expect(tokenizeUnicode('\t\n\r'), []);
    });

    test('Latin with accents (Spanish)', () {
      expect(tokenizeUnicode('canción española'), ['canción', 'española']);
      expect(tokenizeUnicode('¡Hola!'), ['Hola']);
    });

    test('German with umlauts', () {
      expect(tokenizeUnicode('Über alles'), ['Über', 'alles']);
      expect(tokenizeUnicode('Größe'), ['Größe']);
    });

    test('Hebrew script', () {
      expect(tokenizeUnicode('שלום עולם'), ['שלום', 'עולם']);
      expect(tokenizeUnicode('שלום—עולם'), ['שלום', 'עולם']); // em dash separator
    });

    test('Arabic script', () {
      expect(tokenizeUnicode('مرحبا بالعالم'), ['مرحبا', 'بالعالم']);
    });

    test('mixed scripts', () {
      expect(tokenizeUnicode('Hello שלום world'), ['Hello', 'שלום', 'world']);
      expect(tokenizeUnicode('test עברית123'), ['test', 'עברית', '123']);
    });

    test('digits and numbers', () {
      expect(tokenizeUnicode('test123'), ['test123']);
      expect(tokenizeUnicode('word 123 another'), ['word', '123', 'another']);
      expect(tokenizeUnicode('café123'), ['café123']);
    });

    test('emoji and special characters as separators', () {
      expect(tokenizeUnicode('go🏃fast'), ['go', 'fast']);
      expect(tokenizeUnicode('word@symbol'), ['word', 'symbol']);
      expect(tokenizeUnicode('test*star*test'), ['test', 'star', 'test']);
    });

    test('punctuation separation', () {
      expect(tokenizeUnicode('hello,world'), ['hello', 'world']);
      expect(tokenizeUnicode('word1.word2'), ['word1', 'word2']);
      expect(tokenizeUnicode('test:colon'), ['test', 'colon']);
    });

    test('combining marks preservation', () {
      expect(tokenizeUnicode('café'), ['café']); // e with combining acute
      expect(tokenizeUnicode('naïve'), ['naïve']); // i with combining diaeresis
    });

    test('case sensitivity preserved', () {
      expect(tokenizeUnicode('Hello WORLD'), ['Hello', 'WORLD']);
      expect(tokenizeUnicode('CamelCase'), ['CamelCase']);
    });

    test('real-world examples', () {
      expect(tokenizeUnicode('The café is über-cool!'), ['The', 'café', 'is', 'über', 'cool']);
      expect(tokenizeUnicode('Email: user@domain.com'), ['Email', 'user', 'domain', 'com']);
      expect(tokenizeUnicode('Price: $29.99'), ['Price', '29', '99']);
    });

    test('regression test for current word detection', () {
      // These should continue to work as before
      expect(tokenizeUnicode('badword'), ['badword']);
      expect(tokenizeUnicode('bad word'), ['bad', 'word']);
      expect(tokenizeUnicode('bad-word'), ['bad', 'word']);
    });
  });
}