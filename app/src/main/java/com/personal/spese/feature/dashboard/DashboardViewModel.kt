package com.personal.spese.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.datastore.SettingsDataStore
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.DashboardRepository
import com.personal.spese.data.repository.InstallmentSummary
import com.personal.spese.data.repository.MonthTotal
import com.personal.spese.core.model.Category
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth

/** Sorgenti ausiliarie (categorie, riepilogo rate, andamento, budget) per il combine annidato. */
private data class DashboardAux(
    val categories: List<Category>,
    val summary: InstallmentSummary,
    val trend: List<MonthTotal>,
    val budgetCents: Long
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val dashboard: DashboardRepository,
    private val categories: CategoryRepository,
    private val settings: SettingsDataStore
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val uiState = month.flatMapLatest { ym ->
        combine(
            dashboard.totalInMonth(ym),
            dashboard.countInMonth(ym),
            dashboard.categoryBreakdown(ym),
            dashboard.recentExpenses(5),
            // Combine annidato: mantiene l'arità del combine esterno a 5.
            combine(
                categories.observeAll(),
                dashboard.installmentSummary(),
                dashboard.monthlyTrend(ym, 6),
                settings.monthlyBudgetCents
            ) { cats, summary, trend, budget -> DashboardAux(cats, summary, trend, budget) }
        ) { total, count, breakdown, recent, (cats, summary, trend, budget) ->
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
                movementCount = count,
                openInstallments = summary.openCount,
                installmentsResidualCents = summary.residualCents,
                budgetCents = budget,
                categories = shares,
                trend = trend,
                recent = recent.map { e ->
                    RecentExpenseRow(
                        id = e.id,
                        amountCents = e.amountCents,
                        date = e.date,
                        categoryId = e.categoryId,
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
