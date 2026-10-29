package dev.groceryledger.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import dev.groceryledger.domain.model.Expense

sealed interface UiText {
    data class StringResource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class PluralResource(
        @PluralsRes val id: Int,
        val count: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText =
    UiText.StringResource(id, args.toList())

fun uiPlural(@PluralsRes id: Int, count: Int, vararg args: Any): UiText =
    UiText.PluralResource(id, count, args.toList())

fun UiText.resolve(context: Context): String = when (this) {
    is UiText.StringResource -> context.getString(id, *args.toTypedArray())
    is UiText.PluralResource -> context.resources.getQuantityString(
        id,
        count,
        *args.toTypedArray(),
    )
}

enum class SnackbarAction {
    UNDO,
}

data class UiEvent(
    val message: UiText,
    val action: SnackbarAction? = null,
    val deletedEntry: Expense? = null,
)
