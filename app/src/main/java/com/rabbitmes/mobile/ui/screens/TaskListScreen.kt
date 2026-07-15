package com.rabbitmes.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*

@Composable
fun TaskListScreen(
    tasks: List<MobileTask>,
    nextTask: MobileTask?,
    message: String?,
    shiftStarted: Boolean,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var tab by remember { mutableStateOf("upcoming") }
    val upcoming = tasks
        .filter { it.status != TaskStatus.DONE && it.status != TaskStatus.SENT && it.status != TaskStatus.SKIPPED }
        .sortedWith(compareBy<MobileTask> { it.priority.weight }.thenBy { it.plannedStart })
    val completed = tasks
        .filter { it.status == TaskStatus.DONE || it.status == TaskStatus.SENT || it.status == TaskStatus.SKIPPED }
        .sortedByDescending { it.plannedStart }
    val visible = if (tab == "upcoming") upcoming else completed

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { AppHeader("Мои задачи", "Предстоящие и выполненные задания", onBack) }
            if (!shiftStarted) {
                item {
                    MesCard {
                        Text("Задачи доступны только после начала смены", fontWeight = FontWeight.Bold)
                        Text("Начните смену на главном экране, чтобы увидеть предстоящие задачи.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                if (message != null) item { MesCard { Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
                if (nextTask != null && tab == "upcoming") item {
                    MesCard(onClick = { onOpen(nextTask.id) }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Следующая", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(nextTask.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("${nextTask.plannedStart} · ${nextTask.operationType.title}")
                            }
                            Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { onOpen(nextTask.id) }, Modifier.fillMaxWidth()) { Text("Начать") }
                    }
                }
                item {
                    Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tab == "upcoming", onClick = { tab = "upcoming" }, label = { Text("Предстоящие (${upcoming.size})") })
                        FilterChip(selected = tab == "completed", onClick = { tab = "completed" }, label = { Text("Выполненные (${completed.size})") })
                    }
                }
                if (visible.isEmpty()) item { MesCard { Text(if (tab == "upcoming") "Предстоящих задач нет" else "Выполненных задач пока нет") } }
                items(visible) { task -> TaskCard(task, isNext = nextTask?.id == task.id) { onOpen(task.id) } }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(task: MobileTask, isNext: Boolean = false, onClick: () -> Unit) {
    MesCard(onClick = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("${task.plannedStart} · ${task.operationType.title}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TaskStatusBadge(task.status)
        }
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatusBadge(if (isNext) "Можно начать" else "Просмотр", if (isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            PriorityBadge(task.priority)
            StatusBadge(if (task.requiresAcceptance) "Приемка" else "Без приемки", if (task.requiresAcceptance) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
            StatusBadge("${task.plannedDurationMinutes} мин", operationAccent(task.operationType))
        }
        Spacer(Modifier.height(10.dp))
        ProgressLine(task.checklist.count { it.status != ChecklistStatus.PENDING }, task.checklist.size)
        if (task.offlineEvents > 0) {
            Spacer(Modifier.height(8.dp))
            StatusBadge("Offline: ${task.offlineEvents}", Color(0xFFE98500))
        }
    }
}
