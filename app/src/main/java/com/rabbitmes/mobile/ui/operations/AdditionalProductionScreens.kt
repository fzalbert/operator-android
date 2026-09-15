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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
            OutlinedButton(onClick = onBack) { Text("Назад") }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = ProductionText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(task.description.ifBlank { task.operationTypeTitle }, color = ProductionMuted)
                if (task.status == TaskStatus.NEW && canEdit) {
                    Spacer(Modifier.height(6.dp))
                    ProductionButton("Приступить", onBegin)
                }
            }
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

@Composable
fun ProductionAnimalTransferTaskScreen(
    task: MobileTask,
    scannedRfid: String?,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onOpenScanner: (Map<String, String>) -> Unit,
    onValue: (String, String) -> Unit,
    onComplete: () -> Unit,
    canEdit: Boolean,
) {
    var rfid by remember(task.id) { mutableStateOf(task.result.values["rfid"].orEmpty()) }
    var destinationCell by remember(task.id) { mutableStateOf(task.result.values["cellId"].orEmpty()) }
    var comment by remember(task.id) { mutableStateOf(task.result.comment) }
    LaunchedEffect(scannedRfid) {
        scannedRfid?.takeIf(String::isNotBlank)?.let {
            rfid = it
            onValue("rfid", it)
        }
    }
    ProductionPage(task, "Перевод животных", onBack, onBegin, canEdit) {
        ProductionCard {
            Text("Данные перемещения", color = ProductionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(rfid, { rfid = it }, Modifier.fillMaxWidth(), label = { Text("RFID животного") }, singleLine = true)
            OutlinedButton(
                onClick = { onOpenScanner(mapOf("rfid" to rfid, "cellId" to destinationCell)) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Сканировать RFID") }
            OutlinedTextField(destinationCell, { destinationCell = it }, Modifier.fillMaxWidth(), label = { Text("Клетка назначения") }, singleLine = true)
            OutlinedTextField(comment, { comment = it }, Modifier.fillMaxWidth(), label = { Text("Комментарий") }, minLines = 2)
            ProductionButton(
                "Завершить перевод",
                {
                    onValue("rfid", rfid.trim())
                    onValue("cellId", destinationCell.trim())
                    onValue("comment", comment.trim())
                    onComplete()
                },
                rfid.isNotBlank() && destinationCell.isNotBlank(),
            )
        }
    }
}

@Composable
fun ProductionCleaningScreen(
    task: MobileTask,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onValue: (String, String) -> Unit,
    onComment: (String) -> Unit,
    onComplete: () -> Unit,
    canEdit: Boolean,
) {
    var passesSwept by remember(task.id) { mutableStateOf(task.result.values["passesSwept"] == "true") }
    var corpseFridge by remember(task.id) { mutableStateOf(task.result.values["corpseFridge"] == "true") }
    var comment by remember(task.id) { mutableStateOf(task.result.comment) }
    ProductionPage(task, "Уборка", onBack, onBegin, canEdit) {
        ProductionCard {
            Text("Контроль уборки", color = ProductionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            CheckRow("Проходы подметены", passesSwept) { passesSwept = it; onValue("passesSwept", it.toString()) }
            CheckRow("Падеж убран в холодильник", corpseFridge) { corpseFridge = it; onValue("corpseFridge", it.toString()) }
            OutlinedTextField(comment, { comment = it; onComment(it) }, Modifier.fillMaxWidth(), label = { Text("Комментарий") }, minLines = 2)
            ProductionButton(
                "Завершить уборку",
                {
                    onValue("passesSwept", passesSwept.toString())
                    onValue("corpseFridge", corpseFridge.toString())
                    onComment(comment.trim())
                    onComplete()
                },
                passesSwept,
            )
        }
    }
}

@Composable
fun ProductionLightCheckScreen(
    task: MobileTask,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onValue: (String, String) -> Unit,
    onComment: (String) -> Unit,
    onComplete: () -> Unit,
    canEdit: Boolean,
) {
    var allLamps by remember(task.id) { mutableStateOf(task.result.values["allLamps"] == "true") }
    var lightHours by remember(task.id) { mutableStateOf(task.result.values["lightHours"].orEmpty()) }
    var broken by remember(task.id) { mutableStateOf(task.result.values["broken"].orEmpty()) }
    var comment by remember(task.id) { mutableStateOf(task.result.comment) }
    ProductionPage(task, "Проверка светового режима", onBack, onBegin, canEdit) {
        ProductionCard {
            Text("Результат проверки", color = ProductionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            CheckRow("Все лампы горят", allLamps) { allLamps = it; onValue("allLamps", it.toString()) }
            OutlinedTextField(
                lightHours,
                { lightHours = it; onValue("lightHours", it) },
                Modifier.fillMaxWidth(),
                label = { Text("Световой день, часов") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                broken,
                { broken = it.filter(Char::isDigit); onValue("broken", broken) },
                Modifier.fillMaxWidth(),
                label = { Text("Перегоревшие лампы") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            OutlinedTextField(comment, { comment = it; onComment(it) }, Modifier.fillMaxWidth(), label = { Text("Комментарий") }, minLines = 2)
            ProductionButton(
                "Сохранить проверку",
                {
                    onValue("allLamps", allLamps.toString())
                    onValue("lightHours", lightHours.trim())
                    onValue("broken", broken.ifBlank { "0" })
                    onComment(comment.trim())
                    onComplete()
                },
                lightHours.toDoubleOrNull()?.let { it > 0 } == true,
            )
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = ProductionText, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Switch(checked, onChecked)
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
    var weight by remember(openedId) { mutableStateOf("") }
    var expandedCageId by remember(task.id) { mutableStateOf<String?>(null) }
    val cageGroups = task.checklist
        .groupBy { it.cageId ?: "unknown" }
        .toList()
        .sortedBy { (cageId) -> cageId.toLongOrNull() ?: Long.MAX_VALUE }

    ProductionPage(task, "Взвешивание кролика", onBack, onBegin, canEdit) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Клеток: ${cageGroups.size} · Кроликов: ${task.checklist.size}",
                color = ProductionMuted,
                fontWeight = FontWeight.Bold,
            )
            cageGroups.forEach { (cageKey, rabbits) ->
                val expanded = expandedCageId == cageKey
                val completedCount = rabbits.count { it.status != ChecklistStatus.PENDING }
                RabbitCageWeightCard(
                    cageKey = cageKey,
                    rabbits = rabbits,
                    expanded = expanded,
                    completedCount = completedCount,
                    openedId = openedId,
                    weight = weight,
                    onToggle = {
                        expandedCageId = if (expanded) null else cageKey
                        openedId = null
                    },
                    onOpenedId = { openedId = it },
                    onWeight = { weight = it },
                    onDone = onDone,
                    onProblem = onProblem,
                )
            }
            if (task.checklist.isNotEmpty() && task.checklist.all { it.status != ChecklistStatus.PENDING }) {
                ProductionCard {
                    Text("Все кролики взвешены", color = ProductionText, fontWeight = FontWeight.Bold)
                    ProductionButton("Завершить задачу", onComplete)
                }
            }
        }
    }
}

@Composable
private fun RabbitCageWeightCard(
    cageKey: String,
    rabbits: List<ChecklistItem>,
    expanded: Boolean,
    completedCount: Int,
    openedId: String?,
    weight: String,
    onToggle: () -> Unit,
    onOpenedId: (String?) -> Unit,
    onWeight: (String) -> Unit,
    onDone: (String, Map<String, String>) -> Unit,
    onProblem: (String, String, String) -> Unit,
) {
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
                    rabbits.firstOrNull()?.cageLabel ?: cageKey.takeUnless { it == "unknown" }?.let { "Клетка $it" } ?: "Клетка не указана",
                    color = ProductionText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Кроликов: ${rabbits.size} · Готово: $completedCount",
                    color = ProductionMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(if (expanded) "Скрыть  ▲" else "Открыть  ▼", color = ProductionGreen, fontWeight = FontWeight.Bold)
        }
        if (expanded) {
            HorizontalDivider(color = Color(0xFFE0E8E4))
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rabbits.forEachIndexed { index, target ->
                    val opened = openedId == target.id
                    val completed = target.status != ChecklistStatus.PENDING
                    Column(
                        Modifier.fillMaxWidth().background(Color(0xFFF6F9F7), RoundedCornerShape(8.dp)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Кролик ${index + 1}", color = ProductionText, fontWeight = FontWeight.Bold)
                            Text(if (completed) "Готово" else "Ожидает", color = if (completed) ProductionGreen else ProductionMuted, fontSize = 13.sp)
                        }
                        if (!completed && opened) {
                            OutlinedTextField(
                                weight,
                                { onWeight(it.filter(Char::isDigit)) },
                                Modifier.fillMaxWidth(),
                                label = { Text("Вес кролика, г") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                            )
                            ProductionButton("Сохранить вес", {
                                onDone(target.id, mapOf("weightGrams" to weight))
                                onOpenedId(null)
                            }, weight.toIntOrNull()?.let { it > 0 } == true)
                            OutlinedButton({ onProblem(target.id, "Не удалось взвесить", ""); onOpenedId(null) }, Modifier.fillMaxWidth()) {
                                Text("Не удалось взвесить", color = ProductionProblem)
                            }
                        } else if (!completed) {
                            OutlinedButton(onClick = { onOpenedId(target.id) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Ввести вес")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductionTargetPage(
    task: MobileTask,
    title: String,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onComplete: () -> Unit,
    canEdit: Boolean,
    targetContent: @Composable (ChecklistItem) -> Unit,
) {
    val pending = task.checklist.filter { it.status == ChecklistStatus.PENDING }
    ProductionPage(task, title, onBack, onBegin, canEdit) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ожидает: ${pending.size} · Готово: ${task.checklist.size - pending.size}", color = ProductionMuted, fontWeight = FontWeight.Bold)
            for (target in pending) targetContent(target)
            if (task.checklist.isNotEmpty() && pending.isEmpty()) {
                ProductionCard {
                    Text("Все позиции обработаны", color = ProductionText, fontWeight = FontWeight.Bold)
                    ProductionButton("Завершить задачу", onComplete)
                }
            }
        }
    }
}
