package dev.groceryledger.domain.budget

/** Monthly spend against a target. All money is integer paise; [fraction] is display-only. */
data class BudgetProgress(
    val spentPaise: Long,
    val budgetPaise: Long,
) {
    val fraction: Float
        get() = if (budgetPaise <= 0L) {
            0f
        } else {
            (spentPaise.toDouble() / budgetPaise).coerceIn(0.0, 1.0).toFloat()
        }

    val remainingPaise: Long
        get() = (budgetPaise - spentPaise).coerceAtLeast(0L)

    val overspendPaise: Long
        get() = (spentPaise - budgetPaise).coerceAtLeast(0L)

    val isOverBudget: Boolean
        get() = spentPaise > budgetPaise
}
