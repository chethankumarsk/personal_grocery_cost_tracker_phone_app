package dev.groceryledger.domain.model

import java.time.DayOfWeek

enum class WeekStart(val dayOfWeek: DayOfWeek) {
    MONDAY(DayOfWeek.MONDAY),
    SUNDAY(DayOfWeek.SUNDAY),
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class AppSettings(
    val weekStart: WeekStart = WeekStart.MONDAY,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val monthlyBudgetPaise: Long? = null,
)
