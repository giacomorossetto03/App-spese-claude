package com.personal.spese.feature.recurring.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.model.RecurringExpense
import com.personal.spese.core.util.Money
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.RecurringGenerator
import com.personal.spese.data.repository.RecurringRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class RecurringEditViewModel(
    private val recurringId: Long,
    private val recurring: RecurringRepository,
    private val generator: RecurringGenerator,
    categories: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecurringEditUiState(isEdit = recurringId > 0L))
    val state = _state.asStateFlow()

    // Preservato in modifica per non rigenerare i mesi già materializzati.
    private var lastGeneratedPeriod: Int? = null

    init {
        viewModelScope.launch {
            categories.observeActive().collect { cats ->
                _state.update { it.copy(categories = cats, categoryId = it.categoryId ?: cats.firstOrNull()?.id) }
            }
        }
        if (recurringId > 0L) {
            viewModelScope.launch {
                recurring.getById(recurringId)?.let { r ->
                    lastGeneratedPeriod = r.lastGeneratedPeriod
                    _state.update {
                        it.copy(
                            title = r.title,
                            amountInput = centsToInput(r.amountCents),
                            dayInput = r.dayOfMonth.toString(),
                            categoryId = r.categoryId,
                            startDate = r.startDate,
                            hasEnd = r.endDate != null,
                            endDate = r.endDate ?: LocalDate.now(),
                            isActive = r.isActive
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v, titleError = false) }
    fun onAmountChange(v: String) = _state.update { it.copy(amountInput = v, amountError = false) }
    fun onDayChange(v: String) = _state.update { it.copy(dayInput = v.filter(Char::isDigit).take(2), dayError = false) }
    fun onCategorySelect(id: Long) = _state.update { it.copy(categoryId = id, categoryError = false) }
    fun onStartDateChange(d: LocalDate) = _state.update { it.copy(startDate = d) }
    fun onHasEndChange(v: Boolean) = _state.update { it.copy(hasEnd = v) }
    fun onEndDateChange(d: LocalDate) = _state.update { it.copy(endDate = d) }
    fun onActiveChange(v: Boolean) = _state.update { it.copy(isActive = v) }

    fun save() {
        val s = _state.value
        val cents = Money.parseToCents(s.amountInput)
        val day = s.dayInput.toIntOrNull()

        val titleOk = s.title.isNotBlank()
        val amountOk = cents != null && cents > 0
        val dayOk = day != null && day in 1..28
        val categoryOk = s.categoryId != null

        if (!(titleOk && amountOk && dayOk && categoryOk)) {
            _state.update {
                it.copy(
                    titleError = !titleOk,
                    amountError = !amountOk,
                    dayError = !dayOk,
                    categoryError = !categoryOk
                )
            }
            return
        }

        viewModelScope.launch {
            recurring.upsert(
                RecurringExpense(
                    id = if (s.isEdit) recurringId else 0L,
                    title = s.title.trim(),
                    amountCents = cents!!,
                    categoryId = s.categoryId!!,
                    dayOfMonth = day!!,
                    startDate = s.startDate,
                    endDate = if (s.hasEnd) s.endDate else null,
                    isActive = s.isActive,
                    lastGeneratedPeriod = lastGeneratedPeriod
                )
            )
            // In modifica: allinea le istanze già create ai nuovi valori (categoria/importo/titolo).
            if (s.isEdit) {
                generator.syncExistingInstances(recurringId, s.categoryId!!, cents, s.title.trim())
            }
            // Materializza subito eventuali istanze dovute.
            generator.generateUpTo()
            _state.update { it.copy(saved = true) }
        }
    }

    private fun centsToInput(cents: Long): String {
        val euros = cents / 100
        val rem = (cents % 100).toInt()
        return "%d,%02d".format(euros, rem)
    }
}
