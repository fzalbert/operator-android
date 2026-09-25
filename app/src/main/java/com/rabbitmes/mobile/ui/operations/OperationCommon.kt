package com.rabbitmes.mobile.ui.operations

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val taskSkipReasons = listOf(
    "Нет доступа к объекту",
    "Неисправно оборудование",
    "Недостаточно материалов",
    "Не хватает времени смены",
    "Другая причина",
)

private val UnifiedHeaderGreen = Color(0xFF1F8A5B)
private val UnifiedHeaderDarkGreen = Color(0xFF0B2F24)
private val UnifiedHeaderMuted = Color(0xFFD6EEE2)

internal fun normalizeWholeNumberInput(value: String, maxLength: Int = Int.MAX_VALUE): String {
    val digits = value.filter(Char::isDigit).take(maxLength)
    if (digits.isEmpty()) return ""
    return digits.trimStart('0').ifEmpty { "0" }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Modifier.forceSoftwareKeyboardOnFocus(): Modifier {
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    fun showKeyboard() {
        scope.launch {
            listOf(50L, 150L, 300L, 600L).forEach { delayMs ->
                delay(delayMs)
                keyboardController?.show()
                val inputMethodManager = view.context
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val focusedView = view.findFocus() ?: view
                focusedView.post {
                    inputMethodManager.restartInput(focusedView)
                    inputMethodManager.showSoftInput(focusedView, InputMethodManager.SHOW_FORCED)
                }
            }
        }
    }

    return onFocusChanged { state ->
        if (state.isFocused) showKeyboard()
    }.pointerInteropFilter { event ->
        if (event.action == MotionEvent.ACTION_UP) showKeyboard()
        false
    }
}

@Composable
fun UnifiedTaskHeader(
    task: MobileTask,
    onBack: () -> Unit,
    onBegin: (() -> Unit)? = null,
    canEdit: Boolean = true,
    title: String = task.title,
) {
    val processedCount = task.checklist.count { it.status != ChecklistStatus.PENDING }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "← Назад",
            color = UnifiedHeaderGreen,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            modifier = Modifier.padding(vertical = 10.dp).clickable(onClick = onBack),
        )
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(UnifiedHeaderGreen, UnifiedHeaderDarkGreen)))
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                UnifiedPriorityBadge(task.priority)
                Text(
                    "${task.plannedStart} · ${task.plannedDurationMinutes} мин",
                    color = UnifiedHeaderMuted,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                title.ifBlank { task.operationTypeTitle },
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Обработано $processedCount из ${task.checklist.size}",
                color = UnifiedHeaderMuted,
            )
            if (task.status == TaskStatus.NEW && canEdit && onBegin != null) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onBegin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UnifiedHeaderGreen),
                ) {
                    Text("Приступить", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun UnifiedPriorityBadge(priority: Priority) {
    val (background, content) = when (priority) {
        Priority.URGENT -> Color(0xFFFFE4E4) to Color(0xFFB42323)
        Priority.HIGH -> Color(0xFFFFEDC2) to Color(0xFF804B00)
        Priority.NORMAL -> Color(0xFFE3F4EB) to Color(0xFF12633F)
    }
    Surface(
        color = background,
        shape = RoundedCornerShape(99.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, content.copy(alpha = 0.28f)),
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
fun UnifiedReadOnlyNotice() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "Задача доступна только для просмотра",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
        )
    }
}

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
    var hasTaskIssue by remember(task.id) { mutableStateOf(false) }

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
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(checked = hasTaskIssue, onCheckedChange = { hasTaskIssue = it })
            Text("Есть замечания", fontWeight = FontWeight.SemiBold)
        }
        if (hasTaskIssue) {
            SelectionDropdown(
                value = skipReason,
                onValueChange = { skipReason = it },
                options = taskSkipReasons,
                label = "Причина, если невозможно выполнить задачу",
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { onSkip(skipReason) }, Modifier.fillMaxWidth()) { Text("Невозможно выполнить задачу") }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = 18.dp,
            top = 0.dp,
            end = 18.dp,
            bottom = MesSpacing.screenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { UnifiedTaskHeader(task, onBack, onBegin, canEdit) }
        if (!canEdit) {
            item { UnifiedReadOnlyNotice() }
        }
        if (!checklistAfterContent && task.checklist.isNotEmpty()) {
            item { checklist() }
        }
        if (canEdit) {
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
        Button(
            onClick = { onOpenScanner?.invoke() ?: run { scannedRfid = rfid } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.QrCodeScanner, null)
            Spacer(Modifier.width(MesSpacing.smallGap))
            Text("Скан")
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
            Button(
                onClick = {
                    val submittedRfid = scannedRfid ?: return@Button
                    rfid = ""
                    scannedRfid = null
                    onScan(submittedRfid)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Выполнено") }
        }
    }
}

@Composable
fun CageScanPanel(
    title: String,
    onScan: (String) -> Unit,
    onOpenScanner: (() -> Unit)? = null,
    initialRfid: String? = null,
    showSelectionButtons: Boolean = true,
    instruction: String = "Укажите клетку.",
) {
    var rfid by remember { mutableStateOf("") }
    var scannedRfid by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(initialRfid) {
        if (!initialRfid.isNullOrBlank()) {
            rfid = initialRfid
            scannedRfid = initialRfid
        }
    }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    MesCard {
        Text(title, fontWeight = FontWeight.Bold)
        Text(instruction, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = rfid,
            onValueChange = { raw ->
                val cleaned = raw.filterNot { it == '\n' || it == '\r' }
                rfid = cleaned
                scannedRfid = if (raw.any { it == '\n' || it == '\r' } && cleaned.isNotBlank()) cleaned.trim() else null
            },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            label = { Text("RFID") },
            placeholder = { Text("Ожидание сканирования…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (rfid.isNotBlank()) scannedRfid = rfid.trim() }),
        )
        if (showSelectionButtons) {
            Button(
                onClick = { onOpenScanner?.invoke() ?: run { scannedRfid = rfid } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Выбрать клетку") }
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
            Button(onClick = {
                val submittedRfid = scannedRfid ?: return@Button
                Log.d("RabbitSettlement", "RFID confirmed in settlement UI. rfid=$submittedRfid")
                rfid = ""
                scannedRfid = null
                onScan(submittedRfid)
            }, Modifier.fillMaxWidth()) { Text("Выполнено") }
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
    allowIssues: Boolean = true,
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
            var comment by remember(item.id) { mutableStateOf("") }
            Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().padding(vertical = MesSpacing.smallGap)) {
                Column(Modifier.padding(MesSpacing.contentGap)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(item.label, fontWeight = FontWeight.SemiBold)
                            item.secondaryTargetLabel().takeIf { it.isNotBlank() }?.let {
                                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (item.result.scannedRfid != null) Text("RFID: ${item.result.scannedRfid}", color = mobileSuccessGreen)
                            if (item.result.values.isNotEmpty()) Text(item.result.values.entries.joinToString { "${it.key}: ${it.value}" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.result.problemReason != null) Text(item.result.problemReason, color = MaterialTheme.colorScheme.error)
                        }
                        StatusBadge(item.status.title, when(item.status){ ChecklistStatus.DONE -> mobileSuccessGreen; ChecklistStatus.PROBLEM -> MaterialTheme.colorScheme.error; ChecklistStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant; ChecklistStatus.PENDING -> MaterialTheme.colorScheme.primary })
                    }
                    if (canEdit) {
                        Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap), modifier = Modifier.fillMaxWidth()) {
                            if (item.status == ChecklistStatus.PENDING && item.targetType != TargetType.RABBIT) {
                                Button(onClick = { onDone(item.id) }, modifier = Modifier.weight(1f)) { Text("Выполнено") }
                            }
                            if (allowIssues) {
                                OutlinedButton(onClick = { openedItemId = if (openedItemId == item.id) null else item.id }, modifier = Modifier.weight(1f)) { Text("Детали / проблема") }
                            }
                        }
                    }
                    if (canEdit && openedItemId == item.id) {
                        Spacer(Modifier.height(MesSpacing.smallGap))
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            modifier = Modifier.fillMaxWidth().forceSoftwareKeyboardOnFocus(),
                            label = { Text("Опишите проблему или причину пропуска") },
                            minLines = 3,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onProblem(item.id, comment.trim(), comment.trim()) },
                                enabled = comment.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            ) { Text("Проблема") }
                            OutlinedButton(
                                onClick = { onSkip(item.id, comment.trim()) },
                                enabled = comment.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            ) { Text("Пропустить") }
                        }
                    }
                }
            }
        }
    }
}

private fun ChecklistItem.secondaryTargetLabel(): String {
    if (serverType == "production-checklist") return ""
    return when (targetType) {
        TargetType.CAGE -> "ID клетки: $targetId"
        TargetType.RABBIT -> "ID кролика: $targetId"
        TargetType.ROW -> "ID ряда: $targetId"
        TargetType.HANGAR -> "ID ангара: $targetId"
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
            OutlinedTextField(comment, { comment = it; onComment(it) }, Modifier.fillMaxWidth().forceSoftwareKeyboardOnFocus(), label = { Text("Комментарий исполнителя") })
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
