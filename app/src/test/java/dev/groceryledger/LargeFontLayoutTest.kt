package dev.groceryledger

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import dev.groceryledger.ui.calendar.BudgetCard
import dev.groceryledger.ui.shopping.ShoppingListScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for the 200% font QA pass: rows that fit at 100% must stack instead of
 * overlapping or squeezing labels until they break mid-word.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class LargeFontLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private fun setLargeFontContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 3f, fontScale = 2f),
            ) {
                MaterialTheme { content() }
            }
        }
    }

    @Test
    fun budgetAmountSitsBelowTheTitleInsteadOfColliding() {
        setLargeFontContent {
            BudgetCard(spentPaise = 6_000, budgetPaise = 50_000, monthLabel = "November 2026")
        }

        val title = rule.onNodeWithText("Monthly budget").fetchSemanticsNode().boundsInRoot
        val amount = rule.onNodeWithText("₹60 of ₹500").fetchSemanticsNode().boundsInRoot

        assertTrue(
            "the amount should start below the title block, was ${amount.top} vs ${title.bottom}",
            amount.top >= title.bottom - 1f,
        )
        assertTrue("the amount should be inside the card", amount.right <= title.right + 400f)
    }

    @Test
    fun shoppingListFieldsStackAtLargeFont() {
        setLargeFontContent {
            ShoppingListScreen(
                items = emptyList(),
                snackbarHostState = remember { SnackbarHostState() },
                onBack = {},
                onAdd = { _, _ -> },
                onToggle = {},
                onDelete = {},
                onAddChecked = {},
                onClear = {},
            )
        }

        val name = rule.onNodeWithTag("shopping_item_name").fetchSemanticsNode().boundsInRoot
        val amount = rule.onNodeWithTag("shopping_amount").fetchSemanticsNode().boundsInRoot

        assertEquals(name.left, amount.left, 1f)
        assertTrue("the amount field should be below the name", amount.top >= name.bottom - 1f)
    }
}
