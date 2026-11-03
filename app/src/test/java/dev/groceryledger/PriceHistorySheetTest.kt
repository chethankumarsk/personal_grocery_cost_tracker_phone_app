package dev.groceryledger

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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
class PriceHistorySheetTest {
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
    fun historyShowsTrendStatsAndEveryPurchase() {
        seedTwoMilkPurchasesAndBread()
        openMilkHistory()

        composeRule.onNodeWithText("Last paid ${Money.format(6_500)}").assertExists()
        composeRule.onNodeWithText("Average").assertExists()
        composeRule.onNodeWithText("Low").assertExists()
        composeRule.onNodeWithText("High").assertExists()
        composeRule.onNodeWithText("Purchases").assertExists()
        composeRule.onNodeWithText("2 purchases").assertExists()

        val trend = context.getString(
            R.string.price_change_up,
            Money.format(500),
            "8.3%",
            Money.format(6_000),
            DateRanges.formatDayWithYear(today.minusDays(1)),
        )
        composeRule.onNodeWithText(trend).assertExists()
    }

    @Test
    fun tappingAPurchaseOpensItsEditor() {
        seedTwoMilkPurchasesAndBread()
        openMilkHistory()

        val rowDescription = context.getString(
            R.string.price_history_edit_description,
            "Milk",
            DateRanges.formatDayWithYear(today),
            Money.format(6_500),
        )
        composeRule.onNodeWithContentDescription(rowDescription).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("entry_name").assertTextContains("Milk")
        composeRule.onNodeWithTag("entry_amount").assertTextContains("65")
    }

    @Test
    fun singlePurchaseExplainsWhyThereIsNoTrend() {
        runBlocking {
            app.container.expenseRepository.add("Milk", 6_000, today)
        }
        openDaySheet(6_000, itemCount = 1)
        openHistory("Milk")

        composeRule.onNodeWithText(
            context.getString(R.string.price_history_single),
        ).assertExists()
        composeRule.onNodeWithText("1 purchase").assertExists()
    }

    private fun seedTwoMilkPurchasesAndBread() {
        runBlocking {
            app.container.expenseRepository.add(
                "Milk",
                6_000,
                today.minusDays(1),
                category = "Dairy & Eggs",
            )
            app.container.expenseRepository.add("Milk", 6_500, today, category = "Dairy & Eggs")
            app.container.expenseRepository.add("Bread", 4_000, today)
        }
        openDaySheet(totalPaise = 10_500, itemCount = 2)
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

    private fun openMilkHistory() {
        openHistory("Milk")
    }

    private fun openHistory(name: String) {
        val description = context.getString(R.string.price_history_for, name)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onAllNodesWithContentDescription(description)
            .onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)

        // The sheet opens immediately but its Room flow emits a frame later.
        val averageLabel = context.getString(R.string.price_history_average)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(averageLabel).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
