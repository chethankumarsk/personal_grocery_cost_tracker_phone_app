package dev.groceryledger.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {

    @Test
    fun markdownSyntaxIsRemovedButTextIsKept() {
        val body = """
            ## What's new

            - **Category filter.** Chips narrow the totals.
            - **Unit price.** Records `quantity` and unit.

            ---

            **Install:** use the signed APK.
        """.trimIndent()

        val text = ReleaseNotes.plainText(body)

        assertFalse(text.contains("**"))
        assertFalse(text.contains("`"))
        assertFalse(text.contains("---"))
        assertFalse(text.contains("##"))
        assertTrue(text.startsWith("What's new"))
        assertTrue(text.contains("• Category filter. Chips narrow the totals."))
        assertTrue(text.contains("• Unit price. Records quantity and unit."))
        assertTrue(text.contains("Install: use the signed APK."))
        assertFalse(text.contains("\n\n\n"))
    }

    @Test
    fun windowsLineEndingsAndLinksAreNormalised() {
        val text = ReleaseNotes.plainText("See [the release page](https://example.com/x)\r\n\r\nThanks\r\n")

        assertEquals("See the release page\n\nThanks", text)
    }

    @Test
    fun italicUnderscoresInsideWordsAreLeftAlone() {
        assertEquals("entry_uid stays stable", ReleaseNotes.plainText("entry_uid stays stable"))
    }

    @Test
    fun longNotesAreCutOnAWordBoundary() {
        val body = "word ".repeat(1_000)

        val text = ReleaseNotes.plainText(body, maxChars = 120)

        assertTrue(text.length <= 120)
        assertTrue(text.endsWith(ReleaseNotes.ELLIPSIS))
        assertFalse(text.contains("wor…"))
    }

    @Test
    fun shortNotesAreNotTouched() {
        assertEquals("Fixed the budget card.", ReleaseNotes.plainText("Fixed the budget card."))
    }
}
