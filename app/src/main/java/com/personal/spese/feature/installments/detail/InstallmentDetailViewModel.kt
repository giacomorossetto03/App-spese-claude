package com.personal.spese.feature.installments.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.model.InstallmentPlan
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.InstallmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstallmentDetailViewModel(
    private val planId: Long,
    private val installments: InstallmentRepository,
    categories: CategoryRepository
) : ViewModel() {

    private val plan = MutableStateFlow<InstallmentPlan?>(null)

    init {
        viewModelScope.launch { plan.value = installments.getPlan(planId) }
    }

    val uiState = combine(
        plan,
        installments.observeEntries(planId),
        categories.observeAll()
    ) { p, entries, cats ->
        if (p == null) {
            InstallmentDetailUiState(loaded = false)
        } else {
            InstallmentDetailUiState(
                loaded = true,
                title = p.title,
                categoryName = cats.firstOrNull { it.id == p.categoryId }?.name ?: "—",
                totalCents = p.totalAmountCents,
                installmentsCount = p.installmentsCount,
                paidCount = entries.count { it.isPaid },
                residualCents = entries.filter { !it.isPaid }.sumOf { it.amountCents },
                note = p.note,
                entries = entries.map {
                    EntryRow(it.id, it.number, it.dueDate, it.amountCents, it.isPaid)
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallmentDetailUiState())

    fun togglePaid(entryId: Long, paid: Boolean) =
        viewModelScope.launch { installments.setPaid(entryId, paid) }

    fun deletePlan(onDeleted: () -> Unit) =
        viewModelScope.launch {
            installments.deletePlan(planId)
            onDeleted()
        }
}
