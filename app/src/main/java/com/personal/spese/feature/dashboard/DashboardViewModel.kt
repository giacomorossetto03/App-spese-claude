package com.personal.spese.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.DashboardRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val dashboard: DashboardRepository,
    private val categories: CategoryRepository
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState = month.flatMapLatest { ym ->
        combine(
            dashboard.totalInMonth(ym),
            dashboard.countInMonth(ym),
            dashboard.categoryBreakdown(ym),
            dashboard.recentExpenses(5),
            categories.observeAll()
        ) { total, count, breakdown, recent, cats ->
            val nameById = cats.associate { it.id to it.name }
            val shares = breakdown
                .sortedByDescending { it.total }
                .map { cs ->
                    CategoryShare(
                        categoryId = cs.categoryId,
                        categoryName = nameById[cs.categoryId] ?: "—",
                        totalCents = cs.total,
                        fraction = if (total > 0L) cs.total.toFloat() / total.toFloat() else 0f
                    )
                }
            DashboardUiState(
                yearMonth = ym,
                totalCents = total,
                expenseCount = count,
                categories = shares,
                recent = recent.map { e ->
                    RecentExpenseRow(
                        id = e.id,
                        amountCents = e.amountCents,
                        date = e.date,
                        categoryName = nameById[e.categoryId] ?: "—",
                        note = e.note
                    )
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun prevMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }
}
