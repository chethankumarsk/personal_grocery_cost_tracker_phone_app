package dev.groceryledger.data.update

/**
 * GitHub release bodies are Markdown, but the update dialog shows plain text. Without this
 * the dialog reads like `- **Budget.** ...` with backticks and `---` rules in it.
 *
 * This keeps the words, removes the syntax, and leaves one blank line between paragraphs.
 */
object ReleaseNotes {
    /** Marker appended when the notes had to be shortened for the dialog. */
    const val ELLIPSIS = "…"

    private val heading = Regex("""^#{1,6}[ \t]+""", RegexOption.MULTILINE)
    private val rule = Regex("""^[ \t]*([-*_])[ \t]*(?:\1[ \t]*){2,}$""", RegexOption.MULTILINE)
    private val bullet = Regex("""^([ \t]*)[-*+][ \t]+""", RegexOption.MULTILINE)
    private val boldOrItalic = Regex("""(\*\*\*|\*\*|\*|___|__|_)(\S(?:.*?\S)?)\1""")
    private val inlineCode = Regex("""`([^`]*)`""")
    private val link = Regex("""!?\[([^\]]+)\]\([^)]*\)""")
    private val blankRuns = Regex("""\n{3,}""")

    /**
     * @param maxChars upper bound for the dialog; longer notes are cut on a word boundary and
     *   end with [ELLIPSIS]. Zero or negative means "no limit".
     */
    fun plainText(body: String, maxChars: Int = 2_000): String {
        val cleaned = body
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(rule, "")
            .replace(heading, "")
            .replace(bullet, "$1• ")
            .replace(boldOrItalic, "$2")
            .replace(inlineCode, "$1")
            .replace(link, "$1")
            .replace(blankRuns, "\n\n")
            .trim()

        if (maxChars <= 0 || cleaned.length <= maxChars) return cleaned

        val head = cleaned.take(maxChars - ELLIPSIS.length)
        val boundary = head.indexOfLast { it == ' ' || it == '\n' }
        val cut = if (boundary > maxChars / 2) head.take(boundary) else head
        return cut.trimEnd() + ELLIPSIS
    }
}
