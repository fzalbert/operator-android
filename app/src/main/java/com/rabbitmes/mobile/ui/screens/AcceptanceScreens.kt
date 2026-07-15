package com.rabbitmes.mobile.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen

@Composable
fun AcceptanceQueueScreen(tasks: List<MobileTask>, onOpen: (String) -> Unit, onBack: () -> Unit, bottomBar: @Composable () -> Unit) {
    Scaffold(
        bottomBar = bottomBar,
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { AppHeader("Приемка", "Задачи, ожидающие проверку", onBack) }
            if (tasks.isEmpty()) item { MesCard { Text("Нет задач на приемку") } }
            items(tasks) { task -> TaskCard(task) { onOpen(task.id) } }
        }
    }
}

@Composable
fun AcceptanceScreen(
    task: MobileTask,
    remarks: List<AcceptanceRemark>,
    onBack: () -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onRemark: (String?, String, String, List<MediaAttachment>) -> Unit
) {
    var finalComment by remember { mutableStateOf("Все проверено, замечаний нет") }
    val localProblemItems = remember { mutableStateMapOf<String, Boolean>() }
    val hasProblems = localProblemItems.values.any { it }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp)) {
        item { AppHeader("Приемка", task.title, onBack) }
        item {
            MesCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("Проверка выполненной задачи")
                    }
                    TaskStatusBadge(task.status)
                }
                Spacer(Modifier.height(10.dp))
                ProgressLine(task.checklist.count { it.status == ChecklistStatus.DONE }, task.checklist.size)
                Text("Если все нормально — просто нажмите «Готово». Если есть проблема по объекту, включите ползунок у этого объекта и заполните карточку замечания.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(task.checklist) { item ->
            AcceptanceItemCard(
                item = item,
                problemEnabled = localProblemItems[item.id] == true,
                onProblemToggle = { enabled -> localProblemItems[item.id] = enabled },
                onRemark = { reason, itemComment, attachments -> onRemark(item.id, reason, itemComment, attachments) }
            )
        }
        item {
            MesCard {
                Text("Итог приемки", fontWeight = FontWeight.Bold)
                OutlinedTextField(finalComment, { finalComment = it }, Modifier.fillMaxWidth(), label = { Text("Комментарий проверяющего") })
                Spacer(Modifier.height(8.dp))
                if (!hasProblems) {
                    Button(onClick = { onAccept(finalComment) }, Modifier.fillMaxWidth()) { Text("Готово") }
                } else {
                    Text("Есть отмеченные проблемы. Сначала сохраните замечания по проблемным объектам, затем верните задачу на доработку.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onReject(finalComment) }, Modifier.fillMaxWidth()) { Text("Вернуть на доработку") }
                }
            }
        }
        item {
            MesCard {
                Text("Журнал замечаний", fontWeight = FontWeight.Bold)
                val taskRemarks = remarks.filter { it.taskId == task.id }
                if (taskRemarks.isEmpty()) Text("Замечаний пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
                taskRemarks.forEach { remark ->
                    Spacer(Modifier.height(8.dp))
                    Text("• ${remark.reason}: ${remark.comment}", color = MaterialTheme.colorScheme.error)
                    if (remark.attachments.isNotEmpty()) {
                        Text(remark.attachments.joinToString("  ") { "${it.type.emoji} ${it.name}" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AcceptanceItemCard(
    item: ChecklistItem,
    problemEnabled: Boolean,
    onProblemToggle: (Boolean) -> Unit,
    onRemark: (String, String, List<MediaAttachment>) -> Unit
) {
    var reason by remember { mutableStateOf("Не выполнено / дефект") }
    var comment by remember { mutableStateOf("") }
    var attachments by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }
    MesCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(item.label, fontWeight = FontWeight.Bold)
                Text("Исполнение: ${item.status.title}")
                if (item.result.scannedRfid != null) Text("RFID: ${item.result.scannedRfid}", color = mobileSuccessGreen)
                if (item.result.values.isNotEmpty()) Text(item.result.values.entries.joinToString { "${it.key}: ${it.value}" })
            }
            StatusBadge(if (problemEnabled) "Проблема" else "OK", if (problemEnabled) MaterialTheme.colorScheme.error else mobileSuccessGreen)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Есть проблема", fontWeight = FontWeight.SemiBold)
            Switch(checked = problemEnabled, onCheckedChange = onProblemToggle)
        }
        if (problemEnabled) {
            Spacer(Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Карточка проблемы", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("Причина") })
                    OutlinedTextField(comment, { comment = it }, Modifier.fillMaxWidth(), label = { Text("Комментарий") })
                    ReviewAttachmentButtons(attachments, onAttachments = { attachments = it })
                    OutlinedButton(onClick = { onRemark(reason, comment, attachments) }, Modifier.fillMaxWidth()) { Text("Сохранить проблему") }
                }
            }
        }
    }
}

@Composable
private fun ReviewAttachmentButtons(attachments: List<MediaAttachment>, onAttachments: (List<MediaAttachment>) -> Unit) {
    val context = LocalContext.current
    fun create(type: AttachmentType, ext: String): MediaAttachment {
        val time = System.currentTimeMillis()
        return MediaAttachment("review-media-$time", type, "review-$time.$ext", "mock://review-$time.$ext", "now", uploaded = false)
    }
    fun open(type: AttachmentType) {
        val intent = when (type) {
            AttachmentType.PHOTO -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            AttachmentType.VIDEO -> Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            AttachmentType.VOICE -> Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
        }
        try { context.startActivity(intent) } catch (_: ActivityNotFoundException) { }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        ReviewMediaButton("📷", "Фото", Modifier.weight(1f)) {
            open(AttachmentType.PHOTO)
            onAttachments(attachments + create(AttachmentType.PHOTO, "jpg"))
        }
        ReviewMediaButton("🎥", "Видео", Modifier.weight(1f)) {
            open(AttachmentType.VIDEO)
            onAttachments(attachments + create(AttachmentType.VIDEO, "mp4"))
        }
        ReviewMediaButton("🎤", "Голос", Modifier.weight(1f)) {
            open(AttachmentType.VOICE)
            onAttachments(attachments + create(AttachmentType.VOICE, "m4a"))
        }
    }
    if (attachments.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        attachments.forEach { Text("${it.type.emoji} ${it.name}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ReviewMediaButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(icon, maxLines = 1, softWrap = false)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
