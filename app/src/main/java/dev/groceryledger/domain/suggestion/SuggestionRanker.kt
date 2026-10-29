package dev.groceryledger.domain.suggestion

import dev.groceryledger.domain.model.ItemSuggestion
import java.util.Locale

object ItemNames {
    private val whitespace = Regex("\\s+")

    fun normalize(raw: String): String =
        raw.trim().lowercase(Locale.ROOT).replace(whitespace, " ")
}

object SuggestionRanker {
    fun rank(
        items: List<ItemSuggestion>,
        rawQuery: String,
        limit: Int = 6,
    ): List<ItemSuggestion> {
        val query = ItemNames.normalize(rawQuery)
        if (query.length < 2) return emptyList()

        return items
            .mapNotNull { item ->
                val name = ItemNames.normalize(item.name)
                val rank = when {
                    name.startsWith(query) -> 0
                    name.split(' ').any { it.startsWith(query) } -> 1
                    name.contains(query) -> 2
                    else -> null
                } ?: return@mapNotNull null
                RankedSuggestion(rank, item)
            }
            .sortedWith(
                compareBy<RankedSuggestion> { it.rank }
                    .thenByDescending { it.item.usageCount }
                    .thenByDescending { it.item.lastDate }
                    .thenByDescending { it.item.lastUsedAt }
                    .thenBy { ItemNames.normalize(it.item.name) },
            )
            .take(limit)
            .map { it.item }
    }

    private data class RankedSuggestion(
        val rank: Int,
        val item: ItemSuggestion,
    )
}
