package dev.groceryledger.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetProgressTest {
    @Test
    fun underBudgetKeepsTheRemainder() {
        val progress = BudgetProgress(spentPaise = 320_000, budgetPaise = 500_000)

        assertEquals(0.64f, progress.fraction, 0.001f)
        assertEquals(180_000L, progress.remainingPaise)
        assertEquals(0L, progress.overspendPaise)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun overBudgetClampsTheBarAndReportsTheOverspend() {
        val progress = BudgetProgress(spentPaise = 600_000, budgetPaise = 500_000)

        assertEquals(1f, progress.fraction, 0.001f)
        assertEquals(0L, progress.remainingPaise)
        assertEquals(100_000L, progress.overspendPaise)
        assertTrue(progress.isOverBudget)
    }

    @Test
    fun spendingExactlyTheBudgetIsNotOver() {
        val progress = BudgetProgress(spentPaise = 500_000, budgetPaise = 500_000)

        assertEquals(1f, progress.fraction, 0.001f)
        assertEquals(0L, progress.remainingPaise)
        assertEquals(0L, progress.overspendPaise)
        assertFalse(progress.isOverBudget)
    }

    @Test
    fun aZeroBudgetDoesNotDivideByZero() {
        val progress = BudgetProgress(spentPaise = 1_000, budgetPaise = 0)

        assertEquals(0f, progress.fraction, 0.001f)
        assertEquals(1_000L, progress.overspendPaise)
        assertTrue(progress.isOverBudget)
    }
}
