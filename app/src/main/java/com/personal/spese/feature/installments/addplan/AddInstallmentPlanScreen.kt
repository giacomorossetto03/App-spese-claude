package com.personal.spese.feature.installments.addplan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.spese.core.util.Money
import com.personal.spese.di.appContainer
import com.personal.spese.di.viewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ITALY)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInstallmentPlanScreen(onDone: () -> Unit) {
    val container = appContainer()
    val vm: AddInstallmentPlanViewModel = viewModel(
        factory = viewModelFactory {
            AddInstallmentPlanViewModel(container.installmentRepository, container.categoryRepository)
        }
    )
    val s by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(s.saved) { if (s.saved) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo piano di rate") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = s.title,
                onValueChange = vm::onTitleChange,
                label = { Text("Titolo (es. Divano)") },
                isError = s.titleError,
                supportingText = { if (s.titleError) Text("Inserisci un titolo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                val opts = listOf("Totale", "Con interessi")
                opts.forEachIndexed { i, label ->
                    SegmentedButton(
                        selected = (i == 1) == s.withInterest,
                        onClick = { vm.onWithInterestChange(i == 1) },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = opts.size)
                    ) { Text(label, maxLines = 1) }
                }
            }

            OutlinedTextField(
                value = s.amountInput,
                onValueChange = vm::onAmountChange,
                label = { Text(if (s.withInterest) "Prezzo (€)" else "Totale da pagare (€)") },
                isError = s.amountError,
                supportingText = {
                    if (s.amountError) Text("Inserisci importo/percentuale validi")
                    else if (!s.withInterest) Text("Per un piano già in corso, scrivi il residuo da pagare")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            if (s.withInterest) {
                OutlinedTextField(
                    value = s.interestInput,
                    onValueChange = vm::onInterestChange,
                    label = { Text("Interessi (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }

            OutlinedTextField(
                value = s.countInput,
                onValueChange = vm::onCountChange,
                label = { Text("Numero di rate") },
                isError = s.countError,
                supportingText = { if (s.countError) Text("Inserisci un numero >= 1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            CategoryDropdown(
                categories = s.categories.map { it.id to it.name },
                selectedId = s.categoryId,
                isError = s.categoryError,
                onSelect = vm::onCategorySelect
            )

            DateField(date = s.firstDueDate, onPick = vm::onDateChange)

            OutlinedTextField(
                value = s.note,
                onValueChange = vm::onNoteChange,
                label = { Text("Nota (facoltativa)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            PreviewText(
                withInterest = s.withInterest,
                amountInput = s.amountInput,
                interestInput = s.interestInput,
                countInput = s.countInput
            )

            Button(
                onClick = vm::save,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp)
            ) { Text("Crea piano") }
        }
    }
}

@Composable
private fun PreviewText(withInterest: Boolean, amountInput: String, interestInput: String, countInput: String) {
    val total = installmentTotalCents(withInterest, amountInput, interestInput)
    val count = countInput.toIntOrNull()
    val text = if (total != null && total > 0L && count != null && count >= 1) {
        val base = total / count
        val remainder = total - base * count
        val breakdown = if (remainder == 0L) {
            "$count rate da ${Money.format(base)}"
        } else {
            "${count - 1} rate da ${Money.format(base)} + ultima ${Money.format(base + remainder)}"
        }
        "$breakdown\nTotale da pagare: ${Money.format(total)}"
    } else {
        "Inserisci importo e numero di rate per l'anteprima."
    }
    Text(
        text,
        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Pair<Long, String>>,
    selectedId: Long?,
    isError: Boolean,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.first == selectedId }?.second ?: ""
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Categoria") },
            isError = isError,
            supportingText = { if (isError) Text("Seleziona una categoria") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(date: LocalDate, onPick: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date.format(dateFmt),
        onValueChange = {},
        readOnly = true,
        label = { Text("Prima scadenza") },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        trailingIcon = {
            TextButton(onClick = { show = true }) { Text("Scegli") }
        }
    )
    if (show) {
        val initMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Annulla") } }
        ) { DatePicker(state = pickerState) }
    }
}
