package com.personal.spese.feature.installments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.spese.core.util.Money
import com.personal.spese.di.appContainer
import com.personal.spese.di.viewModelFactory

@Composable
fun InstallmentsScreen(onAddPlan: () -> Unit, onOpenPlan: (Long) -> Unit) {
    val container = appContainer()
    val vm: InstallmentsViewModel = viewModel(
        factory = viewModelFactory {
            InstallmentsViewModel(container.installmentRepository, container.categoryRepository)
        }
    )
    val s by vm.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Piani di rate", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onAddPlan) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Nuovo piano", modifier = Modifier.padding(start = 4.dp))
            }
        }
        HorizontalDivider()

        if (s.plans.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Nessun piano di rate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(s.plans, key = { it.planId }) { plan ->
                    PlanItem(plan = plan, onClick = { onOpenPlan(plan.planId) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PlanItem(plan: PlanRow, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(plan.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "${plan.paidCount}/${plan.totalCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { if (plan.totalCount > 0) plan.paidCount.toFloat() / plan.totalCount.toFloat() else 0f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        )
        Text(
            "${plan.categoryName} · residuo ${Money.format(plan.residualCents)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
