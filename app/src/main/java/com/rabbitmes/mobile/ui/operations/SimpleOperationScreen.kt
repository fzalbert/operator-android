package com.rabbitmes.mobile.ui.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rabbitmes.mobile.domain.*

private val SimpleGreen = Color(0xFF1F8A5B)
private val SimpleDarkGreen = Color(0xFF0B2F24)
private val SimpleBackground = Color(0xFFF1F5F3)
private val SimpleText = Color(0xFF10231B)
private val SimpleMuted = Color(0xFF60726A)
private val SimpleBorder = Color(0xFFDCE6E1)
private val SimpleRed = Color(0xFFDC4C4C)

@Composable
fun SimpleOperationScreen(
    task: MobileTask,
    definition: OperationDefinition,
    scannedRfid: String?,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onScan: (String, Map<String, String>) -> Unit,
    onOpenRfidScanner: (Map<String, String>) -> Unit,
    onValue: (String, String) -> Unit,
    onChecklistDone: (String) -> Unit,
    onChecklistDoneWithValues: (String, Map<String, String>) -> Unit,
    onChecklistProblem: (String, String, String) -> Unit,
    onComplete: () -> Unit,
    canEdit: Boolean,
) {
    var activeItemId by remember(task.id) { mutableStateOf<String?>(null) }
    val activeItem = task.checklist.firstOrNull { it.id == activeItemId }
    val pending = task.checklist.filter { it.status == ChecklistStatus.PENDING }
    val closed = task.checklist.filter { it.status != ChecklistStatus.PENDING }
    val doneCount = task.checklist.count { it.status == ChecklistStatus.DONE }
    val problemCount = task.checklist.count { it.status == ChecklistStatus.PROBLEM }
    val allProcessed = task.checklist.isEmpty() || pending.isEmpty()

    if (activeItem != null && !definition.requiresScan) {
        SimpleItemForm(
            task = task,
            definition = definition,
            item = activeItem,
            onCancel = { activeItemId = null },
            onSubmit = { values, problem, comment ->
                values.forEach(onValue)
                if (problem) onChecklistProblem(activeItem.id, comment, comment)
                else onChecklistDoneWithValues(activeItem.id, values)
                activeItemId = null
            },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SimpleBackground).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "← Назад",
                color = SimpleGreen,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 10.dp).clickable(onClick = onBack),
            )
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(SimpleGreen, SimpleDarkGreen))).padding(18.dp),
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    SimpleBadge(task.priority.title, if (task.priority == Priority.URGENT) SimpleRed else Color(0xFFF59E0B))
                    Text("${task.plannedStart} · ${task.plannedDurationMinutes} мин", color = Color(0xFFD6EEE2), fontSize = 13.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(task.title, color = Color.White, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(7.dp))
                Text(definition.type.title, color = Color(0xFFD6EEE2), fontSize = 16.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimpleMetric(doneCount.toString(), "выполнено", Modifier.weight(1f), true)
                    SimpleMetric(problemCount.toString(), "проблем", Modifier.weight(1f), true)
                    SimpleMetric(task.checklist.size.toString(), "всего", Modifier.weight(1f), true)
                }
                if (task.status == TaskStatus.NEW && canEdit) {
                    Spacer(Modifier.height(14.dp))
                    SimpleButton("Приступить", onBegin, Modifier.fillMaxWidth())
                }
            }
        }

        if (task.status != TaskStatus.NEW && canEdit) {
            if (definition.requiresScan) {
                item {
                    SimpleScanPanel(task, definition, scannedRfid, onOpenRfidScanner, onScan)
                }
            } else if (task.checklist.isEmpty()) {
                item {
                    SimpleStandaloneForm(task, definition, onValue, onComplete)
                }
            }
        }

        if (task.checklist.isNotEmpty()) {
            item { SimpleSectionTitle(if (definition.requiresScan) "Чек-лист закрывается сканированием" else "К исполнению") }
            if (pending.isEmpty()) item { SimpleEmpty("Все пункты обработаны") }
            pending.forEach { checklistItem ->
                item(key = checklistItem.id) {
                    SimpleChecklistCard(
                        title = checklistItem.label,
                        subtitle = if (definition.requiresScan) "Ожидает сканирования" else "Открыть короткую форму",
                        action = if (definition.requiresScan) "RFID" else "Открыть",
                        enabled = !definition.requiresScan && canEdit,
                    ) { activeItemId = checklistItem.id }
                }
            }
            item { SimpleSectionTitle("Результаты") }
            if (closed.isEmpty()) item { SimpleEmpty("Пока нет выполненных пунктов") }
            closed.forEach { checklistItem -> item(key = "closed-${checklistItem.id}") { SimpleResultCard(checklistItem, definition) } }
            item {
                SimpleButton(
                    if (task.requiresAcceptance) "Завершить и отправить на приёмку" else "Завершить задачу",
                    onComplete,
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    enabled = allProcessed && task.status != TaskStatus.SENT && canEdit,
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SimpleScanPanel(
    task: MobileTask,
    definition: OperationDefinition,
    scannedRfid: String?,
    onOpenScanner: (Map<String, String>) -> Unit,
    onScan: (String, Map<String, String>) -> Unit,
) {
    val values = remember(task.id) { mutableStateMapOf<String, String>().apply { definition.fields.filterNot(::isRfidField).forEach { put(it.id, defaultValue(it)) } } }
    var hasProblem by remember(task.id) { mutableStateOf(false) }
    var problemComment by remember(task.id) { mutableStateOf("") }
    var error by remember(task.id) { mutableStateOf("") }
    SimpleCard {
        Text("Сканирование RFID", color = SimpleText, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Сканируйте метку объекта, заполните несколько полей и сохраните результат.", color = SimpleMuted, fontSize = 14.sp)
        SimpleButton(
            if (scannedRfid.isNullOrBlank()) "Сканировать RFID" else "Сканировать другую метку",
            { onOpenScanner(values.toMap()) }, Modifier.fillMaxWidth(), secondary = true,
        )
        if (!scannedRfid.isNullOrBlank()) {
            Column(Modifier.fillMaxWidth().background(Color(0xFFEAF7F0), RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFB7DEC9), RoundedCornerShape(18.dp)).padding(14.dp)) {
                Text("Метка считана", color = SimpleGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                Text(scannedRfid, color = SimpleText, fontWeight = FontWeight.Black, fontSize = 19.sp)
            }
        }
        definition.fields.filterNot(::isRfidField).forEach { field -> SimpleField(field, values[field.id].orEmpty()) { values[field.id] = it } }
        SimpleProblemBlock(hasProblem, { hasProblem = it }, problemComment, { problemComment = it })
        if (error.isNotBlank()) Text(error, color = SimpleRed, fontWeight = FontWeight.Bold)
        SimpleButton(
            if (hasProblem) "Зафиксировать проблему" else definition.completionLabel,
            {
                val missing = definition.fields.filter { it.required && !isRfidField(it) && values[it.id].isNullOrBlank() }
                when {
                    scannedRfid.isNullOrBlank() -> error = "Сначала отсканируйте RFID"
                    hasProblem && problemComment.length < 3 -> error = "Укажите причину проблемы"
                    !hasProblem && missing.isNotEmpty() -> error = "Заполните обязательные поля"
                    else -> onScan(scannedRfid, values.toMap() + ("problem" to if (hasProblem) problemComment else ""))
                }
            }, Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SimpleItemForm(task: MobileTask, definition: OperationDefinition, item: ChecklistItem, onCancel: () -> Unit, onSubmit: (Map<String, String>, Boolean, String) -> Unit) {
    val values = remember(item.id) { mutableStateMapOf<String, String>().apply { definition.fields.forEach { put(it.id, defaultValue(it)) } } }
    var problem by remember(item.id) { mutableStateOf(false) }
    var comment by remember(item.id) { mutableStateOf("") }
    var error by remember(item.id) { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().background(Color.White).statusBarsPadding(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column(Modifier.weight(1f)) { Text("Исполнение пункта", color = SimpleMuted, fontWeight = FontWeight.Bold); Text(item.label, color = SimpleText, fontSize = 25.sp, fontWeight = FontWeight.Black) }
                Text("×", color = SimpleMuted, fontSize = 30.sp, modifier = Modifier.clickable(onClick = onCancel).padding(8.dp))
            }
        }
        item { SimpleReadonly("Объект чек-листа", item.label) }
        definition.fields.forEach { field -> item(field.id) { SimpleField(field, values[field.id].orEmpty()) { values[field.id] = it } } }
        item { SimpleProblemBlock(problem, { problem = it }, comment, { comment = it }) }
        if (error.isNotBlank()) item { Text(error, color = SimpleRed, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleButton("Отмена", onCancel, Modifier.width(112.dp), secondary = true)
                SimpleButton(definition.completionLabel, {
                    val missing = definition.fields.filter { it.required && values[it.id].isNullOrBlank() }
                    if (problem && comment.length < 3) error = "Укажите причину проблемы"
                    else if (!problem && missing.isNotEmpty()) error = "Заполните обязательные поля"
                    else onSubmit(values.toMap(), problem, comment)
                }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SimpleStandaloneForm(task: MobileTask, definition: OperationDefinition, onValue: (String, String) -> Unit, onComplete: () -> Unit) {
    val values = remember(task.id) { mutableStateMapOf<String, String>().apply { definition.fields.forEach { put(it.id, task.result.values[it.id] ?: defaultValue(it)) } } }
    SimpleCard {
        Text("Выполнение задачи", color = SimpleText, fontSize = 20.sp, fontWeight = FontWeight.Black)
        definition.fields.forEach { field -> SimpleField(field, values[field.id].orEmpty()) { values[field.id] = it; onValue(field.id, it) } }
        SimpleButton(definition.completionLabel, onComplete, Modifier.fillMaxWidth())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleField(field: OperationField, value: String, onValue: (String) -> Unit) {
    when (field.type) {
        FieldType.BOOLEAN -> Row(Modifier.fillMaxWidth().background(Color(0xFFF6F9F7), RoundedCornerShape(16.dp)).clickable { onValue((value != "true").toString()) }.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(field.title, color = SimpleText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Switch(value == "true", { onValue(it.toString()) })
        }
        FieldType.SELECT, FieldType.FEED_TYPE -> {
            var expanded by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(field.title, color = SimpleMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                    OutlinedTextField(value, {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(16.dp))
                    ExposedDropdownMenu(expanded, { expanded = false }) { field.options.forEach { option -> DropdownMenuItem({ Text(option) }, { onValue(option); expanded = false }) } }
                }
            }
        }
        FieldType.PHOTO, FieldType.VIDEO, FieldType.FILE -> SimpleButton(if (value.isBlank()) field.title else "Добавлено: $value", { onValue("Добавлено") }, Modifier.fillMaxWidth(), secondary = true)
        else -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(field.title + (field.unit?.let { ", $it" } ?: ""), color = SimpleMuted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), placeholder = { Text(field.placeholder) }, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = if (field.type == FieldType.NUMBER || field.type == FieldType.TEMPERATURE || field.type == FieldType.HOURS) KeyboardType.Decimal else KeyboardType.Text))
        }
    }
}

@Composable private fun SimpleProblemBlock(problem: Boolean, onProblem: (Boolean) -> Unit, comment: String, onComment: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFFFFF3D6), RoundedCornerShape(16.dp)).clickable { onProblem(!problem) }.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Есть проблема / отклонение", color = Color(0xFF875100), fontWeight = FontWeight.ExtraBold); Switch(problem, onProblem)
        }
        if (problem) OutlinedTextField(comment, onComment, Modifier.fillMaxWidth(), label = { Text("Причина проблемы") }, shape = RoundedCornerShape(16.dp))
    }
}

@Composable private fun SimpleChecklistCard(title: String, subtitle: String, action: String, enabled: Boolean, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(enabled, onClick = onClick), RoundedCornerShape(18.dp), CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(4.dp)) { Row(Modifier.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = SimpleText, fontWeight = FontWeight.Bold); Text(subtitle, color = SimpleMuted, fontSize = 13.sp) }; Text(action, color = SimpleGreen, fontWeight = FontWeight.ExtraBold) } } }
@Composable private fun SimpleResultCard(item: ChecklistItem, definition: OperationDefinition) { val problem = item.status == ChecklistStatus.PROBLEM; Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(4.dp)) { Row { Box(Modifier.width(4.dp).heightIn(min = 100.dp).background(if (problem) SimpleRed else SimpleGreen)); Row(Modifier.weight(1f).padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.label, color = SimpleText, fontWeight = FontWeight.Bold); Text(if (problem) item.result.problemReason ?: "Есть замечание" else "Выполнено", color = SimpleMuted); val details = item.result.values.entries.joinToString(" · ") { (key, v) -> "${definition.fields.firstOrNull { it.id == key }?.title ?: key}: ${if (v == "true") "Да" else if (v == "false") "Нет" else v}" }; if (details.isNotBlank()) Text(details, color = SimpleMuted, fontSize = 12.sp) }; SimpleBadge(if (problem) "Проблема" else "OK", if (problem) SimpleRed else SimpleGreen) } } } }
@Composable private fun SimpleReadonly(label: String, value: String) { Column(Modifier.fillMaxWidth().background(Color(0xFFF6F9F7), RoundedCornerShape(16.dp)).border(1.dp, SimpleBorder, RoundedCornerShape(16.dp)).padding(14.dp)) { Text(label, color = SimpleMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(value, color = SimpleText, fontWeight = FontWeight.Bold) } }
@Composable private fun SimpleCard(content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(5.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) } }
@Composable private fun SimpleSectionTitle(text: String) = Text(text, color = SimpleText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
@Composable private fun SimpleEmpty(text: String) { Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Text(text, color = SimpleMuted, modifier = Modifier.padding(16.dp)) } }
@Composable private fun SimpleMetric(value: String, label: String, modifier: Modifier = Modifier, dark: Boolean = false) { Column(modifier.background(if (dark) Color.White.copy(.13f) else Color.White, RoundedCornerShape(15.dp)).padding(11.dp)) { Text(value, color = if (dark) Color.White else SimpleText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Text(label, color = if (dark) Color(0xFFD9F1E5) else SimpleMuted, fontSize = 11.sp) } }
@Composable private fun SimpleBadge(text: String, color: Color) { Surface(color = color.copy(.13f), shape = RoundedCornerShape(99.dp)) { Text(text, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }
@Composable private fun SimpleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false, enabled: Boolean = true) { Button(onClick, modifier.heightIn(min = 50.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (secondary) Color(0xFFE4ECE8) else SimpleGreen, contentColor = if (secondary) SimpleDarkGreen else Color.White)) { Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) } }
private fun isRfidField(field: OperationField) = field.id.contains("rfid", true)
private fun defaultValue(field: OperationField) = when (field.type) { FieldType.BOOLEAN -> "false"; FieldType.NUMBER, FieldType.TEMPERATURE, FieldType.HOURS -> ""; FieldType.SELECT, FieldType.FEED_TYPE -> field.options.firstOrNull().orEmpty(); else -> "" }
