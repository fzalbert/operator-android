package com.rabbitmes.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen

@Composable
fun ShiftScreen(
    employee: Employee,
    shift: ShiftState,
    tasks: List<MobileTask>,
    nextTask: MobileTask?,
    message: String?,
    unreadNotifications: Int,
    onStart: () -> Unit,
    onFinish: (String) -> Unit,
    onOpenNext: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var finishReason by remember { mutableStateOf("Смена завершена штатно") }
    val shiftStarted = shift.startedAt != null
    val upcoming = tasks.count { it.status != TaskStatus.DONE && it.status != TaskStatus.SENT && it.status != TaskStatus.SKIPPED }
    val completed = tasks.count { it.status == TaskStatus.DONE || it.status == TaskStatus.SENT || it.status == TaskStatus.SKIPPED }
    val doneItems = tasks.sumOf { it.checklist.count { item -> item.status == ChecklistStatus.DONE || item.status == ChecklistStatus.PROBLEM || item.status == ChecklistStatus.SKIPPED } }
    val totalItems = tasks.sumOf { it.checklist.size }
    val review = tasks.count { it.requiresAcceptance && it.status == TaskStatus.DONE && it.acceptanceStatus == AcceptanceStatus.WAITING }

    Scaffold(
        bottomBar = bottomBar,
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = MesSpacing.screenBottom)) {
            item {
                AppHeader("Рабочая смена", "${employee.fullName} · ${employee.role.title}", trailing = {
                    IconButton(onClick = onOpenNotifications) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge { Text(unreadNotifications.toString()) }
                                }
                            },
                        ) {
                            Icon(Icons.Default.Notifications, null)
                        }
                    }
                    Box(Modifier.size(48.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Icon(
                            if (shift.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                            null,
                            tint = if (shift.isOnline) mobileSuccessGreen else Color(0xFFE98500),
                        )
                    }
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, null) }
                })
            }
            item { Row { MetricTile("Предстоящие", if (shiftStarted) upcoming.toString() else "—", Icons.Default.Schedule, Modifier.weight(1f)); MetricTile("Выполненные", if (shiftStarted) completed.toString() else "—", Icons.Default.DoneAll, Modifier.weight(1f)) } }
            item { Row { MetricTile("Приемка", review.toString(), Icons.Default.Verified, Modifier.weight(1f)); MetricTile("Sync", shift.pendingSyncEvents.toString(), Icons.Default.Sync, Modifier.weight(1f)) } }
            item {
                MesCard {
                    Text(if (!shiftStarted) "Смена не начата" else "Смена начата в ${shift.startedAt}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(MesSpacing.contentGap))
                    ProgressLine(doneItems, totalItems)
                    Text(if (shift.isOnline) "Онлайн" else "Оффлайн: ${shift.pendingSyncEvents} событий", color = if (shift.isOnline) mobileSuccessGreen else Color(0xFFE98500))
                }
            }
            if (message != null) item { MesCard { Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) } }
            if (shiftStarted && nextTask != null) item {
                MesCard(onClick = { onOpenNext(nextTask.id) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Следующая задача", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(nextTask.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${nextTask.plannedStart} · ${nextTask.operationType.title}")
                        }
                        PriorityBadge(nextTask.priority)
                    }
                    Spacer(Modifier.height(MesSpacing.contentGap))
                    ProgressLine(nextTask.checklist.count { it.status != ChecklistStatus.PENDING }, nextTask.checklist.size)
                    Button(onClick = { onOpenNext(nextTask.id) }, Modifier.fillMaxWidth()) { Text("Открыть задачу") }
                }
            }
            item {
                MesCard {
                    if (!shiftStarted) {
                        Button(onClick = onStart, Modifier.fillMaxWidth()) { Text("Начать смену") }
                    } else {
                        OutlinedTextField(finishReason, { finishReason = it }, Modifier.fillMaxWidth(), label = { Text("Причина закрытия") })
                        Spacer(Modifier.height(MesSpacing.contentGap))
                        OutlinedButton(onClick = { onFinish(finishReason) }, Modifier.fillMaxWidth()) { Text("Закончить смену") }
                    }
                }
            }
        }
    }
}
