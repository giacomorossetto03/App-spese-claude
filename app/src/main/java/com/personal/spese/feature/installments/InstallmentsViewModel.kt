package com.personal.spese.feature.installments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.InstallmentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class InstallmentsViewModel(
    installments: InstallmentRepository,
    categories: CategoryRepository
) : ViewModel() {

    val uiState = combine(
        installments.observePlansProgress(),
        categories.observeAll()
    ) { plans, cats ->
        val nameById = cats.associate { it.id to it.name }
        InstallmentsUiState(
            plans = plans.map { p ->
                PlanRow(
                    planId = p.planId,
                    title = p.title,
                    categoryName = nameById[p.categoryId] ?: "—",
                    paidCount = p.paidCount,
                    totalCount = p.totalCount,
                    residualCents = p.residualCents
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallmentsUiState())
}
