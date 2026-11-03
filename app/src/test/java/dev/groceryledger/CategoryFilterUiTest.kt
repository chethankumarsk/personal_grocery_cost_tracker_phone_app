package dev.groceryledger

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.groceryledger.domain.date.DateRanges
import dev.groceryledger.domain.money.Money
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class CategoryFilterUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app: GroceryLedgerApplication
        get() = ApplicationProvider.getApplicationContext()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val today: LocalDate = LocalDate.now()

    @Before
    fun clearData() = runBlocking {
        app.container.expenseRepository.clearAll()
    }

    @Test
    fun filteringByCategoryNarrowsTotalsAndTheDaySheet() {
        runBlocking {
            app.container.expenseRepository.add("Milk", 6_000, today)
            app.container.expenseRepository.add(
                "Chips",
                2_000,
                today,
                category = "Snacks & Beverages",
            )
        }

        // Unfiltered: day, week and month all show ₹80.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("₹80").fetchSemanticsNodes().size >= 3
        }

        composeRule.onNodeWithText("Uncategorized").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("₹80").fetchSemanticsNodes().isEmpty()
        }

        openDaySheet(totalPaise = 6_000, itemCount = 1)
        composeRule.onNodeWithText("Milk").assertExists()
        composeRule.onAllNodesWithText("Chips").assertCountEquals(0)
    }

    @Test
    fun clearingTheFilterRestoresEveryCategory() {
        runBlocking {
            app.container.expenseRepository.add("Milk", 6_000, today)
            app.container.expenseRepository.add(
                "Chips",
                2_000,
                today,
                category = "Snacks & Beverages",
            )
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("₹80").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Uncategorized").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("₹60").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("All").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("₹80").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openDaySheet(totalPaise: Long, itemCount: Int) {
        val items = context.resources.getQuantityString(R.plurals.items_count, itemCount, itemCount)
        val description = context.getString(
            R.string.calendar_day_description,
            DateRanges.formatFullDate(today),
            Money.format(totalPaise),
            items,
        )
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(description).performClick()
        composeRule.waitForIdle()
    }
}
