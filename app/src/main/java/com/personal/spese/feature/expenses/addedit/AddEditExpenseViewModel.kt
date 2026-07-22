package com.personal.spese.feature.expenses.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.model.Expense
import com.personal.spese.core.model.ExpenseType
import com.personal.spese.core.util.Money
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddEditExpenseViewModel(
    private val expenseId: Long,
    private val expenses: ExpenseRepository,
    private val categories: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditExpenseUiState(isEdit = expenseId != -1L))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categories.observeActive().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
        if (expenseId != -1L) {
            viewModelScope.launch {
                expenses.getById(expenseId)?.let { e ->
                    _state.update {
                        it.copy(
                            type = e.type,
                            amountInput = centsToInput(e.amountCents),
                            categoryId = e.categoryId,
                            date = e.date,
                            note = e.note.orEmpty(),
                            paymentMethod = e.paymentMethod.orEmpty()
                        )
                    }
                }
            }
        }
    }

    fun onAmountChange(v: String) = _state.update { it.copy(amountInput = v, amountError = false) }
    fun onCategorySelect(id: Long) = _state.update { it.copy(categoryId = id, categoryError = false) }
    fun onDateChange(d: LocalDate) = _state.update { it.copy(date = d) }
    fun onNoteChange(v: String) = _state.update { it.copy(note = v) }
    fun onPaymentChange(v: String) = _state.update { it.copy(paymentMethod = v) }

    fun save() {
        val s = _state.value
        val cents = Money.parseToCents(s.amountInput)
        val amountOk = cents != null && cents > 0
        val categoryOk = s.categoryId != null
        if (!amountOk || !categoryOk) {
            _state.update { it.copy(amountError = !amountOk, categoryError = !categoryOk) }
            return
        }
        viewModelScope.launch {
            expenses.save(
                Expense(
                    id = if (s.isEdit) expenseId else 0L,
                    amountCents = cents!!,
                    date = s.date,
                    categoryId = s.categoryId!!,
                    note = s.note.trim().ifBlank { null },
                    paymentMethod = s.paymentMethod.trim().ifBlank { null },
                    type = ExpenseType.SINGLE
                )
            )
            _state.update { it.copy(saved = true) }
        }
    }

    private fun centsToInput(cents: Long): String {
        val euros = cents / 100
        val rem = (cents % 100).toInt()
        return "%d,%02d".format(euros, rem)
    }
}
