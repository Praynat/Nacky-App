import org.junit.Test
import org.junit.Assert.*

/**
 * Simple test to validate Unicode tokenization in Android Kotlin
 * Run with: kotlinc-jvm UnicodeTokenizationTest.kt -cp .:junit-4.12.jar && kotlin UnicodeTokenizationTestKt
 */
class UnicodeTokenizationTest {
    
    private fun tokenizeUnicode(text: String): List<String> {
        // Split on non-letter, non-mark, non-digit boundaries (Unicode-aware)
        return text.split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.isNotBlank() }
    }

    @Test
    fun testSpanishWithDiacritics() {
        val tokens = tokenizeUnicode("canción")
        assertEquals(listOf("canción"), tokens)
    }

    @Test
    fun testGermanUmlauts() {
        val tokens = tokenizeUnicode("Über")
        assertEquals(listOf("Über"), tokens)
    }

    @Test
    fun testHebrewText() {
        val tokens = tokenizeUnicode("שלום עולם")
        assertEquals(listOf("שלום", "עולם"), tokens)
    }

    @Test
    fun testMixedScript() {
        val tokens = tokenizeUnicode("hello123世界")
        assertEquals(listOf("hello123世界"), tokens)
    }

    @Test
    fun testDigitsInline() {
        val tokens = tokenizeUnicode("test123word")
        assertEquals(listOf("test123word"), tokens)
    }

    @Test
    fun testLegacyBehavior() {
        val tokens = tokenizeUnicode("bad word")
        assertEquals(listOf("bad", "word"), tokens)
    }

    @Test
    fun testEmptyAndWhitespace() {
        assertEquals(emptyList<String>(), tokenizeUnicode(""))
        assertEquals(emptyList<String>(), tokenizeUnicode("   "))
        assertEquals(emptyList<String>(), tokenizeUnicode("\t\n"))
    }

    @Test
    fun testPunctuationBoundaries() {
        val tokens = tokenizeUnicode("hello, world! test.")
        assertEquals(listOf("hello", "world", "test"), tokens)
    }

    @Test
    fun testArabicText() {
        val tokens = tokenizeUnicode("مرحبا بالعالم")
        assertEquals(listOf("مرحبا", "بالعالم"), tokens)
    }
}