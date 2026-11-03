package dev.groceryledger

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Regression tests for two reported device bugs:
 *  1. the add-entry sheet appeared to reopen while typing;
 *  2. saving appeared to open a fresh blank add sheet.
 *
 * The sheet lives in its own dialog window, and Robolectric cannot deliver real touch
 * events there, so buttons inside the sheet are activated through their semantics click
 * action. The FAB lives in the activity window and is tapped normally.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class EntrySheetStateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app: GroceryLedgerApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearData() = runBlocking {
        app.container.expenseRepository.clearAll()
    }

    @Test
    fun typingKeepsOneSheetOpenAndKeepsFocus() {
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithText("Add entry", useUnmergedTree = true).assertExists()

        "Milk".forEach { char ->
            composeRule.onNodeWithTag("entry_name").performTextInput(char.toString())
            composeRule.waitForIdle()
            // Still exactly one sheet, and it still holds what was typed.
            composeRule.onAllNodesWithText("Add entry").assertCountEquals(1)
            composeRule.onAllNodesWithTag("entry_name").assertCountEquals(1)
        }

        composeRule.onNodeWithTag("entry_name").assertTextContains("Milk")
        composeRule.onNodeWithTag("entry_name").assertIsFocused()
    }

    @Test
    fun typingAmountKeepsOneSheetOpen() {
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag("entry_name").performTextInput("Milk")

        "60".forEach { char ->
            composeRule.onNodeWithTag("entry_amount").performTextInput(char.toString())
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText("Add entry").assertCountEquals(1)
        }

        composeRule.onNodeWithTag("entry_amount").assertTextContains("60")
    }

    @Test
    fun saveClosesSheetAndDoesNotReopen() {
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag("entry_name").performTextInput("Milk")
        composeRule.onNodeWithTag("entry_amount").performTextInput("60")

        composeRule.onNodeWithTag("entry_save")
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.container.expenseRepository.all().isNotEmpty() }
        }
        composeRule.waitForIdle()

        assertEquals(1, runBlocking { app.container.expenseRepository.all() }.size)
        // Nothing may be left behind: no reopened sheet, no blank add form.
        composeRule.onAllNodesWithTag("entry_name").assertCountEquals(0)
        composeRule.onAllNodesWithText("Add entry").assertCountEquals(0)
        composeRule.onAllNodesWithTag("entry_save").assertCountEquals(0)
    }

    @Test
    fun saveAndAddAnotherOpensExactlyOneBlankSheet() {
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag("entry_name").performTextInput("Milk")
        composeRule.onNodeWithTag("entry_amount").performTextInput("60")

        composeRule.onNodeWithText("Save & add another")
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.container.expenseRepository.all().isNotEmpty() }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Add entry").assertCountEquals(1)
        composeRule.onAllNodesWithTag("entry_name").assertCountEquals(1)
        composeRule.onNodeWithTag("entry_name").assertTextContains("")
    }

    @Test
    fun recentItemChipFillsNamePriceAndCategory() {
        runBlocking {
            app.container.expenseRepository.add(
                name = "Milk",
                amountPaise = 6_000,
                date = LocalDate.now(),
                category = "Dairy & Eggs",
            )
        }

        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Milk").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Recent items", useUnmergedTree = true).assertExists()

        composeRule.onNodeWithText("Milk").performClick()

        composeRule.onNodeWithTag("entry_name").assertTextContains("Milk")
        composeRule.onNodeWithTag("entry_amount").assertTextContains("60")
        composeRule.onNodeWithText("Dairy & Eggs").assertIsSelected()
    }

    @Test
    fun recentItemsHideOnceTheNameIsNoLongerBlank() {
        runBlocking {
            app.container.expenseRepository.add(
                name = "Milk",
                amountPaise = 6_000,
                date = LocalDate.now(),
                category = "Dairy & Eggs",
            )
        }

        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("entry_recent_items").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("entry_name").performTextInput("Br")

        composeRule.onAllNodesWithTag("entry_recent_items").assertCountEquals(0)
    }
}
