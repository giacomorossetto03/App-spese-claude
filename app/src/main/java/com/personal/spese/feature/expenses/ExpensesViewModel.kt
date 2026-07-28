package com.personal.spese.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.model.Expense
import com.personal.spese.core.model.ExpenseType
import com.personal.spese.core.model.InstallmentDue
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.ExpenseRepository
import com.personal.spese.data.repository.InstallmentRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel(
    private val expenses: ExpenseRepository,
    private val installments: InstallmentRepository,
    private val categories: CategoryRepository
) : ViewModel() {

    private data class Filters(
        val ym: YearMonth = YearMonth.now(),
        val category: Long? = null,
        val filter: ExpenseFilter = ExpenseFilter.ALL
    )

    private val filters = MutableStateFlow(Filters())

    val uiState = combine(filters, categories.observeAll()) { f, cats -> f to cats }
        .flatMapLatest { (f, cats) ->
            val nameById = cats.associate { it.id to it.name }

            // Sorgente spese: tipo derivato dal filtro; nessuna spesa se il filtro è "Rate".
            val expenseType = when (f.filter) {
                ExpenseFilter.SINGLE -> ExpenseType.SINGLE
                ExpenseFilter.RECURRING -> ExpenseType.RECURRING_INSTANCE
                else -> null
            }
            val expenseFlow: Flow<List<Expense>> =
                if (f.filter == ExpenseFilter.INSTALLMENT) flowOf(emptyList())
                else expenses.observeMonth(f.ym, f.category, expenseType)

            // Sorgente rate: nessuna rata se il filtro è "Singole" o "Ricorrenti".
            val installmentFlow: Flow<List<InstallmentDue>> =
                if (f.filter == ExpenseFilter.SINGLE || f.filter == ExpenseFilter.RECURRING) flowOf(emptyList())
                else installments.observeDueInMonth(f.ym, f.category)

            combine(expenseFlow, installmentFlow) { exp, due ->
                val expRows = exp.map { e ->
                    ExpenseListRow(
                        id = e.id,
                        kind = if (e.type == ExpenseType.RECURRING_INSTANCE) RowKind.RECURRING else RowKind.SINGLE,
                        amountCents = e.amountCents,
                        date = e.date,
                        categoryId = e.categoryId,
                        categoryName = nameById[e.categoryId] ?: "—",
                        note = e.note
                    )
                }
                val dueRows = due.map { d ->
                    ExpenseListRow(
                        id = d.entryId,
                        kind = RowKind.INSTALLMENT,
                        amountCents = d.amountCents,
                        date = d.dueDate,
                        categoryId = d.categoryId,
                        categoryName = nameById[d.categoryId] ?: "—",
                        note = d.title,
                        installmentInfo = "${d.number}/${d.installmentsCount}",
                        isPaid = d.isPaid,
                        planId = d.planId
                    )
                }
                val rows = (expRows + dueRows)
                    .sortedWith(compareByDescending<ExpenseListRow> { it.date }.thenByDescending { it.id })
                ExpensesUiState(
                    yearMonth = f.ym,
                    categoryFilter = f.category,
                    filter = f.filter,
                    categories = cats.filter { !it.isArchived },
                    rows = rows
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())

    fun prevMonth() = filters.update { it.copy(ym = it.ym.minusMonths(1)) }
    fun nextMonth() = filters.update { it.copy(ym = it.ym.plusMonths(1)) }
    fun setCategory(id: Long?) = filters.update { it.copy(category = id) }
    fun setFilter(filter: ExpenseFilter) = filters.update { it.copy(filter = filter) }

    fun delete(id: Long) = viewModelScope.launch { expenses.delete(id) }
}
