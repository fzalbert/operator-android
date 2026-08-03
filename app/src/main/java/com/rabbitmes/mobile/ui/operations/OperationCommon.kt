package com.rabbitmes.mobile.ui.operations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen

private val taskSkipReasons = listOf(
    "Нет доступа к объекту",
    "Неисправно оборудование",
    "Недостаточно материалов",
    "Не хватает времени смены",
    "Другая причина",
)

private val checklistIssueReasons = listOf(
    "RFID не считывается",
    "Животное отсутствует",
    "Клетка или объект недоступны",
    "Операцию невозможно выполнить",
    "Другая причина",
)

@Composable
fun TaskExecutionScaffold(
    task: MobileTask,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onComplete: () -> Unit,
    onSkip: (String) -> Unit,
    onChecklistDone: (String) -> Unit,
    onChecklistProblem: (String, String, String) -> Unit,
    onChecklistSkip: (String, String) -> Unit,
    allowRootComplete: Boolean = true,
    canEdit: Boolean = true,
    checklistAfterContent: Boolean = false,
    checklistDescription: String? = null,
    afterChecklist: @Composable ColumnScope.() -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit
) {
    var skipReason by remember { mutableStateOf(taskSkipReasons.first()) }

    val checklist: @Composable () -> Unit = {
        if (task.checklist.isNotEmpty()) {
            ChecklistExecutionBlock(
                items = task.checklist,
                onDone = onChecklistDone,
                onProblem = onChecklistProblem,
                onSkip = onChecklistSkip,
                description = checklistDescription,
                canEdit = canEdit,
            )
        }
    }

    val readonlyNotice: @Composable ColumnScope.() -> Unit = {
        if (!canEdit) {
            StatusBadge("Только просмотр", MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(MesSpacing.contentGap))
            Text(
                "Эту задачу можно посмотреть, но взять в работу получится только после завершения предыдущей задачи.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    val startControl: @Composable ColumnScope.() -> Unit = {
        if (task.status == TaskStatus.NEW) {
            Button(onClick = onBegin, Modifier.fillMaxWidth()) { Text("Приступить") }
        }
    }

    val completionControls: @Composable ColumnScope.() -> Unit = {
        val pendingItems = task.checklist.count { it.status == ChecklistStatus.PENDING }
        if (allowRootComplete) {
            Button(onClick = onComplete, Modifier.fillMaxWidth()) {
                Text(if (task.requiresAcceptance) "Отправить на приемку" else "Отправить результат")
            }
        } else {
            Button(onClick = onComplete, Modifier.fillMaxWidth(), enabled = pendingItems == 0) {
                Text(
                    when {
                        pendingItems > 0 -> "Осталось обработать: $pendingItems"
                        task.checklist.isEmpty() -> "Отправить результат"
                        else -> "Отправить обработанный чек-лист"
                    }
                )
            }
        }
        Spacer(Modifier.height(MesSpacing.contentGap))
        SelectionDropdown(
            value = skipReason,
            onValueChange = { skipReason = it },
            options = taskSkipReasons,
            label = "Причина, если невозможно выполнить всю задачу",
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = { onSkip(skipReason) }, Modifier.fillMaxWidth()) { Text("Невозможно выполнить всю задачу") }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = MesSpacing.screenBottom)) {
        item { AppHeader(task.title, "${task.plannedStart} · ${task.operationType.title}", onBack) }
        item {
            MesCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    TaskStatusBadge(task.status)
                    StatusBadge("${task.plannedDurationMinutes} мин", operationAccent(task.operationType))
                    PriorityBadge(task.priority)
                }
                if (task.requiresAcceptance) {
                    Spacer(Modifier.height(MesSpacing.smallGap))
                    StatusBadge("Приемка", MaterialTheme.colorScheme.tertiary)
                }
                if (task.checklist.isNotEmpty()) {
                    Spacer(Modifier.height(MesSpacing.contentGap))
                    ProgressLine(task.checklist.count { it.status != ChecklistStatus.PENDING }, task.checklist.size)
                }
            }
        }
        if (!canEdit) {
            item { MesCard { readonlyNotice() } }
        }
        if (!checklistAfterContent && task.checklist.isNotEmpty()) {
            item { checklist() }
        }
        if (canEdit) {
            if (task.status == TaskStatus.NEW) {
                item { MesCard { startControl() } }
            }
            item { Column { bottom() } }
            if (!checklistAfterContent) {
                item { Column { afterChecklist() } }
                item { MesCard { completionControls() } }
            }
        }
        if (checklistAfterContent) {
            if (task.checklist.isNotEmpty()) {
                item { checklist() }
            }
            if (canEdit) {
                item { Column { afterChecklist() } }
                item { MesCard { completionControls() } }
            }
        }
    }
}

