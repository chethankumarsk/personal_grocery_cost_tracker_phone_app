package dev.groceryledger

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h640dp-xhdpi")
class UnitPriceUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app: GroceryLedgerApplication
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun clearData() = runBlocking {
        app.container.expenseRepository.clearAll()
    }

    @Test
    fun quantityAndUnitAreSavedAndShownAsAUnitPrice() {
        composeRule.onNodeWithContentDescription("Add item").performClick()
        composeRule.onNodeWithTag("entry_name").performTextInput("Rice")
        composeRule.onNodeWithTag("entry_amount").performTextInput("80")

        composeRule.onNodeWithText("+ Add quantity").performClick()
        composeRule.onNodeWithTag("entry_quantity").performTextInput("2")
        composeRule.onNodeWithTag("entry_unit_kg").performClick()

        composeRule.onNodeWithTag("entry_unit_price").assertTextContains("₹40/kg")

        composeRule.onNodeWithTag("entry_save")
            .performSemanticsAction(SemanticsActions.OnClick)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { app.container.expenseRepository.all().isNotEmpty() }
        }

        val saved = runBlocking { app.container.expenseRepository.all() }.single()
        assertEquals(2_000L, saved.quantityMilli)
        assertEquals("kg", saved.unit)
        assertEquals(80_00L, saved.amountPaise)
        assertEquals(40_00L, saved.unitPricePaise)
        assertEquals(LocalDate.now(), saved.date)
    }
}
