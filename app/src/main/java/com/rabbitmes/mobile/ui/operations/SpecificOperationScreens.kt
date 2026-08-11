package com.rabbitmes.mobile.ui.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*
import ru.profikrol.operator.domain.model.Rabbit as RabbitInfo
import ru.profikrol.operator.feature.rfidscanresult.RabbitInfoCard

private const val SHOW_BLUETOOTH_SCALE_BUTTON = false

private val inseminationProblemReasons = listOf(
    "Самка не готова к осеменению",
    "Проблема со здоровьем",
    "RFID не считывается",
    "Нет нужного материала",
    "Другая причина",
)

@Composable
fun InseminationScreen(
    task: MobileTask,
    scannedRfid: String?,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onScan: (String, Map<String, String>) -> Unit,
    onOpenRfidScanner: (Map<String, String>) -> Unit,
    onValue: (String, String) -> Unit,
    onPhoto: (String, String) -> Unit,
    onVideo: (String, String) -> Unit,
    onFile: (String, String) -> Unit,
    onComment: (String) -> Unit,
    onChecklistDone: (String) -> Unit,
    onChecklistProblem: (String, String, String) -> Unit,
    onChecklistSkip: (String, String) -> Unit,
    onComplete: () -> Unit,
    onSkip: (String) -> Unit,
    onOpenAnimal: (String) -> Unit,
    canEdit: Boolean = true,
) {
    var rfidInput by remember(task.id) { mutableStateOf("") }
    var selectedRfid by remember(task.id) { mutableStateOf<String?>(null) }
    var maleMaterialCode by remember(task.id) { mutableStateOf(task.result.values["maleMaterialCode"].orEmpty()) }
    var inseminated by remember(task.id) { mutableStateOf(false) }
    var hasProblem by remember(task.id) { mutableStateOf(false) }
    var problemReason by remember(task.id) { mutableStateOf(inseminationProblemReasons.first()) }
    var problemComment by remember(task.id) { mutableStateOf("") }

    LaunchedEffect(scannedRfid) {
        if (!scannedRfid.isNullOrBlank()) {
            val isPending = task.checklist.any {
                it.targetType == TargetType.RABBIT &&
                    it.targetId.equals(scannedRfid, ignoreCase = true) &&
                    it.status == ChecklistStatus.PENDING
            }
            if (isPending) {
                rfidInput = scannedRfid
                selectedRfid = scannedRfid
            } else if (selectedRfid == scannedRfid) {
                rfidInput = ""
                selectedRfid = null
            }
        }
    }

    val checklistItem = selectedRfid?.let { selected ->
        task.checklist.firstOrNull {
            it.targetType == TargetType.RABBIT && it.targetId.equals(selected, ignoreCase = true)
        }
    }
    val scannerValues = mapOf(
        "maleMaterialCode" to maleMaterialCode,
        "inseminated" to inseminated.toString(),
    )

    TaskExecutionScaffold(
        task = task,
        onBack = onBack,
        onBegin = onBegin,
        onComplete = onComplete,
        onSkip = onSkip,
        onChecklistDone = onChecklistDone,
        onChecklistProblem = onChecklistProblem,
        onChecklistSkip = onChecklistSkip,
        allowRootComplete = false,
        canEdit = canEdit,
        checklistAfterContent = true,
        checklistDescription = "Сканирование закрывает пункт выбранной самки.",
        afterChecklist = { ExecutionEvidencePanel(task) },
    ) {
        MesCard {
            Text("Сканирование RFID", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = rfidInput,
                onValueChange = {
                    rfidInput = it
                    selectedRfid = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("RFID самки") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap),
            ) {
                Button(
                    onClick = {
                        if (rfidInput.isNotBlank()) selectedRfid = rfidInput.trim()
                        else onOpenRfidScanner(scannerValues)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Скан") }
                OutlinedButton(
                    onClick = {
                        val serverRfid = task.checklist.firstOrNull {
                            it.targetType == TargetType.RABBIT && it.status == ChecklistStatus.PENDING
                        }?.targetId.orEmpty()
                        rfidInput = serverRfid
                        selectedRfid = serverRfid.takeIf { it.isNotBlank() }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Mock RFID") }
            }

            if (selectedRfid != null && checklistItem == null) {
                Text("Самка с таким RFID не найдена", color = MaterialTheme.colorScheme.error)
            }

            selectedRfid?.takeIf { checklistItem != null }?.let { selected ->
                RabbitInfoCard(
                    isLoading = false,
                    rabbit = RabbitInfo(
                        rfidCode = selected,
                        status = "К работе",
                        age = "—",
                        cage = "—",
                        weight = "—",
                        diagnosis = "—",
                    ),
                    onClick = { onOpenAnimal(selected) },
                )

                OutlinedTextField(
                    value = maleMaterialCode,
                    onValueChange = {
                        maleMaterialCode = it
                        onValue("maleMaterialCode", it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Код самца / материала") },
                    singleLine = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("Самка осеменена", fontWeight = FontWeight.SemiBold)
                    Checkbox(checked = inseminated, onCheckedChange = { inseminated = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("Есть проблема", fontWeight = FontWeight.SemiBold)
                    Checkbox(checked = hasProblem, onCheckedChange = { hasProblem = it })
                }

                if (hasProblem) {
                    SelectionDropdown(
                        value = problemReason,
                        onValueChange = { problemReason = it },
                        options = inseminationProblemReasons,
                        label = "Причина",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = problemComment,
                        onValueChange = { problemComment = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Комментарий") },
                    )
                    AttachmentPickerButtons(onAttachment = { type, name, uri ->
                        when (type) {
                            AttachmentType.PHOTO -> onPhoto(name, uri)
                            AttachmentType.VIDEO -> onVideo(name, uri)
                            AttachmentType.FILE -> onFile(name, uri)
                        }
                    })
                }

                Button(
                    onClick = {
                        val rfid = selectedRfid ?: return@Button
                        val values = mapOf(
                            "Код самца / материала" to maleMaterialCode.trim(),
                            "Самка осеменена" to inseminated.toString(),
                        )
                        onScan(rfid, values)
                        if (hasProblem && checklistItem != null) {
                            onChecklistProblem(checklistItem.id, problemReason, problemComment)
                            if (problemComment.isNotBlank()) onComment(problemComment)
                        }
                        rfidInput = ""
                        selectedRfid = null
                        inseminated = false
                        hasProblem = false
                        problemComment = ""
                    },
                    enabled = task.status != TaskStatus.NEW &&
                        checklistItem?.status == ChecklistStatus.PENDING &&
                        maleMaterialCode.isNotBlank() &&
                        (inseminated || hasProblem),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (hasProblem) "Сохранить результат" else "Самка осеменена")
                }
            }
        }
    }
}

@Composable
fun PalpationScreen(task: MobileTask, scannedRfid: String?, onBack: () -> Unit, onBegin: () -> Unit, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    var result by remember { mutableStateOf("Сукрольная") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard {
            Text("Результат пальпации", fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(MesSpacing.smallGap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap)) {
                    listOf("Сукрольная", "Не сукрольная").forEach { option ->
                        FilterChip(
                            selected = result == option,
                            onClick = { result = option; onValue("palpationResult", option) },
                            label = { Text(option) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                FilterChip(
                    selected = result == "Сомнительно",
                    onClick = { result = "Сомнительно"; onValue("palpationResult", "Сомнительно") },
                    label = { Text("Сомнительно") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        ScanPanel("RFID самки", "RFID", onScan = { rfid -> onScan(rfid, mapOf("palpationResult" to result)) }, onOpenScanner = { onOpenRfidScanner(mapOf("palpationResult" to result)) }, initialRfid = scannedRfid)
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun WeighingScreen(
    task: MobileTask,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onWeighingSaved: (String, Map<String, String>) -> Unit,
    onPhoto: (String, String) -> Unit,
    onVideo: (String, String) -> Unit,
    onFile: (String, String) -> Unit,
    onComment: (String) -> Unit,
    onChecklistDone: (String) -> Unit,
    onChecklistProblem: (String, String, String) -> Unit,
    onChecklistSkip: (String, String) -> Unit,
    onComplete: () -> Unit,
    onSkip: (String) -> Unit,
    canEdit: Boolean = true,
) {
    val pendingCages = task.checklist
        .filter { it.status == ChecklistStatus.PENDING }
        .mapNotNull { item -> MockRepository.cage(item.targetId)?.let { cage -> item to cage } }
    var openedItemId by remember(task.id) { mutableStateOf<String?>(null) }
    var weightGrams by remember(task.id) { mutableStateOf("") }

    LaunchedEffect(task.checklist) {
        if (pendingCages.none { (item) -> item.id == openedItemId }) {
            openedItemId = null
            weightGrams = ""
        }
    }

    TaskExecutionScaffold(
        task = task,
        onBack = onBack,
        onBegin = onBegin,
        onComplete = onComplete,
        onSkip = onSkip,
        onChecklistDone = onChecklistDone,
        onChecklistProblem = onChecklistProblem,
        onChecklistSkip = onChecklistSkip,
        allowRootComplete = false,
        canEdit = canEdit,
        checklistAfterContent = true,
        checklistDescription = "Сохраненный вес отображается в готовых пунктах.",
        afterChecklist = {
            ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
            ExecutionEvidencePanel(task)
        },
    ) {
        MesCard {
            Text("Клетки контрольной группы", fontWeight = FontWeight.Bold)
            if (pendingCages.isEmpty()) {
                Text("Все клетки взвешены", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            pendingCages.forEach { (item, cage) ->
                val isOpened = openedItemId == item.id
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MesSpacing.smallGap),
                ) {
                    Column(Modifier.padding(MesSpacing.contentGap)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.label, fontWeight = FontWeight.SemiBold)
                                Text("Клетка ${cage.code}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(
                                onClick = {
                                    openedItemId = if (isOpened) null else item.id
                                    weightGrams = ""
                                },
                                enabled = task.status != TaskStatus.NEW,
                            ) { Text(if (isOpened) "Закрыть" else "Открыть") }
                        }

                        if (isOpened) {
                            OutlinedTextField(
                                value = weightGrams,
                                onValueChange = { weightGrams = it.filter(Char::isDigit) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = MesSpacing.smallGap),
                                label = { Text("Вес, г") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                            )
                            if (SHOW_BLUETOOTH_SCALE_BUTTON) {
                                OutlinedButton(
                                    onClick = { weightGrams = (3100..3900).random().toString() },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Mock Bluetooth-весы") }
                            }
                            Button(
                                onClick = {
                                    onWeighingSaved(
                                        item.id,
                                        mapOf(
                                            "Клетка" to cage.code,
                                            "Вес, г" to weightGrams,
                                        ),
                                    )
                                    openedItemId = null
                                    weightGrams = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = MesSpacing.contentGap),
                                enabled = weightGrams.toIntOrNull()?.let { it > 0 } == true,
                            ) { Text("Сохранить вес") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CageOperationScreen(title: String, task: MobileTask, scannedRfid: String?, fieldsTitle: String, onBack: () -> Unit, onBegin: () -> Unit, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    var ok by remember { mutableStateOf(true) }
    var number by remember { mutableStateOf("0") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard { Text(fieldsTitle, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Готово / норма"); Switch(ok, { ok = it; onValue("ok", it.toString()) }) }; OutlinedTextField(number, { number = it; onValue("count", it) }, Modifier.fillMaxWidth(), label = { Text("Количество / показатель") }) }
        CageScanPanel(title, onScan = { rfid -> onScan(rfid, mapOf("ok" to ok.toString(), "count" to number)) }, onOpenScanner = { onOpenRfidScanner(mapOf("ok" to ok.toString(), "count" to number)) }, initialRfid = scannedRfid)
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun NestPreparationScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    val pendingItems = task.checklist.filter { it.status == ChecklistStatus.PENDING }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard {
            Text("Подготовка гнезд", fontWeight = FontWeight.Bold)
            Text("Отмечайте готовность по номеру клетки.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(MesSpacing.contentGap))
            if (pendingItems.isEmpty()) {
                Text("Все клетки отмечены", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                pendingItems.forEach { item ->
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(vertical = MesSpacing.smallGap)
                    ) {
                        Column(Modifier.padding(MesSpacing.contentGap)) {
                            Text(item.label, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(MesSpacing.smallGap))
                            Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { onChecklistDone(item.id) }, modifier = Modifier.weight(1f)) { Text("Готова") }
                                OutlinedButton(
                                    onClick = { onChecklistProblem(item.id, "Клетка не готова", "Гнездо не подготовлено") },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Не готова") }
                            }
                        }
                    }
                }
            }
        }
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun HangarGenericOperationScreen(task: MobileTask, definition: OperationDefinition, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text(definition.type.title, fontWeight = FontWeight.Bold); GenericFields(definition, onValue) }
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun LightAutomationTaskScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    var hours by remember { mutableStateOf("14") }
    var mode by remember { mutableStateOf("База 14:00") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text("Управление освещением", fontWeight = FontWeight.Bold); OutlinedTextField(hours, { hours = it; onValue("lightHours", it) }, Modifier.fillMaxWidth(), label = { Text("Длительность светового дня, ч") }); Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap)) { listOf("База 14:00", "Стимуляция 22:00").forEach { FilterChip(selected = mode == it, onClick = { mode = it; onValue("mode", it) }, label = { Text(it) }) } } }
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun FeedOperationScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    var feed by remember { mutableStateOf("Лактация") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text("Подача / проверка корма", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap)) { listOf("Откорм", "Отъем", "Лактация").forEach { FilterChip(selected = feed == it, onClick = { feed = it; onValue("feedType", it) }, label = { Text(it) }) } } }
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun OperationScreenFactory(task: MobileTask, definition: OperationDefinition, onBack: () -> Unit, onBegin: () -> Unit, scannedRfid: String? = null, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistDoneWithValues: (String, Map<String, String>)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onGeneralComplete: (String)->Unit, onGeneralReject: (String, String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    SimpleOperationScreen(
            task = task,
            definition = definition,
            scannedRfid = scannedRfid,
            onBack = onBack,
            onBegin = onBegin,
            onScan = onScan,
            onOpenRfidScanner = onOpenRfidScanner,
            onValue = onValue,
            onChecklistDone = onChecklistDone,
            onChecklistDoneWithValues = onChecklistDoneWithValues,
            onChecklistProblem = onChecklistProblem,
            onComplete = onComplete,
            onSkip = onSkip,
            onGeneralComplete = onGeneralComplete,
            onGeneralReject = onGeneralReject,
            onPhoto = onPhoto,
            onVideo = onVideo,
            onFile = onFile,
            onComment = onComment,
            onOpenAnimal = onOpenAnimal,
            canEdit = canEdit,
    )
    return
    @Suppress("UNREACHABLE_CODE")
    when (task.operationType) {
        OperationType.INSEMINATION -> InseminationScreen(task, scannedRfid, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, onOpenAnimal, canEdit)
        OperationType.PALPATION -> PalpationScreen(task, scannedRfid, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, onOpenAnimal, canEdit)
        OperationType.WEIGHING -> WeighingScreen(task, onBack, onBegin, onChecklistDoneWithValues, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.NEST_PREPARATION -> NestPreparationScreen(task, onBack, onBegin, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.NEST_CONTROL -> CageOperationScreen("Клетка", task, scannedRfid, "Контроль лактации", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.NEST_SELECTION -> CageOperationScreen("Клетка", task, scannedRfid, "Выравнивание / калибровка гнезда", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.OKROL -> CageOperationScreen("Клетка", task, scannedRfid, "Окрол: учет живых и мертвых", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.LACTATION_CONTROL -> CageOperationScreen("Клетка", task, scannedRfid, "Контроль лактации", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.ANIMAL_TRANSFER, OperationType.ANIMAL_SETTLEMENT, OperationType.FEMALE_DELIVERY -> CageOperationScreen("RFID объекта", task, scannedRfid, task.operationType.title, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.LIGHT_STIMULATION, OperationType.LIGHTING_CHECK -> LightAutomationTaskScreen(task, onBack, onBegin, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.FEED_CHECK, OperationType.MANUAL_FEEDING -> FeedOperationScreen(task, onBack, onBegin, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        else -> HangarGenericOperationScreen(task, definition, onBack, onBegin, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
    }
}