@Composable
fun ScanPanel(
    title: String,
    placeholder: String,
    onScan: (String) -> Unit,
    onOpenAnimal: ((String) -> Unit)? = null,
    onOpenScanner: (() -> Unit)? = null,
    initialRfid: String? = null,
) {
    var rfid by remember { mutableStateOf("") }
    var scannedRfid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialRfid) {
        if (!initialRfid.isNullOrBlank()) {
            rfid = initialRfid
            scannedRfid = initialRfid
        }
    }
    MesCard {
        Text(title, fontWeight = FontWeight.Bold)
        Text("Отсканируйте RFID.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(rfid, { rfid = it; scannedRfid = null }, Modifier.fillMaxWidth(), label = { Text(placeholder) })
        Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onOpenScanner?.invoke() ?: run { scannedRfid = rfid } }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.QrCodeScanner, null); Spacer(Modifier.width(MesSpacing.smallGap)); Text("Скан") }
            OutlinedButton(
                onClick = {
                    val mockRfid = MockRepository.rabbits.first().rfid
                    rfid = mockRfid
                    scannedRfid = mockRfid
                },
                modifier = Modifier.weight(1f)
            ) { Text("Mock RFID") }
        }
        val rabbit = scannedRfid?.let { MockRepository.rabbitByRfid(it) }
        if (rabbit != null) {
            Spacer(Modifier.height(MesSpacing.contentGap))
            RabbitMiniCard(rabbit, onOpenAnimal)
        } else if (scannedRfid != null) {
            Spacer(Modifier.height(MesSpacing.smallGap))
            Text("RFID отсканирован: $scannedRfid", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (scannedRfid != null) {
            Spacer(Modifier.height(MesSpacing.contentGap))
            Button(onClick = { onScan(scannedRfid!!) }, Modifier.fillMaxWidth()) { Text("Выполнено") }
        }
    }
}

@Composable
fun CageScanPanel(
    title: String,
    onScan: (String) -> Unit,
    onOpenScanner: (() -> Unit)? = null,
    initialRfid: String? = null,
) {
    var rfid by remember { mutableStateOf("") }
    var scannedRfid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialRfid) {
        if (!initialRfid.isNullOrBlank()) {
            rfid = initialRfid
            scannedRfid = initialRfid
        }
    }

    MesCard {
        Text(title, fontWeight = FontWeight.Bold)
        Text("Отсканируйте RFID клетки.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(rfid, { rfid = it; scannedRfid = null }, Modifier.fillMaxWidth(), label = { Text("RFID клетки") })
        Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onOpenScanner?.invoke() ?: run { scannedRfid = rfid } }, Modifier.weight(1f)) { Text("Скан клетки") }
            OutlinedButton(
                onClick = {
                    val mockRfid = MockRepository.allCages.first().rfid
                    rfid = mockRfid
                    scannedRfid = mockRfid
                },
                Modifier.weight(1f)
            ) { Text("Mock") }
        }
        val cage = scannedRfid?.let { MockRepository.cageByRfid(it) }
        if (cage != null) {
            Spacer(Modifier.height  (MesSpacing.contentGap))
            Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(MesSpacing.contentGap)) {
                    Text("${cage.code} · ${cage.rfid}", fontWeight = FontWeight.Bold)
                    Text("Ряд ${cage.rowNumber}, клетка ${cage.number}")
                }
            }
        } else if (scannedRfid != null) {
            Spacer(Modifier.height(MesSpacing.smallGap))
            Text("RFID отсканирован: $scannedRfid", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (scannedRfid != null) {
            Spacer(Modifier.height(MesSpacing.contentGap))
            Button(onClick = { onScan(scannedRfid!!) }, Modifier.fillMaxWidth()) { Text("Выполнено") }
        }
    }
}

@Composable
fun RabbitMiniCard(rabbit: Rabbit, onOpenAnimal: ((String) -> Unit)?) {
    Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(MesSpacing.contentGap)) {
            Text("${rabbit.earNumber} · ${rabbit.rfid}", fontWeight = FontWeight.Bold)
            Text("Возраст ${rabbit.ageDays} дней · вес ${"%.2f".format(rabbit.lastWeightKg)} кг")
            Text("Статус: ${rabbit.healthStatus}")
            if (onOpenAnimal != null) TextButton(onClick = { onOpenAnimal(rabbit.id) }) { Text("История животного") }
        }
    }
}

