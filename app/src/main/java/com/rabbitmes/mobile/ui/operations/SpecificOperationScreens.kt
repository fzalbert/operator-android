package com.rabbitmes.mobile.ui.operations

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.data.MockRepository
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.MesCard

@Composable
fun InseminationScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String)->Unit, onVideo: (String)->Unit, onVoice: (String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    var seedBatch by remember { mutableStateOf("S-26-07") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard { Text("Осеменение", fontWeight = FontWeight.Bold); OutlinedTextField(seedBatch, { seedBatch = it; onValue("seedBatch", it) }, Modifier.fillMaxWidth(), label = { Text("Партия семени") }); Text("Сканируйте RFID самки. После подтверждения пункт чек-листа закрывается автоматически.") }
        ScanPanel("RFID самки", "RFID самки", onScan = { rfid -> onScan(rfid, mapOf("inseminated" to "true", "seedBatch" to seedBatch)) }, onOpenScanner = { onOpenRfidScanner(mapOf("inseminated" to "true", "seedBatch" to seedBatch)) }, initialRfid = task.result.scannedRfid)
        ProblemAndMediaControls(onPhoto, onVideo, onVoice, onComment, task.result.attachments)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun PalpationScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String)->Unit, onVideo: (String)->Unit, onVoice: (String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    var result by remember { mutableStateOf("Сукрольная") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard { Text("Результат пальпации", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Сукрольная", "Не сукрольная", "Сомнительно").forEach { FilterChip(selected = result == it, onClick = { result = it; onValue("palpationResult", it) }, label = { Text(it) }) } } }
        ScanPanel("RFID самки", "RFID", onScan = { rfid -> onScan(rfid, mapOf("palpationResult" to result)) }, onOpenScanner = { onOpenRfidScanner(mapOf("palpationResult" to result)) }, initialRfid = task.result.scannedRfid)
        ProblemAndMediaControls(onPhoto, onVideo, onVoice, onComment, task.result.attachments)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun WeighingScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String)->Unit, onVideo: (String)->Unit, onVoice: (String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    var weight by remember { mutableStateOf("3.45") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard { Text("Взвешивание", fontWeight = FontWeight.Bold); OutlinedTextField(weight, { weight = it; onValue("weight", it) }, Modifier.fillMaxWidth(), label = { Text("Вес, кг") }); OutlinedButton(onClick = { weight = "3.${(10..90).random()}"; onValue("weight", weight) }, Modifier.fillMaxWidth()) { Text("Mock Bluetooth-весы") } }
        ScanPanel("RFID кролика", "RFID", onScan = { rfid -> onScan(rfid, mapOf("weight" to weight)) }, onOpenScanner = { onOpenRfidScanner(mapOf("weight" to weight)) }, initialRfid = task.result.scannedRfid)
        ProblemAndMediaControls(onPhoto, onVideo, onVoice, onComment, task.result.attachments)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun CageOperationScreen(title: String, task: MobileTask, fieldsTitle: String, onBack: () -> Unit, onBegin: () -> Unit, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String)->Unit, onVideo: (String)->Unit, onVoice: (String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    var ok by remember { mutableStateOf(true) }
    var number by remember { mutableStateOf("0") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, allowRootComplete = false, canEdit = canEdit) {
        MesCard { Text(fieldsTitle, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Готово / норма"); Switch(ok, { ok = it; onValue("ok", it.toString()) }) }; OutlinedTextField(number, { number = it; onValue("count", it) }, Modifier.fillMaxWidth(), label = { Text("Количество / показатель") }) }
        CageScanPanel(title, onScan = { rfid -> onScan(rfid, mapOf("ok" to ok.toString(), "count" to number)) }, onOpenScanner = { onOpenRfidScanner(mapOf("ok" to ok.toString(), "count" to number)) }, initialRfid = task.result.scannedRfid)
        ProblemAndMediaControls(onPhoto, onVideo, onVoice, onComment, task.result.attachments)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun HangarGenericOperationScreen(task: MobileTask, definition: OperationDefinition, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String)->Unit, onVideo: (String)->Unit, onVoice: (String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text(definition.type.title, fontWeight = FontWeight.Bold); Text("Заполните обязательные данные по операции."); GenericFields(definition, onValue) }
        ProblemAndMediaControls(onPhoto, onVideo, onVoice, onComment, task.result.attachments)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun LightAutomationTaskScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String)->Unit, onVideo: (String)->Unit, onVoice: (String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    var hours by remember { mutableStateOf("14") }
    var mode by remember { mutableStateOf("База 14:00") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text("Управление освещением", fontWeight = FontWeight.Bold); OutlinedTextField(hours, { hours = it; onValue("lightHours", it) }, Modifier.fillMaxWidth(), label = { Text("Длительность светового дня, ч") }); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("База 14:00", "Стимуляция 22:00").forEach { FilterChip(selected = mode == it, onClick = { mode = it; onValue("mode", it) }, label = { Text(it) }) } } }
        ProblemAndMediaControls(onPhoto, onVideo, onVoice, onComment, task.result.attachments)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun FeedOperationScreen(task: MobileTask, onBack: () -> Unit, onBegin: () -> Unit, onValue: (String,String) -> Unit, onPhoto: (String)->Unit, onVideo: (String)->Unit, onVoice: (String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, canEdit: Boolean = true) {
    var feed by remember { mutableStateOf("Лактация") }
    TaskExecutionScaffold(task, onBack, onBegin, onComplete, onSkip, onChecklistDone, onChecklistProblem, onChecklistSkip, canEdit = canEdit) {
        MesCard { Text("Подача / проверка корма", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Откорм", "Отъем", "Лактация").forEach { FilterChip(selected = feed == it, onClick = { feed = it; onValue("feedType", it) }, label = { Text(it) }) } }; Text("Уставка применяется к ангару задачи, не к справочнику оборудования.") }
        ProblemAndMediaControls(onPhoto, onVideo, onVoice, onComment, task.result.attachments)
        ExecutionEvidencePanel(task)
    }
}

@Composable
fun OperationScreenFactory(task: MobileTask, definition: OperationDefinition, onBack: () -> Unit, onBegin: () -> Unit,scannedRfid: String? = null, onScan: (String, Map<String,String>) -> Unit, onOpenRfidScanner: (Map<String, String>) -> Unit, onValue: (String,String) -> Unit, onPhoto: (String)->Unit, onVideo: (String)->Unit, onVoice: (String)->Unit, onComment: (String)->Unit, onChecklistDone: (String)->Unit, onChecklistProblem: (String,String,String)->Unit, onChecklistSkip: (String,String)->Unit, onComplete: () -> Unit, onSkip: (String)->Unit, onOpenAnimal: (String)->Unit, canEdit: Boolean = true) {
    when (task.operationType) {
        OperationType.INSEMINATION -> InseminationScreen(task, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, onOpenAnimal, canEdit)
        OperationType.PALPATION -> PalpationScreen(task, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, onOpenAnimal, canEdit)
        OperationType.WEIGHING -> WeighingScreen(task, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, onOpenAnimal, canEdit)
        OperationType.NEST_PREPARATION -> CageOperationScreen("RFID клетки", task, "Подготовка гнезда", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.NEST_CONTROL -> CageOperationScreen("RFID клетки", task, "Контроль гнезда: сытые/голодные/мертвые", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.NEST_SELECTION -> CageOperationScreen("RFID клетки", task, "Выравнивание / калибровка гнезда", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.OKROL -> CageOperationScreen("RFID клетки", task, "Окрол: учет живых и мертвых", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.LACTATION_CONTROL -> CageOperationScreen("RFID клетки", task, "Контроль лактации", onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.ANIMAL_TRANSFER, OperationType.ANIMAL_SETTLEMENT, OperationType.FEMALE_DELIVERY -> CageOperationScreen("RFID объекта", task, task.operationType.title, onBack, onBegin, onScan, onOpenRfidScanner, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.LIGHT_STIMULATION, OperationType.LIGHTING_CHECK -> LightAutomationTaskScreen(task, onBack, onBegin, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        OperationType.FEED_CHECK, OperationType.MANUAL_FEEDING -> FeedOperationScreen(task, onBack, onBegin, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
        else -> HangarGenericOperationScreen(task, definition, onBack, onBegin, onValue, onPhoto, onVideo, onVoice, onComment, onChecklistDone, onChecklistProblem, onChecklistSkip, onComplete, onSkip, canEdit)
    }
}
