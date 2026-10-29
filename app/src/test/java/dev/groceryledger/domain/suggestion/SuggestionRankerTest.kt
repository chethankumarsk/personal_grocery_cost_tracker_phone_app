package dev.groceryledger.domain.suggestion

import dev.groceryledger.domain.model.ItemSuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SuggestionRankerTest {
    private val items = listOf(
        suggestion("Milk", usage = 3, date = "2026-10-12", usedAt = 300),
        suggestion("Amul Milk", usage = 5, date = "2026-10-20", usedAt = 500),
        suggestion("Millet", usage = 1, date = "2026-10-10", usedAt = 100),
        suggestion("Almond Milk", usage = 0, date = "2026-09-01", usedAt = 50),
        suggestion("Bread", usage = 9, date = "2026-10-25", usedAt = 900),
    )

    @Test
    fun prefixMatchesRankBeforeWordMatches() {
        val result = SuggestionRanker.rank(items, "mi")
        assertEquals(listOf("Milk", "Millet", "Amul Milk", "Almond Milk"), result.map { it.name })
    }

    @Test
    fun wordMatchesRankBeforeContainsMatches() {
        val result = SuggestionRanker.rank(items, "lk")
        assertTrue(result.isNotEmpty())
        assertEquals("Amul Milk", result.first().name)
    }

    @Test
    fun usageThenRecencyBreakTiesWithinARank() {
        val result = SuggestionRanker.rank(items, "milk")
        assertEquals(listOf("Milk", "Amul Milk", "Almond Milk"), result.map { it.name })
    }

    @Test
    fun matchingIsCaseAndWhitespaceInsensitiveAndRequiresTwoCharacters() {
        assertEquals(emptyList<ItemSuggestion>(), SuggestionRanker.rank(items, "m"))
        assertEquals(
            "Milk",
            SuggestionRanker.rank(items, "  MILK ").first().name,
        )
    }

    @Test
    fun resultCountIsCapped() {
        val many = (1..20).map { index ->
            suggestion("Milk $index", usage = index, date = "2026-10-12", usedAt = index.toLong())
        }
        assertEquals(6, SuggestionRanker.rank(many, "milk").size)
    }

    private fun suggestion(
        name: String,
        usage: Int,
        date: String,
        usedAt: Long,
    ): ItemSuggestion = ItemSuggestion(
        name = name,
        lastPricePaise = 6_000,
        lastDate = LocalDate.parse(date),
        usageCount = usage,
        lastUsedAt = usedAt,
    )
}
