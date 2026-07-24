package com.personal.spese.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.model.ExpenseType
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel(
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository
) : ViewModel() {

    private data class Filters(
        val ym: YearMonth = YearMonth.now(),
        val category: Long? = null,
        val type: ExpenseType? = null
    )

    private val filters = MutableStateFlow(Filters())

    val uiState = combine(filters, categories.observeAll()) { f, cats -> f to cats }
        .flatMapLatest { (f, cats) ->
            expenses.observeMonth(f.ym, f.category, f.type).map { list ->
                val nameById = cats.associate { it.id to it.name }
                ExpensesUiState(
                    yearMonth = f.ym,
                    categoryFilter = f.category,
                    typeFilter = f.type,
                    categories = cats.filter { !it.isArchived },
                    rows = list.map { e ->
                        ExpenseListRow(e.id, e.amountCents, e.date, e.categoryId, nameById[e.categoryId] ?: "—", e.note, e.type)
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpensesUiState())

    fun prevMonth() = filters.update { it.copy(ym = it.ym.minusMonths(1)) }
    fun nextMonth() = filters.update { it.copy(ym = it.ym.plusMonths(1)) }
    fun setCategory(id: Long?) = filters.update { it.copy(category = id) }
    fun setType(type: ExpenseType?) = filters.update { it.copy(type = type) }

    fun delete(id: Long) = viewModelScope.launch { expenses.delete(id) }
}
