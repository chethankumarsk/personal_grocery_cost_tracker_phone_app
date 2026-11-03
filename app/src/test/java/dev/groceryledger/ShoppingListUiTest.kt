package dev.groceryledger

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class ShoppingListUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app: GroceryLedgerApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearData() = runBlocking {
        app.container.expenseRepository.clearAll()
        app.container.shoppingListRepository.clear()
    }

    @Test
    fun itemsCanBeAddedCheckedAndConvertedFromTheScreen() {
        composeRule.onNodeWithContentDescription("Shopping list").performClick()
        composeRule.onNodeWithText("Shopping list").assertExists()

        composeRule.onNodeWithTag("shopping_item_name").performTextInput("Milk")
        composeRule.onNodeWithTag("shopping_amount").performClick()
        composeRule.onNodeWithTag("shopping_amount").performTextInput("60")
        composeRule.onNodeWithText("Add").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking {
                app.container.shoppingListRepository.observeItems().first()
                    .any { it.name == "Milk" && it.amountPaise == 6_000L }
            }
        }

        composeRule.onAllNodes(isToggleable()).onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.container.shoppingListRepository.checkedItems().isNotEmpty() }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("shopping_add_checked")
                .fetchSemanticsNodes()
                .any { node -> node.config.getOrNull(SemanticsProperties.Disabled) == null }
        }
        composeRule.onNodeWithTag("shopping_add_checked").assertIsEnabled()
    }
}
