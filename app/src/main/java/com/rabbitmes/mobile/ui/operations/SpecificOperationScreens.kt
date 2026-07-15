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
import com.rabbitmes.mobile.ui.components.MesCard

private const val SHOW_BLUETOOTH_SCALE_BUTTON = false

@Composable
fun InseminationScreen(task: MobileTask, scannedRfid: String?, onBack: () -> Unit, onBegin: () -> Unit, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    var seedBatch by remember { mutableStateOf("S-26-07") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard { Text("Осеменение", fontWeight = FontWeight.Bold); OutlinedTextField(seedBatch, { seedBatch = it; onValue("seedBatch", it) }, Modifier.fillMaxWidth(), label = { Text("Партия семени") }); Text("Сканируйте RFID самки. После подтверждения пункт чек-листа закрывается автоматически.") }
        ScanPanel("RFID самки", "RFID самки", onScan = { rfid -> onScan(rfid, mapOf("inseminated" to "true", "seedBatch" to seedBatch)) }, onOpenScanner = { onOpenRfidScanner(mapOf("inseminated" to "true", "seedBatch" to seedBatch)) }, initialRfid = scannedRfid)
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun PalpationScreen(task: MobileTask, scannedRfid: String?, onBack: () -> Unit, onBegin: () -> Unit, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    var result by remember { mutableStateOf("Сукрольная") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard { Text("Результат пальпации", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Сукрольная", "Не сукрольная", "Сомнительно").forEach { FilterChip(selected = result == it, onClick = { result = it; onValue("palpationResult", it) }, label = { Text(it) }) } } }
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
    var cageNumber by remember(task.id) { mutableStateOf(pendingCages.firstOrNull()?.second?.number?.toString().orEmpty()) }
    var weightGrams by remember(task.id) { mutableStateOf("") }

    LaunchedEffect(pendingCages.firstOrNull()?.first?.id) {
        val currentCageStillPending = pendingCages.any { (_, cage) ->
            cage.number.toString() == cageNumber.trim() || cage.code.equals(cageNumber.trim(), ignoreCase = true)
        }
        if (!currentCageStillPending) {
            cageNumber = pendingCages.firstOrNull()?.second?.number?.toString().orEmpty()
        }
    }

    val selected = pendingCages.firstOrNull { (_, cage) ->
        cage.number.toString() == cageNumber.trim() || cage.code.equals(cageNumber.trim(), ignoreCase = true)
    }
    val weightValue = weightGrams.toIntOrNull()

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
        checklistDescription = "В списке указаны клетки контрольной группы. После сохранения веса соответствующая клетка автоматически переходит в статус «Готово».",
        afterChecklist = {
            ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
            ExecutionEvidencePanel(task)
        },
    ) {
        MesCard {
            Text("Взвешивание", fontWeight = FontWeight.Bold)
            Text("Укажите номер клетки и вес контрольной группы.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = cageNumber,
                onValueChange = { cageNumber = it.filter(Char::isDigit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                label = { Text("Номер клетки") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            selected?.second?.let { cage ->
                Text("Ряд ${cage.rowNumber} · клетка ${cage.number}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (cageNumber.isNotBlank() && selected == null) {
                Text("Клетки с таким номером нет среди ожидающих взвешивания.", color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(
                value = weightGrams,
                onValueChange = { weightGrams = it.filter(Char::isDigit) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
                    val (item, cage) = selected ?: return@Button
                    onWeighingSaved(
                        item.id,
                        mapOf(
                            "Номер клетки" to cage.number.toString(),
                            "Ряд" to cage.rowNumber.toString(),
                            "Вес, г" to weightGrams,
                        ),
                    )
                    weightGrams = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                enabled = task.status != TaskStatus.NEW && selected != null && weightValue != null && weightValue > 0,
            ) { Text("Сохранить вес") }
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
fun HangarGenericOperationScreen(task: MobileTask, definition: OperationDefinition, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text(definition.type.title, fontWeight = FontWeight.Bold); Text("Заполните обязательные данные по операции."); GenericFields(definition, onValue) }
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun LightAutomationTaskScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    var hours by remember { mutableStateOf("14") }
    var mode by remember { mutableStateOf("База 14:00") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text("Управление освещением", fontWeight = FontWeight.Bold); OutlinedTextField(hours, { hours = it; onValue("lightHours", it) }, Modifier.fillMaxWidth(), label = { Text("Длительность светового дня, ч") }); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("База 14:00", "Стимуляция 22:00").forEach { FilterChip(selected = mode == it, onClick = { mode = it; onValue("mode", it) }, label = { Text(it) }) } } }
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun FeedOperationScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    var feed by remember { mutableStateOf("Лактация") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text("Подача / проверка корма", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Откорм", "Отъем", "Лактация").forEach { FilterChip(selected = feed == it, onClick = { feed = it; onValue("feedType", it) }, label = { Text(it) }) } }; Text("Уставка применяется к ангару задачи, не к справочнику оборудования.") }
        ProblemAndMediaControls(onPhoto, onVideo, onFile, onComment)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun OperationScreenFactory(task: MobileTask, definition: OperationDefinition, onBack: () -> Unit, onBegin: () -> Unit, scannedRfid: String? = null, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String,String)->Unit, onVideo: (String,String)->Unit, onFile: (String,String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistDoneWithValues: (String, Map<String, String>)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    when (task.operationType) {
        OperationType.INSEMINATION -> InseminationScreen(task, scannedRfid, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, onOpenAnimal, canEdit)
        OperationType.PALPATION -> PalpationScreen(task, scannedRfid, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, onOpenAnimal, canEdit)
        OperationType.WEIGHING -> WeighingScreen(task, onBack, onBegin, onChecklistDoneWithValues, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.NEST_PREPARATION -> CageOperationScreen("RFID клетки", task, scannedRfid, "Подготовка гнезда", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.NEST_CONTROL -> CageOperationScreen("RFID клетки", task, scannedRfid, "Контроль гнезда: сытые/голодные/мертвые", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.NEST_SELECTION -> CageOperationScreen("RFID клетки", task, scannedRfid, "Выравнивание / калибровка гнезда", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.OKROL -> CageOperationScreen("RFID клетки", task, scannedRfid, "Окрол: учет живых и мертвых", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.LACTATION_CONTROL -> CageOperationScreen("RFID клетки", task, scannedRfid, "Контроль лактации", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.ANIMAL_TRANSFER, OperationType.ANIMAL_SETTLEMENT, OperationType.FEMALE_DELIVERY -> CageOperationScreen("RFID объекта", task, scannedRfid, task.operationType.title, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.LIGHT_STIMULATION, OperationType.LIGHTING_CHECK -> LightAutomationTaskScreen(task, onBack, onBegin, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.FEED_CHECK, OperationType.MANUAL_FEEDING -> FeedOperationScreen(task, onBack, onBegin, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        else -> HangarGenericOperationScreen(task, definition, onBack, onBegin, onValue, onPhoto, onVideo, onFile, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
    }
}
