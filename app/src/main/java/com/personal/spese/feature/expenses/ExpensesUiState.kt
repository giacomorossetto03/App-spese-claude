package com.personal.spese.feature.expenses

import com.personal.spese.core.model.Category
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Natura della riga in lista: spesa singola, istanza ricorrente o rata di un piano. */
enum class RowKind { SINGLE, RECURRING, INSTALLMENT }

/** Filtro della lista spese (include le rate). */
enum class ExpenseFilter { ALL, SINGLE, RECURRING, INSTALLMENT }

/**
 * Riga unificata della lista: spese (`expense`) + rate dovute nel mese (`installment_entry`).
 * Per le rate, [note] porta il titolo del piano, [installmentInfo] "n/tot", [planId] per aprire il piano.
 */
data class ExpenseListRow(
    val id: Long,
    val kind: RowKind,
    val amountCents: Long,
    val date: LocalDate,
    val categoryId: Long,
    val categoryName: String,
    val note: String?,
    val installmentInfo: String? = null,
    val isPaid: Boolean = false,
    val planId: Long? = null
) {
    /** Chiave stabile e unica in lista: id di tabelle diverse possono collidere, il tipo li separa. */
    val listKey: String get() = "${kind.name}-$id"
}

data class ExpensesUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val categoryFilter: Long? = null,
    val filter: ExpenseFilter = ExpenseFilter.ALL,
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
