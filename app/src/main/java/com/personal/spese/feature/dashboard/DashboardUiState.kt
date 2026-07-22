package com.personal.spese.feature.dashboard

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Quota di una categoria sul totale mese (frazione 0..1 per la barra). */
data class CategoryShare(
    val categoryId: Long,
    val categoryName: String,
    val totalCents: Long,
    val fraction: Float
)

/** Riga compatta usata nella sezione "Ultime spese" della dashboard. */
data class RecentExpenseRow(
    val id: Long,
    val amountCents: Long,
    val date: LocalDate,
    val categoryName: String,
    val note: String?
)

data class DashboardUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val totalCents: Long = 0L,
    val expenseCount: Int = 0,
    // Popolate in M7 (rate): in M6 restano placeholder.
    val hasInstallmentData: Boolean = false,
    val openInstallments: Int = 0,
    val installmentsResidualCents: Long = 0L,
    val categories: List<CategoryShare> = emptyList(),
    val recent: List<RecentExpenseRow> = emptyList()
) {
    val monthLabel: String
        get() {
            val m = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALY)
                .replaceFirstChar { it.uppercase(Locale.ITALY) }
            return "$m ${yearMonth.year}"
        }
}
