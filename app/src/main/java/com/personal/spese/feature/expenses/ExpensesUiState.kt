package com.personal.spese.feature.expenses

import com.personal.spese.core.model.Category
import com.personal.spese.core.model.ExpenseType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class ExpenseListRow(
    val id: Long,
    val amountCents: Long,
    val date: LocalDate,
    val categoryName: String,
    val note: String?,
    val type: ExpenseType
)

data class ExpensesUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val categoryFilter: Long? = null,
    val typeFilter: ExpenseType? = null,
    val categories: List<Category> = emptyList(),
    val rows: List<ExpenseListRow> = emptyList()
) {
    val monthLabel: String
        get() {
            val m = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALY)
                .replaceFirstChar { it.uppercase(Locale.ITALY) }
            return "$m ${yearMonth.year}"
        }
}
