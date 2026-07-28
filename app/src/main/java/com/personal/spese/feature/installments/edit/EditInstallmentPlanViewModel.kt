package com.personal.spese.feature.installments.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.data.repository.CategoryRepository
import com.personal.spese.data.repository.InstallmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditInstallmentPlanViewModel(
    private val planId: Long,
    private val installments: InstallmentRepository,
    categories: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditInstallmentPlanUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            categories.observeActive().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
        viewModelScope.launch {
            installments.getPlan(planId)?.let { p ->
                _state.update {
                    it.copy(
                        loaded = true,
                        title = p.title,
                        categoryId = p.categoryId,
                        note = p.note.orEmpty()
                    )
                }
            }
        }
    }

    fun onTitleChange(v: String) = _state.update { it.copy(title = v, titleError = false) }
    fun onCategorySelect(id: Long) = _state.update { it.copy(categoryId = id, categoryError = false) }
    fun onNoteChange(v: String) = _state.update { it.copy(note = v) }

    fun save() {
        val s = _state.value
        val titleOk = s.title.isNotBlank()
        val categoryOk = s.categoryId != null
        if (!(titleOk && categoryOk)) {
            _state.update { it.copy(titleError = !titleOk, categoryError = !categoryOk) }
            return
        }
        viewModelScope.launch {
            installments.updatePlanMeta(planId, s.title, s.categoryId!!, s.note.ifBlank { null })
            _state.update { it.copy(saved = true) }
        }
    }
}
