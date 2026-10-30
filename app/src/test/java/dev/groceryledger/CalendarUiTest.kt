package dev.groceryledger

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class CalendarUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsCalendarAndOpensTheAddSheet() {
        composeRule.onNodeWithText("Grocery Ledger").assertExists()
        composeRule.onNodeWithContentDescription("Add item").assertExists().performClick()
        composeRule.onNodeWithText("Add entry", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Item name", useUnmergedTree = true).assertExists()
    }

    @Test
    fun opensSettings() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Week starts on").assertExists()
        composeRule.onNodeWithText("Categories").assertExists()
        composeRule.onNodeWithText("Export CSV").assertExists()
        composeRule.onNodeWithText("Delete all data").assertExists()
        composeRule.onNodeWithText("Check for updates").assertExists()
    }
}
