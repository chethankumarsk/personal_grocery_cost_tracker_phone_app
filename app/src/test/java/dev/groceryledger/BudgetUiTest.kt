package dev.groceryledger

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class BudgetUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app: GroceryLedgerApplication
        get() = ApplicationProvider.getApplicationContext()

    private val today: LocalDate = LocalDate.now()

    @Before
    fun clearData() = runBlocking {
        app.container.expenseRepository.clearAll()
        app.container.settingsRepository.setMonthlyBudget(null)
    }

    @After
    fun clearBudget() = runBlocking {
        app.container.settingsRepository.setMonthlyBudget(null)
    }

    @Test
    fun budgetCardShowsSpentAndRemaining() {
        runBlocking {
            app.container.settingsRepository.setMonthlyBudget(50_000)
            app.container.expenseRepository.add("Milk", 6_000, today)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("₹60 of ₹500").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("₹440 left").assertExists()
    }

    @Test
    fun overBudgetIsReported() {
        runBlocking {
            app.container.settingsRepository.setMonthlyBudget(5_000)
            app.container.expenseRepository.add("Milk", 6_000, today)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("₹10 over budget").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun settingsShowsAndChangesTheBudget() {
        runBlocking {
            app.container.settingsRepository.setMonthlyBudget(50_000)
        }

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Monthly budget").assertExists()
        composeRule.onNodeWithText("₹500 per month").assertExists()

        composeRule.onNodeWithText("Clear").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("No budget set.").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("₹500 per month").assertCountEquals(0)
    }
}
