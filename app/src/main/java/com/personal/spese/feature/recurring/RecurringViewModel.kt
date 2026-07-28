package com.personal.spese.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.model.RecurringExpense
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.RecurringGenerator
import com.personal.spese.data.repository.RecurringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringViewModel(
    private val recurring: RecurringRepository,
    private val generator: RecurringGenerator,
    categories: CategoryRepository
) : ViewModel() {

    private val rules = MutableStateFlow<List<RecurringExpense>>(emptyList())

    init {
        viewModelScope.launch { recurring.observeAll().collect { rules.value = it } }
    }

    val uiState = combine(rules, categories.observeAll()) { rs, cats ->
        val nameById = cats.associate { it.id to it.name }
        RecurringUiState(
            items = rs.map { r ->
                RecurringRow(
                    id = r.id,
                    title = r.title,
                    amountCents = r.amountCents,
                    categoryName = nameById[r.categoryId] ?: "—",
                    dayOfMonth = r.dayOfMonth,
                    isActive = r.isActive
                )
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    fun setActive(id: Long, active: Boolean) = viewModelScope.launch {
        val rule = rules.value.firstOrNull { it.id == id } ?: return@launch
        if (active) {
            // Riattivazione: azzera il cursore e materializza subito i mesi mancanti (anche futuri).
            recurring.upsert(rule.copy(isActive = true, lastGeneratedPeriod = null))
            generator.generateUpTo()
        } else {
            // Disattivazione: rimuove le previsioni future, mantiene lo storico.
            recurring.upsert(rule.copy(isActive = false))
            generator.removeFutureInstances(id)
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        // Elimina prima le istanze future materializzate, poi la regola (lo storico passato resta).
        generator.removeFutureInstances(id)
        recurring.delete(id)
    }
}
