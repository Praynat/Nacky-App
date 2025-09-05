/**
 * Simple Kotlin validation script for Unicode tokenization
 * This verifies our tokenizeUnicode function works correctly
 */

import java.text.Normalizer
import java.util.Locale

fun tokenizeUnicode(text: String): List<String> {
    // Split on non-letter, non-mark, non-digit boundaries (Unicode-aware)
    return text.split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.isNotBlank() }
}

fun String.normalizeWord(): String {
    val lower = this.lowercase(Locale.ROOT)
    val norm = Normalizer.normalize(lower, Normalizer.Form.NFD)
    return norm.replace("\\p{Mn}+".toRegex(), "")
}

fun main() {
    println("Unicode Tokenization Validation (Kotlin):")
    println("=" * 40)

    val testCases = listOf(
        "canción" to listOf("canción"),
        "Über" to listOf("Über"),
        "שלום עולם" to listOf("שלום", "עולם"),
        "hello123世界" to listOf("hello123世界"),
        "test123word" to listOf("test123word"),
        "bad word" to listOf("bad", "word"),
        "" to emptyList(),
        "   " to emptyList(),
        "\t\n" to emptyList(),
        "hello, world! test." to listOf("hello", "world", "test"),
        "مرحبا بالعالم" to listOf("مرحبا", "بالعالم")
    )

    var passed = 0
    val total = testCases.size

    testCases.forEachIndexed { index, (input, expected) ->
        val result = tokenizeUnicode(input)
        val success = result == expected

        println("Test ${index + 1}: ${if (success) "✓" else "✗"}")
        println("  Input: '$input'")
        println("  Expected: $expected")
        println("  Got: $result")

        if (success) {
            passed++
        } else {
            println("  *** MISMATCH ***")
        }
        println()
    }

    println("Results: $passed/$total tests passed")

    // Additional test: verify normalization still works
    println("\nNormalization tests:")
    val normTests = listOf(
        "café" to "cafe",
        "naïve" to "naive", 
        "piñata" to "pinata"
    )
    
    normTests.forEach { (input, expected) ->
        val result = input.normalizeWord()
        val success = result == expected
        println("${if (success) "✓" else "✗"} '$input' -> '$result' (expected '$expected')")
    }
}