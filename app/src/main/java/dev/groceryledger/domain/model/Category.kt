package dev.groceryledger.domain.model

/**
 * Category rules shared by the data layer, the CSV format and the UI.
 *
 * Categories are stored by **name** on each entry rather than by id, so a backup stays
 * readable on its own and an unknown category from a future backup can still be imported.
 */
object Categories {
    /** Every entry has a category; this is the one used when none was chosen. */
    const val UNCATEGORIZED = "Uncategorized"

    const val MAX_NAME_LENGTH = 40

    /** Seeded into an empty database. They are ordinary categories afterwards. */
    val presets: List<String> = listOf(
        "Vegetables & Fruits",
        "Dairy & Eggs",
        "Grains & Pulses",
        "Snacks & Beverages",
        "Household",
        "Personal Care",
        "Other",
    )

    /** Trims and collapses internal whitespace so "Dairy  &  Eggs" and "Dairy & Eggs" match. */
    fun normalize(raw: String): String = raw.trim().replace(Regex("\\s+"), " ")

    /**
     * Returns the cleaned name, or null when the value cannot be used. Names are compared
     * case-insensitively against [UNCATEGORIZED] so there is only ever one bucket for
     * "no category".
     */
    fun sanitize(raw: String): String? {
        val name = normalize(raw)
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) return null
        if (name.equals(UNCATEGORIZED, ignoreCase = true)) return null
        return name
    }
}
