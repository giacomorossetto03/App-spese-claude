package com.personal.spese.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.spese.core.util.Money
import com.personal.spese.data.repository.MonthTotal
import com.personal.spese.di.appContainer
import com.personal.spese.di.viewModelFactory
import com.personal.spese.ui.theme.CategoryColors
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rowDateFmt = DateTimeFormatter.ofPattern("dd/MM", Locale.ITALY)

@Composable
fun DashboardScreen(onOpenExpense: (Long) -> Unit = {}) {
    val container = appContainer()
    val vm: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            DashboardViewModel(container.dashboardRepository, container.categoryRepository, container.settings)
        }
    )
    val s by vm.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        MonthBar(label = s.monthLabel, onPrev = vm::prevMonth, onNext = vm::nextMonth)
        SummaryGrid(state = s)
        BudgetSection(spentCents = s.totalCents, budgetCents = s.budgetCents)
        TrendSection(trend = s.trend)
        CategoryBreakdown(shares = s.categories, totalCents = s.totalCents)
        RecentExpenses(rows = s.recent, onOpenExpense = onOpenExpense)
    }
}

@Composable
private fun MonthBar(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
private fun SummaryGrid(state: DashboardUiState) {
    Column(
        Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                title = "Totale mese",
                value = Money.format(state.totalCents),
                icon = Icons.Outlined.Payments,
                modifier = Modifier.weight(1f),
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            )
            SummaryCard(
                title = "N° movimenti",
                value = state.movementCount.toString(),
                icon = Icons.Outlined.Receipt,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                title = "Rate aperte",
                value = state.openInstallments.toString(),
                icon = Icons.Outlined.CalendarMonth,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Residuo rate",
                value = Money.format(state.installmentsResidualCents),
                icon = Icons.Outlined.Savings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = content,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
private fun BudgetSection(spentCents: Long, budgetCents: Long) {
    if (budgetCents <= 0L) return
    val fraction = (spentCents.toFloat() / budgetCents.toFloat()).coerceIn(0f, 1f)
    val over = spentCents > budgetCents
    SectionTitle("Budget del mese")
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${Money.format(spentCents)} di ${Money.format(budgetCents)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (over) "sforato di ${Money.format(spentCents - budgetCents)}"
                    else "rimane ${Money.format(budgetCents - spentCents)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { fraction },
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun TrendSection(trend: List<MonthTotal>) {
    if (trend.isEmpty()) return
    SectionTitle("Andamento (ultimi mesi)")
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            TrendChart(
                data = trend,
                barColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                trend.forEach { m ->
                    Text(
                        m.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendChart(data: List<MonthTotal>, barColor: Color, modifier: Modifier = Modifier) {
    val max = (data.maxOfOrNull { it.totalCents } ?: 0L).coerceAtLeast(1L)
    Canvas(modifier) {
        val n = data.size
        if (n == 0) return@Canvas
        val gap = size.width * 0.04f
        val barW = (size.width - gap * (n - 1)) / n
        data.forEachIndexed { i, m ->
            val h = if (m.totalCents <= 0L) 0f else size.height * (m.totalCents.toFloat() / max.toFloat())
            val left = i * (barW + gap)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }
}

@Composable
private fun DonutChart(slices: List<Pair<Color, Float>>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.16f
        val d = size.minDimension - stroke
        val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
        var start = -90f
        slices.forEach { (color, fraction) ->
            val sweep = fraction * 360f
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            start += sweep
        }
    }
}

@Composable
private fun CategoryBreakdown(shares: List<CategoryShare>, totalCents: Long) {
    SectionTitle("Ripartizione categorie")
    if (shares.isEmpty()) {
        EmptyHint("Nessuna spesa in questo mese.")
        return
    }
    Box(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        DonutChart(
            slices = shares.map { CategoryColors.forId(it.categoryId) to it.fraction },
            modifier = Modifier.size(160.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Totale",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                Money.format(totalCents),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        shares.forEach { share ->
            val color = CategoryColors.forId(share.categoryId)
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(color)
                    Text(
                        share.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    Text(
                        Money.format(share.totalCents),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                LinearProgressIndicator(
                    progress = { share.fraction },
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentExpenses(rows: List<RecentExpenseRow>, onOpenExpense: (Long) -> Unit) {
    SectionTitle("Ultime spese")
    if (rows.isEmpty()) {
        EmptyHint("Ancora nessuna spesa registrata.")
        return
    }
    Column(Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpenExpense(row.id) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorDot(CategoryColors.forId(row.categoryId))
                Column(Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)) {
                    Text(
                        row.categoryName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    val meta = buildString {
                        append(row.date.format(rowDateFmt))
                        if (!row.note.isNullOrBlank()) append(" · ${row.note}")
                    }
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    Money.format(row.amountCents),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
