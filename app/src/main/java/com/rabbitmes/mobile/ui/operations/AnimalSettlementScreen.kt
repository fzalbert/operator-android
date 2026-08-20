package com.rabbitmes.mobile.ui.operations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.domain.ChecklistStatus
import com.rabbitmes.mobile.domain.MobileTask
import com.rabbitmes.mobile.domain.TaskStatus

@Composable
fun ProductionAnimalSettlementScreen(
    task: MobileTask,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onSaveRfid: (String?, String) -> Unit,
    onComplete: () -> Unit,
    isSubmitting: Boolean,
    canEdit: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    val pending = task.checklist.filter { it.status == ChecklistStatus.PENDING }
    val current = pending.firstOrNull()
    val hasTaskLevelResult = task.result.scannedRfid?.isNotBlank() == true
    var rfid by remember(task.id, current?.id) { mutableStateOf("") }
    var validationError by remember(task.id, current?.id) { mutableStateOf<String?>(null) }

    fun submit(input: String = rfid) {
        val value = input.trim()
        validationError = when {
            current == null && hasTaskLevelResult -> "RFID уже сохранён"
            value.isBlank() -> "Отсканируйте RFID-метку"
            task.checklist.any { it.result.scannedRfid.equals(value, ignoreCase = true) } -> "Эта RFID-метка уже использована"
            else -> null
        }
        if (validationError == null) onSaveRfid(current?.id, value)
    }

    LaunchedEffect(task.status, current?.id, isSubmitting) {
        if (task.status != TaskStatus.NEW && (current != null || !hasTaskLevelResult) && !isSubmitting) focusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SimpleBackground).statusBarsPadding(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("← Назад", color = SimpleGreen, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clickable(onClick = onBack).padding(vertical = 4.dp)) }
        item {
            SimpleCard {
                Text(task.title, color = SimpleText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                if (task.description.isNotBlank()) Text(task.description, color = SimpleMuted)
                Text("Готово ${task.checklist.size - pending.size} из ${task.checklist.size}", color = SimpleGreen, fontWeight = FontWeight.Bold)
                if (task.status == TaskStatus.NEW && canEdit) SimpleButton("Приступить", onBegin, Modifier.fillMaxWidth())
            }
        }
        if (task.status != TaskStatus.NEW && (current != null || !hasTaskLevelResult) && canEdit) {
            item {
                SimpleCard {
                    Text("Текущая позиция", color = SimpleMuted)
                    Text(current?.label ?: "RFID заселяемого животного", color = SimpleText, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = rfid,
                        onValueChange = { raw ->
                            val hasTerminator = raw.any { it == '\n' || it == '\r' }
                            val cleaned = raw.filterNot { it == '\n' || it == '\r' }
                            rfid = cleaned
                            validationError = null
                            if (hasTerminator) submit(cleaned)
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onPreviewKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) { submit(); true } else false
                        },
                        enabled = !isSubmitting,
                        singleLine = true,
                        label = { Text("RFID") },
                        placeholder = { Text("Ожидание сканирования…") },
                        isError = validationError != null,
                        supportingText = validationError?.let { message -> { Text(message) } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        shape = RoundedCornerShape(16.dp),
                    )
                    SimpleButton("Сохранить RFID", { submit() }, Modifier.fillMaxWidth(), enabled = rfid.isNotBlank() && !isSubmitting)
                    if (isSubmitting) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Сохраняем RFID в базе…", color = SimpleMuted)
                    }
                }
            }
        }
        task.checklist.filter { it.status == ChecklistStatus.DONE }.forEach { item ->
            item(key = item.id) { SimpleCard { Text("✓ ${item.label}", color = SimpleGreen, fontWeight = FontWeight.Bold); item.result.scannedRfid?.let { Text("RFID: $it", color = SimpleText) } } }
        }
        if (hasTaskLevelResult) item { SimpleCard { Text("✓ RFID сохранён", color = SimpleGreen, fontWeight = FontWeight.Bold); Text("RFID: ${task.result.scannedRfid}", color = SimpleText) } }
        if (pending.isEmpty() && (task.checklist.isNotEmpty() || hasTaskLevelResult) && task.status != TaskStatus.DONE) {
            item { SimpleButton("Завершить заселение", onComplete, Modifier.fillMaxWidth(), enabled = !isSubmitting && canEdit) }
        }
        if (task.status == TaskStatus.DONE) item { SimpleCard { Text("Заселение успешно завершено", color = SimpleGreen, fontWeight = FontWeight.Bold) } }
    }
}
