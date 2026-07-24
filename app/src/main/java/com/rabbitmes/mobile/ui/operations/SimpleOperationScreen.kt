package com.rabbitmes.mobile.ui.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalDensity
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.ui.components.AttachmentPickerButtons
import ru.profikrol.operator.domain.model.Rabbit as RabbitInfo
import ru.profikrol.operator.feature.rfidscanresult.RabbitInfoCard

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
    onPhoto: (String, String) -> Unit,
    onVideo: (String, String) -> Unit,
    onFile: (String, String) -> Unit,
    onComment: (String) -> Unit,
    onOpenAnimal: (String) -> Unit,
    canEdit: Boolean,
) {
    var activeItemId by remember(task.id) { mutableStateOf<String?>(null) }
    val activeItem = task.checklist.firstOrNull { it.id == activeItemId }
    val pending = task.checklist.filter { it.status == ChecklistStatus.PENDING }
    val closed = task.checklist.filter { it.status != ChecklistStatus.PENDING }
    val doneCount = task.checklist.count { it.status == ChecklistStatus.DONE }
    val problemCount = task.checklist.count { it.status == ChecklistStatus.PROBLEM }
    val allProcessed = task.checklist.isEmpty() || pending.isEmpty()
    val listState = rememberLazyListState()
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    var previousProcessedCount by remember(task.id) { mutableIntStateOf(closed.size) }

    LaunchedEffect(closed.size) {
        if (closed.size > previousProcessedCount) listState.animateScrollToItem(0)
        previousProcessedCount = closed.size
    }

    if (activeItem != null && !definition.requiresScan) {
        SimpleItemForm(
            task = task,
            definition = definition,
            item = activeItem,
            onCancel = { activeItemId = null },
            onSubmit = { values, problem, reason, comment ->
                values.forEach(onValue)
                if (problem) {
                    onChecklistProblem(activeItem.id, reason, comment)
                    if (comment.isNotBlank()) onComment(comment)
                }
                else onChecklistDoneWithValues(activeItem.id, values)
                activeItemId = null
            },
            onPhoto = onPhoto,
            onVideo = onVideo,
            onFile = onFile,
        )
        return
    }

    LazyColumn(
        state = listState,
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
                if (largeFont) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SimplePriorityBadge(task.priority)
                        Text("${task.plannedStart} · ${task.plannedDurationMinutes} мин", color = Color(0xFFD6EEE2), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        SimplePriorityBadge(task.priority)
                        Text("${task.plannedStart} · ${task.plannedDurationMinutes} мин", color = Color(0xFFD6EEE2), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(task.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(5.dp))
                Text(
                    listOf(definition.type.title, workshopName(task), hangarName(task))
                        .filter(String::isNotBlank)
                        .joinToString(" · "),
                    color = Color(0xFFD6EEE2),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                if (largeFont) {
                    Text(
                        "$doneCount выполнено · $problemCount проблем · ${task.checklist.size} всего",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        SimpleMetric(doneCount.toString(), "выполнено", Modifier.weight(1f), true)
                        SimpleMetric(problemCount.toString(), "проблем", Modifier.weight(1f), true)
                        SimpleMetric(task.checklist.size.toString(), "всего", Modifier.weight(1f), true)
                    }
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
                    SimpleScanPanel(
                        task = task,
                        definition = definition,
                        scannedRfid = scannedRfid,
                        onOpenScanner = onOpenRfidScanner,
                        onScan = onScan,
                        onPhoto = onPhoto,
                        onVideo = onVideo,
                        onFile = onFile,
                        onComment = onComment,
                        onOpenAnimal = onOpenAnimal,
                    )
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
    onPhoto: (String, String) -> Unit,
    onVideo: (String, String) -> Unit,
    onFile: (String, String) -> Unit,
    onComment: (String) -> Unit,
    onOpenAnimal: (String) -> Unit,
) {
    val values = remember(task.id) {
        mutableStateMapOf<String, String>().apply {
            definition.fields.filterNot(::isRfidField).forEach { field ->
                put(field.id, task.result.values[field.id] ?: defaultValue(field))
            }
        }
    }
    var hasProblem by remember(task.id) {
        mutableStateOf(task.result.values[PROBLEM_REASON_KEY].orEmpty().isNotBlank())
    }
    var problemReason by remember(task.id) {
        mutableStateOf(task.result.values[PROBLEM_REASON_KEY].orEmpty())
    }
    var problemComment by remember(task.id) {
        mutableStateOf(task.result.values[PROBLEM_COMMENT_KEY].orEmpty())
    }
    var error by remember(task.id) { mutableStateOf("") }
    SimpleCard {
        Text("Сканирование RFID", color = SimpleText, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Сканируйте метку объекта, заполните несколько полей и сохраните результат.", color = SimpleMuted, fontSize = 14.sp)
        SimpleButton(
            if (scannedRfid.isNullOrBlank()) "Сканировать RFID" else "Сканировать другую метку",
            {
                onOpenScanner(
                    values.toMap() + mapOf(
                        PROBLEM_REASON_KEY to if (hasProblem) problemReason else "",
                        PROBLEM_COMMENT_KEY to if (hasProblem) problemComment else "",
                    ),
                )
            },
            Modifier.fillMaxWidth(),
            secondary = true,
        )
        if (!scannedRfid.isNullOrBlank()) {
            val scannedRabbit = MockRepository.rabbitByRfid(scannedRfid)
            if (scannedRabbit != null) {
                RabbitInfoCard(
                    isLoading = false,
                    rabbit = RabbitInfo(
                        rfidCode = scannedRabbit.rfid,
                        status = scannedRabbit.healthStatus,
                        age = "${scannedRabbit.ageDays / 30} мес",
                        cage = MockRepository.cage(scannedRabbit.cageId)?.code ?: "—",
                        weight = "%.2f кг".format(scannedRabbit.lastWeightKg),
                        diagnosis = scannedRabbit.healthStatus,
                    ),
                    onClick = { onOpenAnimal(scannedRabbit.rfid) },
                )
            } else {
                Column(Modifier.fillMaxWidth().background(Color(0xFFEAF7F0), RoundedCornerShape(18.dp)).border(1.dp, Color(0xFFB7DEC9), RoundedCornerShape(18.dp)).padding(14.dp)) {
                    Text("Метка считана", color = SimpleGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Text(scannedRfid, color = SimpleText, fontWeight = FontWeight.Black, fontSize = 19.sp)
                }
            }
        }
        definition.fields.filterNot(::isRfidField).forEach { field -> SimpleField(field, values[field.id].orEmpty()) { values[field.id] = it } }
        SimpleProblemBlock(
            problem = hasProblem,
            onProblem = {
                hasProblem = it
                if (!it) {
                    problemReason = ""
                    problemComment = ""
                }
            },
            reason = problemReason,
            onReason = { problemReason = it },
            comment = problemComment,
            onComment = { problemComment = it },
            reasons = problemReasons(definition.type),
            onPhoto = onPhoto,
            onVideo = onVideo,
            onFile = onFile,
        )
        if (error.isNotBlank()) Text(error, color = SimpleRed, fontWeight = FontWeight.Bold)
        SimpleButton(
            if (hasProblem) "Зафиксировать проблему" else definition.completionLabel,
            {
                val missing = definition.fields.filter { it.required && !isRfidField(it) && values[it.id].isNullOrBlank() }
                when {
                    hasProblem && problemReason.isBlank() -> error = "Выберите причину замечания"
                    !hasProblem && scannedRfid.isNullOrBlank() -> error = "Сначала отсканируйте RFID"
                    !hasProblem && missing.isNotEmpty() -> error = "Заполните обязательные поля"
                    else -> {
                        if (problemComment.isNotBlank()) onComment(problemComment)
                        onScan(
                            scannedRfid.orEmpty(),
                            values.toMap() + mapOf(
                                PROBLEM_REASON_KEY to if (hasProblem) problemReason else "",
                                PROBLEM_COMMENT_KEY to if (hasProblem) problemComment else "",
                            ),
                        )
                    }
                }
            }, Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SimpleItemForm(
    task: MobileTask,
    definition: OperationDefinition,
    item: ChecklistItem,
    onCancel: () -> Unit,
    onSubmit: (Map<String, String>, Boolean, String, String) -> Unit,
    onPhoto: (String, String) -> Unit,
    onVideo: (String, String) -> Unit,
    onFile: (String, String) -> Unit,
) {
    val values = remember(item.id) { mutableStateMapOf<String, String>().apply { definition.fields.forEach { put(it.id, defaultValue(it)) } } }
    var problem by remember(item.id) { mutableStateOf(false) }
    var reason by remember(item.id) { mutableStateOf("") }
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
        item {
            SimpleProblemBlock(
                problem = problem,
                onProblem = {
                    problem = it
                    if (!it) {
                        reason = ""
                        comment = ""
                    }
                },
                reason = reason,
                onReason = { reason = it },
                comment = comment,
                onComment = { comment = it },
                reasons = problemReasons(definition.type),
                onPhoto = onPhoto,
                onVideo = onVideo,
                onFile = onFile,
            )
        }
        if (error.isNotBlank()) item { Text(error, color = SimpleRed, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleButton("Отмена", onCancel, Modifier.width(112.dp), secondary = true)
                SimpleButton(definition.completionLabel, {
                    val missing = definition.fields.filter { field ->
                        field.required && values[field.id].isMissingRequiredValue()
                    }
                    if (problem && reason.isBlank()) error = "Выберите причину замечания"
                    else if (!problem && missing.isNotEmpty()) error = "Заполните обязательные поля"
                    else if (
                        !problem &&
                        definition.type == OperationType.NEST_SELECTION &&
                        values["sourceCage"] == values["destinationCage"]
                    ) error = "Клетки «откуда» и «куда» должны отличаться"
                    else if (
                        !problem &&
                        definition.type == OperationType.NEST_SELECTION &&
                        (values["movedCount"]?.toIntOrNull() ?: 0) <= 0
                    ) error = "Укажите количество крольчат больше нуля"
                    else onSubmit(values.toMap(), problem, reason, comment)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleProblemBlock(
    problem: Boolean,
    onProblem: (Boolean) -> Unit,
    reason: String,
    onReason: (String) -> Unit,
    comment: String,
    onComment: (String) -> Unit,
    reasons: List<String>,
    onPhoto: (String, String) -> Unit,
    onVideo: (String, String) -> Unit,
    onFile: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().background(Color(0xFFFFF3D6), RoundedCornerShape(16.dp)).clickable { onProblem(!problem) }.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Есть замечание", color = Color(0xFF875100), fontWeight = FontWeight.ExtraBold)
            Switch(problem, onProblem)
        }
        if (problem) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Причина замечания") },
                    placeholder = { Text("Выберите причину") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    reasons.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onReason(option)
                                expanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = comment,
                onValueChange = onComment,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Комментарий") },
                placeholder = { Text("Опишите подробности, если необходимо") },
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
            )
            AttachmentPickerButtons(
                onAttachment = { type, name, uri ->
                    when (type) {
                        AttachmentType.PHOTO -> onPhoto(name, uri)
                        AttachmentType.VIDEO -> onVideo(name, uri)
                        AttachmentType.FILE -> onFile(name, uri)
                    }
                },
            )
        }
    }
}

@Composable private fun SimpleChecklistCard(title: String, subtitle: String, action: String, enabled: Boolean, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(enabled, onClick = onClick), RoundedCornerShape(18.dp), CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(4.dp)) { Row(Modifier.padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = SimpleText, fontWeight = FontWeight.Bold); Text(subtitle, color = SimpleMuted, fontSize = 13.sp) }; Text(action, color = SimpleGreen, fontWeight = FontWeight.ExtraBold) } } }
@Composable private fun SimpleResultCard(item: ChecklistItem, definition: OperationDefinition) { val problem = item.status == ChecklistStatus.PROBLEM; Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(4.dp)) { Row { Box(Modifier.width(4.dp).heightIn(min = 100.dp).background(if (problem) SimpleRed else SimpleGreen)); Row(Modifier.weight(1f).padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.label, color = SimpleText, fontWeight = FontWeight.Bold); Text(if (problem) item.result.problemReason ?: "Есть замечание" else "Выполнено", color = SimpleMuted); val details = item.result.values.entries.joinToString(" · ") { (key, v) -> "${definition.fields.firstOrNull { it.id == key }?.title ?: key}: ${if (v == "true") "Да" else if (v == "false") "Нет" else v}" }; if (details.isNotBlank()) Text(details, color = SimpleMuted, fontSize = 12.sp) }; SimpleBadge(if (problem) "Проблема" else "OK", if (problem) SimpleRed else SimpleGreen) } } } }
@Composable private fun SimpleReadonly(label: String, value: String) { Column(Modifier.fillMaxWidth().background(Color(0xFFF6F9F7), RoundedCornerShape(16.dp)).border(1.dp, SimpleBorder, RoundedCornerShape(16.dp)).padding(14.dp)) { Text(label, color = SimpleMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(value, color = SimpleText, fontWeight = FontWeight.Bold) } }
@Composable private fun SimpleCard(content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(5.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) } }
@Composable private fun SimpleSectionTitle(text: String) = Text(text, color = SimpleText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 8.dp))
@Composable private fun SimpleEmpty(text: String) { Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Text(text, color = SimpleMuted, modifier = Modifier.padding(16.dp)) } }
@Composable private fun SimpleMetric(value: String, label: String, modifier: Modifier = Modifier, dark: Boolean = false) { Column(modifier.background(if (dark) Color.White.copy(.12f) else Color.White, RoundedCornerShape(13.dp)).padding(horizontal = 9.dp, vertical = 7.dp)) { Text(value, color = if (dark) Color.White else SimpleText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold); Text(label, color = if (dark) Color(0xFFD9F1E5) else SimpleMuted, fontSize = 10.sp) } }
@Composable
private fun SimplePriorityBadge(priority: Priority) {
    val (background, content) = when (priority) {
        Priority.URGENT -> Color(0xFFFFE4E4) to Color(0xFFB42323)
        Priority.HIGH -> Color(0xFFFFEDC2) to Color(0xFF804B00)
        Priority.NORMAL -> Color(0xFFE3F4EB) to Color(0xFF12633F)
    }
    Surface(
        color = background,
        shape = RoundedCornerShape(99.dp),
        border = BorderStroke(1.dp, content.copy(alpha = 0.28f)),
    ) {
        Text(
            text = priority.title,
            color = content,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}
@Composable
private fun SimpleBadge(text: String, color: Color) {
    Surface(color = Color.White, shape = RoundedCornerShape(99.dp), border = BorderStroke(1.dp, color.copy(alpha = .35f))) {
        Text(text, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}
@Composable private fun SimpleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false, enabled: Boolean = true) { Button(onClick, modifier.heightIn(min = 50.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (secondary) Color(0xFFE4ECE8) else SimpleGreen, contentColor = if (secondary) SimpleDarkGreen else Color.White)) { Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) } }
private fun isRfidField(field: OperationField) = field.id.contains("rfid", true)
private fun defaultValue(field: OperationField) = when (field.type) { FieldType.BOOLEAN -> "false"; FieldType.NUMBER, FieldType.TEMPERATURE, FieldType.HOURS -> ""; FieldType.SELECT, FieldType.FEED_TYPE -> field.options.firstOrNull().orEmpty(); else -> "" }
private fun String?.isMissingRequiredValue(): Boolean =
    isNullOrBlank() || startsWith("Выберите", ignoreCase = true)

private fun problemReasons(type: OperationType): List<String> = when (type) {
    OperationType.INSEMINATION -> listOf(
        "Самка не готова к осеменению",
        "Проблема со здоровьем",
        "RFID не считывается",
        "Нет нужного материала",
        "Другая причина",
    )
    OperationType.PALPATION -> listOf(
        "Невозможно определить результат",
        "Проблема со здоровьем",
        "RFID не считывается",
        "Животное отсутствует",
        "Другая причина",
    )
    OperationType.WEIGHING -> listOf(
        "Весы недоступны или неисправны",
        "Некорректные показания",
        "Объект отсутствует",
        "Другая причина",
    )
    else -> listOf(
        "Отклонение от нормы",
        "Объект отсутствует",
        "Оборудование неисправно",
        "Не хватает материалов",
        "RFID не считывается",
        "Другая причина",
    )
}

const val PROBLEM_REASON_KEY = "__problemReason"
const val PROBLEM_COMMENT_KEY = "__problemComment"

private fun workshopName(task: MobileTask): String =
    MockRepository.workshop.takeIf { it.id == task.workshopId }?.name.orEmpty()

private fun hangarName(task: MobileTask): String =
    MockRepository.workshop.hangars.firstOrNull { it.id == task.hangarId }?.name.orEmpty()
