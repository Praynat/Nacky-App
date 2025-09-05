#!/usr/bin/env python3
"""
Simple validation script for Unicode tokenization
This simulates the behavior to verify our regex patterns work correctly
"""

import re

def tokenize_unicode(text):
    """
    Python equivalent of our Unicode tokenization
    Split on non-letter, non-mark, non-digit boundaries
    """
    # Python regex equivalent of \p{L}\p{M}\p{N}
    pattern = r'[^\w\u0300-\u036f\u1ab0-\u1aff\u1dc0-\u1dff\u20d0-\u20ff\ufe20-\ufe2f]+'
    tokens = re.split(pattern, text, flags=re.UNICODE)
    return [token for token in tokens if token.strip()]

def test_tokenization():
    """Run validation tests"""
    test_cases = [
        ("canción", ["canción"]),
        ("Über", ["Über"]),
        ("שלום עולם", ["שלום", "עולם"]),
        ("hello123世界", ["hello123世界"]),
        ("test123word", ["test123word"]),
        ("bad word", ["bad", "word"]),
        ("", []),
        ("   ", []),
        ("\t\n", []),
        ("hello, world! test.", ["hello", "world", "test"]),
        ("مرحبا بالعالم", ["مرحبا", "بالعالم"]),
    ]
    
    print("Unicode Tokenization Validation:")
    print("=" * 40)
    
    passed = 0
    total = len(test_cases)
    
    for i, (input_text, expected) in enumerate(test_cases, 1):
        result = tokenize_unicode(input_text)
        success = result == expected
        
        print(f"Test {i}: {'✓' if success else '✗'}")
        print(f"  Input: '{input_text}'")
        print(f"  Expected: {expected}")
        print(f"  Got: {result}")
        
        if success:
            passed += 1
        else:
            print(f"  *** MISMATCH ***")
        print()
    
    print(f"Results: {passed}/{total} tests passed")
    return passed == total

if __name__ == "__main__":
    success = test_tokenization()
    exit(0 if success else 1)