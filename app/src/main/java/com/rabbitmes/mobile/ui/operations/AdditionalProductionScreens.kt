
package com.rabbitmes.mobile.ui.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.ColumnScope
import com.rabbitmes.mobile.domain.ChecklistItem
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.TaskStatus

private val ProductionBackground = Color(0xFFF1F5F3)
private val ProductionGreen = Color(0xFF1F8A5B)
private val ProductionText = Color(0xFF10231B)
private val ProductionMuted = Color(0xFF60726A)
private val ProductionProblem = Color(0xFFDC4C4C)

@Composable
private fun ProductionPage(
    task: MobileTask,
    title: String,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    canEdit: Boolean,
    content: @Composable () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(ProductionBackground).statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            UnifiedTaskHeader(
                task = task,
                onBack = onBack,
                onBegin = onBegin,
                canEdit = canEdit,
                title = task.title.ifBlank { title },
            )
        }
        if (!canEdit) {
            item { UnifiedReadOnlyNotice() }
        }
        if (task.status != TaskStatus.NEW && canEdit) item { content() }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProductionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ProductionButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = ProductionGreen),
        shape = RoundedCornerShape(8.dp),
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

/**
 * Задачу начали, а целей у неё нет: например, в ангаре нет животных. Выполнять нечего,
 * поэтому задачу закрывают замечанием.
 */
@Composable
fun ProductionNoTargetsScreen(
    task: MobileTask,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onReportProblem: (String) -> Unit,
    canEdit: Boolean,
) {
    var comment by remember(task.id) { mutableStateOf("") }
    ProductionPage(task, task.operationTypeTitle, onBack, onBegin, canEdit) {
        ProductionCard {
            Text("Выполнять нечего", color = ProductionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Сервер не передал ни одной цели. Опишите, почему задачу нельзя выполнить, и она закроется с замечанием.",
                color = ProductionMuted,
            )
            OutlinedTextField(
                comment,
                { comment = it },
                Modifier.fillMaxWidth().forceSoftwareKeyboardOnFocus(),
                label = { Text("Замечание") },
                minLines = 2,
            )
            ProductionButton("Оставить замечание", { onReportProblem(comment.trim()) }, comment.isNotBlank())
        }
    }
}

@Composable
fun ProductionRabbitWeighingScreen(
    task: MobileTask,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onDone: (String, Map<String, String>) -> Unit,
    onProblem: (String, String, String) -> Unit,
    onComplete: () -> Unit,
    canEdit: Boolean,
) {
    var openedId by remember(task.id) { mutableStateOf<String?>(null) }
    var weights by remember(openedId) { mutableStateOf(listOf("")) }
    val cageTargets = task.checklist.sortedBy { target ->
        (target.cageId ?: target.targetId).toLongOrNull() ?: Long.MAX_VALUE
    }
    val completedCount = cageTargets.count { it.status != ChecklistStatus.PENDING }

    ProductionPage(task, "Взвешивание кролика", onBack, onBegin, canEdit) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Клеток: ${cageTargets.size} · Готово: $completedCount",
                color = ProductionMuted,
                fontWeight = FontWeight.Bold,
            )
            cageTargets.forEach { target ->
                val opened = openedId == target.id
                RabbitCageWeightCard(
                    target = target,
                    opened = opened,
                    weights = weights,
                    onToggle = {
                        openedId = if (opened) null else target.id
                    },
                    onWeightChange = { index, value ->
                        weights = weights.toMutableList().also { it[index] = value }
                    },
                    onAddWeight = { weights = weights + "" },
                    onRemoveWeight = { index ->
                        weights = weights.filterIndexed { itemIndex, _ -> itemIndex != index }
                            .ifEmpty { listOf("") }
                    },
                    onDone = {
                        val validWeights = weights.mapNotNull(String::toIntOrNull).filter { it > 0 }
                        onDone(target.id, mapOf("weightsGrams" to validWeights.joinToString(",")))
                        openedId = null
                    },
                    onProblem = {
                        onProblem(target.id, "Не удалось взвесить", "")
                        openedId = null
                    },
                )
            }
            if (cageTargets.isNotEmpty() && completedCount == cageTargets.size) {
                ProductionCard {
                    Text("Все клетки обработаны", color = ProductionText, fontWeight = FontWeight.Bold)
                    ProductionButton("Завершить задачу", onComplete)
                }
            }
        }
    }
}

@Composable
private fun RabbitCageWeightCard(
    target: ChecklistItem,
    opened: Boolean,
    weights: List<String>,
    onToggle: () -> Unit,
    onWeightChange: (Int, String) -> Unit,
    onAddWeight: () -> Unit,
    onRemoveWeight: (Int) -> Unit,
    onDone: () -> Unit,
    onProblem: () -> Unit,
) {
    val completed = target.status != ChecklistStatus.PENDING
    val enteredWeights = weights.mapNotNull(String::toIntOrNull).filter { it > 0 }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    target.cageLabel ?: target.label,
                    color = ProductionText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    when {
                        completed -> "Взвешивание завершено"
                        target.rabbitCount != null -> "Мясных кроликов в клетке: ${target.rabbitCount}"
                        else -> "Введите результаты взвешивания"
                    },
                    color = ProductionMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!completed) {
                Text(if (opened) "Скрыть  ▲" else "Открыть  ▼", color = ProductionGreen, fontWeight = FontWeight.Bold)
            }
        }
        if (opened && !completed) {
            HorizontalDivider(color = Color(0xFFE0E8E4))
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weights.forEachIndexed { index, weight ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = weight,
                            onValueChange = { onWeightChange(index, normalizeWholeNumberInput(it)) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Кролик ${index + 1}, г") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                        if (weights.size > 1) {
                            IconButton(onClick = { onRemoveWeight(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить вес кролика ${index + 1}")
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onAddWeight, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Добавить кролика")
                }
                ProductionButton(
                    label = "Сохранить веса (${enteredWeights.size})",
                    onClick = onDone,
                    enabled = enteredWeights.isNotEmpty() && enteredWeights.size == weights.size,
                )
                OutlinedButton(onClick = onProblem, modifier = Modifier.fillMaxWidth()) {
                    Text("Не удалось взвесить", color = ProductionProblem)
                }
            }
        }
    }
}
