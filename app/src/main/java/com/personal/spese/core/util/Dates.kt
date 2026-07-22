package com.personal.spese.core.util

import java.time.LocalDate
import java.time.YearMonth

/** Date come epochDay (Long) nel DB. Periodo mese come Int YYYYMM. */
object Dates {

    fun toEpochDay(date: LocalDate): Long = date.toEpochDay()

    fun fromEpochDay(day: Long): LocalDate = LocalDate.ofEpochDay(day)

    /** Confini half-open del mese in epochDay: (primo giorno incluso, primo del mese dopo escluso). */
    fun monthBounds(ym: YearMonth): Pair<Long, Long> {
        val start = ym.atDay(1).toEpochDay()
        val end = ym.plusMonths(1).atDay(1).toEpochDay()
        return start to end
    }

    fun period(ym: YearMonth): Int = ym.year * 100 + ym.monthValue

    fun today(): LocalDate = LocalDate.now()

    fun currentYearMonth(): YearMonth = YearMonth.now()
}
