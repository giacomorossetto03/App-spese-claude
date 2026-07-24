package com.personal.spese.feature.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.spese.core.model.ExpenseType
import com.personal.spese.core.util.Money
import com.personal.spese.di.appContainer
import com.personal.spese.di.viewModelFactory
import com.personal.spese.ui.theme.CategoryColors
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rowDateFmt = DateTimeFormatter.ofPattern("dd/MM", Locale.ITALY)

@Composable
fun ExpensesScreen(onEdit: (Long) -> Unit) {
    val container = appContainer()
    val vm: ExpensesViewModel = viewModel(
        factory = viewModelFactory { ExpensesViewModel(container.expenseRepository, container.categoryRepository) }
    )
    val s by vm.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize()) {
        MonthBar(label = s.monthLabel, onPrev = vm::prevMonth, onNext = vm::nextMonth)
        FiltersRow(
            categories = s.categories.map { it.id to it.name },
            categoryFilter = s.categoryFilter,
            typeFilter = s.typeFilter,
            onCategory = vm::setCategory,
            onType = vm::setType
        )
        HorizontalDivider()

        if (s.rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nessuna spesa in questo mese.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(s.rows, key = { it.id }) { row ->
                    ExpenseRow(row = row, onClick = { onEdit(row.id) }, onDelete = { pendingDelete = row.id })
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminare la spesa?") },
            text = { Text("L'operazione non è reversibile.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(id); pendingDelete = null }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun MonthBar(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mese precedente")
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mese successivo")
        }
    }
}

@Composable
private fun FiltersRow(
    categories: List<Pair<Long, String>>,
    categoryFilter: Long?,
    typeFilter: ExpenseType?,
    onCategory: (Long?) -> Unit,
    onType: (ExpenseType?) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryFilter(categories, categoryFilter, onCategory)
        FilterChip(
            selected = typeFilter == null,
            onClick = { onType(null) },
            label = { Text("Tutte") }
        )
        FilterChip(
            selected = typeFilter == ExpenseType.SINGLE,
            onClick = { onType(ExpenseType.SINGLE) },
            label = { Text("Singole") }
        )
    }
}

@Composable
private fun CategoryFilter(
    categories: List<Pair<Long, String>>,
    selected: Long?,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = categories.firstOrNull { it.first == selected }?.second ?: "Categoria"
    Box {
        TextButton(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Tutte le categorie") }, onClick = { onSelect(null); expanded = false })
            categories.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onSelect(id); expanded = false })
            }
        }
    }
}

@Composable
private fun ExpenseRow(row: ExpenseListRow, onClick: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(CategoryColors.forId(row.categoryId))
        )
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
            Text(row.categoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val meta = buildString {
                append(row.date.format(rowDateFmt))
                if (!row.note.isNullOrBlank()) append(" · ${row.note}")
                if (row.type == ExpenseType.RECURRING_INSTANCE) append(" · ricorrente")
            }
            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Money.format(row.amountCents), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Altre azioni")
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Modifica") }, onClick = { menu = false; onClick() })
                DropdownMenuItem(text = { Text("Elimina") }, onClick = { menu = false; onDelete() })
            }
        }
    }
}
