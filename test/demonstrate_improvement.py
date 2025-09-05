#!/usr/bin/env python3
"""
Demonstration script showing the difference between ASCII-only and Unicode-aware tokenization
This shows how WF-1 improves the detection of non-Latin scripts
"""

import re

def tokenize_ascii_only(text):
    """Old ASCII-only tokenization (what we're replacing)"""
    tokens = re.split(r'[^a-z0-9]+', text, flags=re.IGNORECASE)
    return [token for token in tokens if token.strip()]

def tokenize_unicode(text):
    """New Unicode-aware tokenization (WF-1 implementation)"""
    # Use a simplified Unicode pattern that works in Python
    pattern = r'[^\w\u0300-\u036f\u1ab0-\u1aff\u1dc0-\u1dff\u20d0-\u20ff\ufe20-\ufe2f]+'
    tokens = re.split(pattern, text, flags=re.UNICODE)
    return [token for token in tokens if token.strip()]

def demonstrate_improvement():
    """Show the improvement in Unicode handling"""
    test_cases = [
        "canción",
        "Über den Berg",  
        "שלום עולם",
        "مرحبا بالعالم",
        "hello123世界",
        "bad word",  # Should work the same
        "café naïve"
    ]
    
    print("WF-1 Unicode Tokenization Improvement Demonstration")
    print("=" * 60)
    print()
    
    for text in test_cases:
        ascii_tokens = tokenize_ascii_only(text)
        unicode_tokens = tokenize_unicode(text)
        
        print(f"Input: '{text}'")
        print(f"  ASCII-only:     {ascii_tokens}")
        print(f"  Unicode-aware:  {unicode_tokens}")
        
        if ascii_tokens != unicode_tokens:
            print("  *** IMPROVEMENT: Non-Latin tokens now preserved! ***")
        else:
            print("  (Same result - ASCII text handled correctly)")
        print()

if __name__ == "__main__":
    demonstrate_improvement()