package dev.groceryledger

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class AddEntryUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearData() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<GroceryLedgerApplication>()
        app.container.expenseRepository.clearAll()
    }

    @Test
    fun addingAnEntrySavesIt() {
        composeRule.onNodeWithContentDescription("Add item").performClick()

        composeRule.onNodeWithTag("entry_name").performTextInput("Milk")
        composeRule.onNodeWithTag("entry_amount").performTextInput("60")
        composeRule.onNodeWithTag("entry_name").assertTextContains("Milk")
        composeRule.onNodeWithTag("entry_amount").assertTextContains("60")

        composeRule.onNodeWithTag("entry_save").assertHasClickAction()
        composeRule.onNodeWithTag("entry_save")
            .performSemanticsAction(SemanticsActions.OnClick)

        val app = ApplicationProvider.getApplicationContext<GroceryLedgerApplication>()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.container.expenseRepository.all().isNotEmpty() }
        }
        composeRule.waitForIdle()

        val entries = runBlocking { app.container.expenseRepository.all() }
        assertEquals(1, entries.size)
        assertEquals("Milk", entries.single().name)
        assertEquals(6_000L, entries.single().amountPaise)
        assertTrue(
            composeRule.onAllNodesWithTag("entry_name").fetchSemanticsNodes().isEmpty(),
        )
    }
}
