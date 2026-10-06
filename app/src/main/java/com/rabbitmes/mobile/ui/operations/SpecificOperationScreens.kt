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

/** Кроличий пункт задачи по RFID: сначала среди ожидающих, потом среди уже обработанных. */
private fun MobileTask.findRabbitChecklistItem(rfid: String, resolvedRabbitId: String?, pendingOnly: Boolean): ChecklistItem? =
    checklist.firstOrNull { item ->
        item.targetType == TargetType.RABBIT &&
            (!pendingOnly || item.status == ChecklistStatus.PENDING) &&
            item.matchesRfid(rfid, resolvedRabbitId)
    }

@Composable
fun InseminationScreen(
    task: MobileTask,
    scannedRfid: String?,
    scannedValues: Map<String, String>,
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
    resolveRabbitId: (String) -> String?,
    canEdit: Boolean = true,
) {
    var rfidInput by remember(task.id) { mutableStateOf("") }
    var selectedRfid by remember(task.id) { mutableStateOf<String?>(null) }
    var submittedRfid by remember(task.id) { mutableStateOf<String?>(null) }
    var hasProblem by remember(task.id, scannedValues) {
        mutableStateOf(!scannedValues[PROBLEM_REASON_KEY].isNullOrBlank())
    }
    var problemComment by remember(task.id, scannedValues) {
        mutableStateOf(scannedValues[PROBLEM_COMMENT_KEY].orEmpty())
    }

    LaunchedEffect(scannedRfid) {
        if (!scannedRfid.isNullOrBlank()) {
            if (scannedRfid == submittedRfid) {
                rfidInput = ""
                selectedRfid = null
                return@LaunchedEffect
            }
            val rabbitId = resolveRabbitId(scannedRfid)
            val isKnown = task.findRabbitChecklistItem(scannedRfid, rabbitId, pendingOnly = false) != null
            if (isKnown) {
                rfidInput = scannedRfid
                selectedRfid = scannedRfid
            } else if (selectedRfid == scannedRfid) {
                rfidInput = ""
                selectedRfid = null
            }
        }
    }

    LaunchedEffect(task.checklist, submittedRfid) {
        if (submittedRfid != null) {
            rfidInput = ""
            selectedRfid = null
        }
    }

    val checklistItem = selectedRfid?.let { selected ->
        val rabbitId = resolveRabbitId(selected)
        task.findRabbitChecklistItem(selected, rabbitId, pendingOnly = true)
            ?: task.findRabbitChecklistItem(selected, rabbitId, pendingOnly = false)
    }
    val scannerValues = buildMap {
        if (hasProblem) {
            put(PROBLEM_REASON_KEY, problemComment)
            put(PROBLEM_COMMENT_KEY, problemComment)
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
            Button(
                onClick = {
                    if (rfidInput.isNotBlank()) selectedRfid = rfidInput.trim()
                    else onOpenRfidScanner(scannerValues)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Скан") }

            if (selectedRfid != null && checklistItem == null) {
                Text("Самка с таким RFID не найдена", color = MaterialTheme.colorScheme.error)
            }

            selectedRfid?.takeIf { checklistItem != null }?.let { selected ->
                Column(verticalArrangement = Arrangement.spacedBy(MesSpacing.contentGap)) {
                    Text("RFID найден: $selected", color = MaterialTheme.colorScheme.primary)

                    if (task.status == TaskStatus.NEW) {
                        Text(
                            "Сначала нажмите «Приступить»",
                            color = MaterialTheme.colorScheme.error,
                        )
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
                        OutlinedTextField(
                            value = problemComment,
                            onValueChange = { problemComment = it },
                            modifier = Modifier.fillMaxWidth().forceSoftwareKeyboardOnFocus(),
                            label = { Text("Опишите проблему") },
                            minLines = 3,
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
                            val values = buildMap {
                            put("inseminated", (!hasProblem).toString())
                            if (hasProblem) {
                                put(PROBLEM_REASON_KEY, problemComment.trim())
                                put(PROBLEM_COMMENT_KEY, problemComment.trim())
                                }
                            }
                            submittedRfid = rfid
                            rfidInput = ""
                            selectedRfid = null
                            hasProblem = false
                            problemComment = ""
                            onScan(rfid, values)
                        },
                        enabled = canEdit &&
                            task.status != TaskStatus.NEW &&
                            checklistItem != null &&
                            (!hasProblem || problemComment.isNotBlank()),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (hasProblem) "Самка не осеменена" else "Самка осеменена")
                    }
                }
            }
        }
    }
}

@Composable
fun PalpationScreen(
    task: MobileTask,
    scannedRfid: String?,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onScan: (String, Map<String, String>) -> Unit,
    onOpenRfidScanner: (Map<String, String>) -> Unit,
    onChecklistDone: (String) -> Unit,
    onChecklistProblem: (String, String, String) -> Unit,
    onChecklistSkip: (String, String) -> Unit,
    onComplete: () -> Unit,
    onSkip: (String) -> Unit,
    resolveRabbitId: (String) -> String?,
    canEdit: Boolean = true,
) {
    var rfidInput by remember(task.id) { mutableStateOf("") }
    var selectedRfid by remember(task.id) { mutableStateOf<String?>(null) }
    var submittedRfid by remember(task.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(scannedRfid) {
        if (!scannedRfid.isNullOrBlank()) {
            if (scannedRfid == submittedRfid) {
                rfidInput = ""
                selectedRfid = null
                return@LaunchedEffect
            }
            rfidInput = scannedRfid
            selectedRfid = scannedRfid
        }
    }

    LaunchedEffect(task.checklist, submittedRfid) {
        if (submittedRfid != null) {
            rfidInput = ""
            selectedRfid = null
        }
    }

    val checklistItem = selectedRfid?.let { selected ->
        val rabbitId = resolveRabbitId(selected)
        task.findRabbitChecklistItem(selected, rabbitId, pendingOnly = true)
            ?: task.findRabbitChecklistItem(selected, rabbitId, pendingOnly = false)
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
        checklistDescription = "Пальпация закрывает пункт выбранной самки.",
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
            Button(
                onClick = {
                    if (rfidInput.isNotBlank()) selectedRfid = rfidInput.trim()
                    else onOpenRfidScanner(emptyMap())
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Скан") }

            if (selectedRfid != null && checklistItem == null) {
                Text("Самка с таким RFID не найдена", color = MaterialTheme.colorScheme.error)
            }

            selectedRfid?.takeIf { checklistItem != null }?.let { rfid ->
                Text("RFID найден: $rfid", color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap),
                ) {
                    Button(
                        onClick = {
                            submittedRfid = rfid
                            rfidInput = ""
                            selectedRfid = null
                            onScan(rfid, mapOf("pregnant" to "true", "palpationResult" to "Сукрольная"))
                        },
                        enabled = canEdit && task.status != TaskStatus.NEW && checklistItem != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("Беременна") }
                    OutlinedButton(
                        onClick = {
                            submittedRfid = rfid
                            rfidInput = ""
                            selectedRfid = null
                            onScan(rfid, mapOf("pregnant" to "false", "palpationResult" to "Не беременна"))
                        },
                        enabled = canEdit && task.status != TaskStatus.NEW && checklistItem != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("Не беременна") }
                }
            }
        }
    }
}

/** Причина, с которой закрывается задача без целей; комментарий оператора идёт следом. */
private const val NO_TARGETS_REASON = "Нет целей для выполнения"

/**
 * Экран задачи по её операции. Тип операции определяется только по коду операции с бэка,
 * всё незнакомое открывается общим экраном.
 *
 * [hasPendingSync]: в очереди есть неотправленные действия, например старт задачи. Пока они не ушли,
 * сервер не построил цели, и пустой список целей ещё ничего не значит.
 */
@Composable
fun OperationScreenFactory(task: MobileTask, definition: OperationDefinition, onBack: () -> Unit, onBegin: () -> Unit, scannedRfid: String? = null, scannedValues: Map<String, String> = emptyMap(), onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistDoneWithValues: (String, Map<String, String>)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onMortalityRoundProblem: (String, String, String, String, String, Int?, Int?, Int?) -> Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onGeneralComplete: (String)->Unit, onGeneralReject: (String, String)->Unit, onOpenAnimal: (String)->Unit, resolveRabbitId: (String) -> String?, canEdit: Boolean = true, hasPendingSync: Boolean = false) {
    val startedWithoutTargets = task.operationType.expectsTargets &&
        task.status == TaskStatus.IN_PROGRESS &&
        task.checklist.isEmpty() &&
        !hasPendingSync
    if (startedWithoutTargets) {
        ProductionNoTargetsScreen(task, onBack, onBegin, { comment -> onGeneralReject(NO_TARGETS_REASON, comment) }, canEdit)
        return
    }

    when (task.operationType) {
        OperationType.MORTALITY_ROUND ->
            ProductionMortalityRoundScreen(task, definition, onBack, onBegin, onMortalityRoundProblem, onComplete, resolveRabbitId, canEdit)
        OperationType.FEMALE_ARRIVAL ->
            FemaleArrivalScreen(task, scannedRfid, onBack, onBegin, onScan, onOpenRfidScanner, onPhoto, onVideo, onFile, canEdit)
        OperationType.INSEMINATION ->
            InseminationScreen(task, scannedRfid, scannedValues, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, onOpenAnimal, resolveRabbitId, canEdit)
        OperationType.PALPATION ->
            PalpationScreen(task, scannedRfid, onBack, onBegin, onScan, onOpenRfidScanner, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, resolveRabbitId, canEdit)
        OperationType.NEST_EQUALIZATION ->
            NestEqualizationScreen(task, onBack, onBegin, onChecklistDoneWithValues, onComplete, canEdit)
        OperationType.ANIMAL_SETTLEMENT ->
            AnimalSettlementScreen(
                task = task,
                definition = definition,
                scannedRfid = scannedRfid,
                onBack = onBack,
                onBegin = onBegin,
                onOpenScanner = onOpenRfidScanner,
                onChecklistDoneWithValues = onChecklistDoneWithValues,
                onChecklistProblem = onChecklistProblem,
                onComplete = onComplete,
                onPhoto = onPhoto,
                onVideo = onVideo,
                onFile = onFile,
                onComment = onComment,
                onOpenAnimal = onOpenAnimal,
                canEdit = canEdit,
            )
        OperationType.WEIGHING_RABBIT ->
            ProductionRabbitWeighingScreen(task, onBack, onBegin, onChecklistDoneWithValues, onChecklistProblem, onComplete, canEdit)
        OperationType.NEST_PREPARATION,
        OperationType.SLAUGHTER_SHIPPING,
        OperationType.WEIGHING_CAGE,
        OperationType.GENERAL ->
            SimpleOperationScreen(task, definition, scannedRfid, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onChecklistDone, onChecklistDoneWithValues, onChecklistProblem, onComplete, onSkip, onGeneralComplete, onGeneralReject, onPhoto, onVideo, onFile, onComment, onOpenAnimal, canEdit)
    }
}