@Composable
fun ChecklistExecutionBlock(
    items: List<ChecklistItem>,
    onDone: (String) -> Unit,
    onProblem: (String, String, String) -> Unit,
    onSkip: (String, String) -> Unit,
    description: String? = null,
    canEdit: Boolean = true,
) {
    var openedItemId by remember { mutableStateOf<String?>(null) }
    var showCompleted by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    val pendingItems = items.filter { it.status == ChecklistStatus.PENDING }
    val completedItems = items.filter { it.status != ChecklistStatus.PENDING }
    val visibleItems = if (showCompleted) completedItems else pendingItems
    MesCard {
        Text("Рабочий чек-лист", fontWeight = FontWeight.Bold)
        Text(
            description ?: "Сканирование или ручная отметка.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MesSpacing.smallGap))
        OutlinedButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isExpanded) "Скрыть список" else "Показать список: ожидает ${pendingItems.size}, готово ${completedItems.size}")
        }
        if (isExpanded) {
            Spacer(Modifier.height(MesSpacing.smallGap))
            Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = !showCompleted,
                    onClick = { showCompleted = false },
                    label = { Text("Ожидает (${pendingItems.size})") }
                )
                FilterChip(
                    selected = showCompleted,
                    onClick = { showCompleted = true },
                    label = { Text("Готово (${completedItems.size})") }
                )
            }
            Spacer(Modifier.height(MesSpacing.smallGap))
            if (visibleItems.isEmpty()) {
                Text("Нет пунктов в этом разделе", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isExpanded) visibleItems.forEach { item ->
            var reason by remember(item.id) { mutableStateOf(checklistIssueReasons.first()) }
            var comment by remember(item.id) { mutableStateOf("") }
            Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(vertical = MesSpacing.smallGap)) {
                Column(Modifier.padding(MesSpacing.contentGap)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.label, fontWeight = FontWeight.SemiBold)
                            Text("Объект: ${item.targetType.name} · ${item.targetId}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.result.scannedRfid != null) Text("RFID: ${item.result.scannedRfid}", color = mobileSuccessGreen)
                            if (item.result.values.isNotEmpty()) Text(item.result.values.entries.joinToString { "${it.key}: ${it.value}" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.result.problemReason != null) Text(item.result.problemReason, color = MaterialTheme.colorScheme.error)
                        }
                        StatusBadge(item.status.title, when(item.status){ ChecklistStatus.DONE -> mobileSuccessGreen; ChecklistStatus.PROBLEM -> MaterialTheme.colorScheme.error; ChecklistStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant; ChecklistStatus.PENDING -> MaterialTheme.colorScheme.primary })
                    }
                    if (canEdit) {
                        Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { openedItemId = if (openedItemId == item.id) null else item.id }, modifier = Modifier.weight(1f)) { Text("Детали / проблема") }
                        }
                    }
                    if (canEdit && openedItemId == item.id) {
                        Spacer(Modifier.height(MesSpacing.smallGap))
                        SelectionDropdown(
                            value = reason,
                            onValueChange = { reason = it },
                            options = checklistIssueReasons,
                            label = "Причина проблемы/пропуска",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(comment, { comment = it }, Modifier.fillMaxWidth(), label = { Text("Комментарий по объекту") })
                        Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { onProblem(item.id, reason, comment) }, modifier = Modifier.weight(1f)) { Text("Проблема") }
                            OutlinedButton(onClick = { onSkip(item.id, reason) }, modifier = Modifier.weight(1f)) { Text("Пропустить") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProblemAndMediaControls(
    onPhoto: (String, String) -> Unit,
    onVideo: (String, String) -> Unit,
    onFile: (String, String) -> Unit,
    onComment: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    var hasRemarks by remember { mutableStateOf(false) }
    MesCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Checkbox(checked = hasRemarks, onCheckedChange = { hasRemarks = it })
            Text("Есть замечания", fontWeight = FontWeight.SemiBold)
        }
        if (hasRemarks) {
            Spacer(Modifier.height(MesSpacing.smallGap))
            OutlinedTextField(comment, { comment = it; onComment(it) }, Modifier.fillMaxWidth(), label = { Text("Комментарий исполнителя") })
            Spacer(Modifier.height(MesSpacing.contentGap))
            AttachmentPickerButtons(onAttachment = { type, name, uri ->
                when (type) {
                    AttachmentType.PHOTO -> onPhoto(name, uri)
                    AttachmentType.VIDEO -> onVideo(name, uri)
                    AttachmentType.FILE -> onFile(name, uri)
                }
            })
        }
    }
}

@Composable
fun ExecutionEvidencePanel(task: MobileTask) {
    val attachments = task.result.attachments
    if (attachments.isNotEmpty() || task.result.comment.isNotBlank()) {
        MesCard {
            Text("Доказательства выполнения", fontWeight = FontWeight.Bold)
            if (task.result.comment.isNotBlank()) Text(task.result.comment)
            attachments.forEach { attachment ->
                Row(Modifier.fillMaxWidth().padding(vertical = MesSpacing.tinyGap), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${attachment.type.emoji} ${attachment.name}")
                    Text(attachment.createdAt, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun GenericFields(definition: OperationDefinition, onValue: (String, String) -> Unit) {
    definition.fields.filterNot { it.id.lowercase().contains("rfid") }.forEach { field ->
        var value by remember(field.id) { mutableStateOf(if (field.options.isNotEmpty()) field.options.first() else "") }
        when(field.type) {
            FieldType.BOOLEAN -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(field.title); Switch(checked = value == "true", onCheckedChange = { value = it.toString(); onValue(field.id, value) }) }
            FieldType.SELECT, FieldType.FEED_TYPE -> Column { Text(field.title); field.options.forEach { opt -> FilterChip(selected = value == opt, onClick = { value = opt; onValue(field.id, opt) }, label = { Text(opt) }, modifier = Modifier.padding(end = 6.dp)) } }
            else -> OutlinedTextField(value, { value = it; onValue(field.id, it) }, Modifier.fillMaxWidth(), label = { Text(field.title + (field.unit?.let { u -> ", $u" } ?: "")) })
        }
    }
}
