package com.personal.spese.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.spese.core.model.Category
import com.personal.spese.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repo: CategoryRepository) : ViewModel() {

    val uiState = repo.observeAll()
        .map { CategoriesUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val next = (uiState.value.categories.maxOfOrNull { it.sortOrder } ?: -1) + 1
        viewModelScope.launch {
            repo.upsert(Category(name = trimmed, isDefault = false, sortOrder = next))
        }
    }

    fun rename(category: Category, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == category.name) return
        viewModelScope.launch { repo.upsert(category.copy(name = trimmed)) }
    }

    fun setArchived(id: Long, archived: Boolean) = viewModelScope.launch {
        if (archived) repo.archive(id) else repo.unarchive(id)
    }
}
