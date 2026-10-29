package dev.groceryledger.domain.time

import java.time.LocalDate

fun interface DateProvider {
    fun today(): LocalDate
}

object SystemDateProvider : DateProvider {
    override fun today(): LocalDate = LocalDate.now()
}
