package com.personal.spese.feature.installments.addplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.util.Money
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.InstallmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddInstallmentPlanViewModel(
    private val installments: InstallmentRepository,
    categories: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddInstallmentPlanUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categories.observeActive().collect { cats ->
                _state.update { st ->
                    st.copy(
                        categories = cats,
                        categoryId = st.categoryId ?: cats.firstOrNull()?.id
                    )
                }
            }
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v, titleError = false) }
    fun onAmountChange(v: String) = _state.update { it.copy(amountInput = v, amountError = false) }
    fun onCountChange(v: String) = _state.update { it.copy(countInput = v.filter(Char::isDigit), countError = false) }
    fun onDateChange(d: LocalDate) = _state.update { it.copy(firstDueDate = d) }
    fun onCategorySelect(id: Long) = _state.update { it.copy(categoryId = id, categoryError = false) }
    fun onNoteChange(v: String) = _state.update { it.copy(note = v) }

    fun save() {
        val st = _state.value
        val cents = Money.parseToCents(st.amountInput)
        val count = st.countInput.toIntOrNull()

        val titleOk = st.title.isNotBlank()
        val amountOk = cents != null && cents > 0
        val countOk = count != null && count >= 1
        val categoryOk = st.categoryId != null

        if (!(titleOk && amountOk && countOk && categoryOk)) {
            _state.update {
                it.copy(
                    titleError = !titleOk,
                    amountError = !amountOk,
                    countError = !countOk,
                    categoryError = !categoryOk
                )
            }
            return
        }

        viewModelScope.launch {
            installments.createPlan(
                title = st.title,
                totalAmountCents = cents!!,
                installmentsCount = count!!,
                firstDueDate = st.firstDueDate,
                categoryId = st.categoryId!!,
                note = st.note.ifBlank { null }
            )
            _state.update { it.copy(saved = true) }
        }
    }
}
