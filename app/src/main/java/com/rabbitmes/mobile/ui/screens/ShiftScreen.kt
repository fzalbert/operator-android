package com.rabbitmes.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*

private val ShiftGreen = Color(0xFF1F8A5B)
private val ShiftDark = Color(0xFF0B2F24)

@Composable
fun ShiftScreen(
    employee: Employee,
    shift: ShiftState,
    tasks: List<MobileTask>,
    nextTask: MobileTask?,
    message: String?,
    unreadNotifications: Int,
    isShiftActionInProgress: Boolean,
    isTasksLoading: Boolean,
    onStart: () -> Unit,
    onFinish: (String) -> Unit,
    onOpenNext: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val active = shift.startedAt != null && shift.finishedAt == null
    val openTasks = tasks.filter { it.status != TaskStatus.DONE && it.status != TaskStatus.SENT && it.status != TaskStatus.SKIPPED }
    val totalItems = openTasks.sumOf { it.checklist.size }
    val closedItems = openTasks.sumOf { task -> task.checklist.count { it.status != ChecklistStatus.PENDING } }
    val progress = if (totalItems == 0) 0f else closedItems.toFloat() / totalItems
    val greetingName = employee.fullName.greetingName()

    Scaffold(bottomBar = bottomBar, containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppHeader("Смена", "Профикроль${if (shift.pendingSyncEvents > 0) " · ${shift.pendingSyncEvents} в очереди" else ""}", trailing = {
                    IconButton(onClick = onOpenNotifications) { BadgedBox({ if (unreadNotifications > 0) Badge { Text(unreadNotifications.toString()) } }) { Icon(Icons.Default.Notifications, null, tint = Color.White) } }
                    Icon(if (shift.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff, null, tint = Color(0xFF48D491), modifier = Modifier.padding(12.dp).size(26.dp))
                    IconButton(onClick = onLogout) { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color.White) }
                })
            }
            item {
                Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                    Row(Modifier.padding(14.dp, 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text(employee.fullName, fontWeight = FontWeight.Bold, color = Color(0xFF10231B))
                        Text(employee.role.title, color = Color(0xFF60726A), fontSize = 13.sp)
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(ShiftGreen, ShiftDark))).padding(16.dp),
                ) {
                    Text("Рабочая смена", color = Color(0xFFB9E5CF), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Text(if (shift.finishedAt != null) "Смена закрыта" else "Доброе утро, $greetingName", color = Color.White, fontSize = 25.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(10.dp).background(Color.White.copy(.18f), RoundedCornerShape(99.dp))) { if (progress > 0) Box(Modifier.fillMaxWidth(progress).height(10.dp).background(Color(0xFF48D491), RoundedCornerShape(99.dp))) }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShiftMetric(openTasks.size.toString(), "активных", Modifier.weight(1f))
                        ShiftMetric("$closedItems/$totalItems", "пунктов", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { if (active) onFinish("Смена завершена штатно") else onStart() },
                        enabled = !isShiftActionInProgress,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (active) Color(0xFFE4ECE8) else ShiftGreen, contentColor = if (active) ShiftDark else Color.White),
                    ) {
                        if (isShiftActionInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(if (active) "Закончить смену" else "Начать смену", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            if (isTasksLoading) item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Загружаем задачи…", color = Color(0xFF60726A))
                }
            }
            if (active && nextTask != null) item {
                PrototypeNextTaskCard(nextTask) { onOpenNext(nextTask.id) }
            }
            if (message != null) item { Surface(color = Color(0xFFE4F5EC), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) { Text(message, color = ShiftGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp)) } }
        }
    }
}

@Composable private fun ShiftMetric(value: String, label: String, modifier: Modifier) { Column(modifier.background(Color.White.copy(.13f), RoundedCornerShape(15.dp)).padding(11.dp)) { Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Text(label, color = Color(0xFFD9F1E5), fontSize = 12.sp) } }

private fun String.greetingName(): String {
    val parts = trim().split(Regex("\\s+")).filter(String::isNotBlank)
    return parts.getOrNull(1) ?: parts.firstOrNull().orEmpty()
}

@Composable private fun PrototypeNextTaskCard(task: MobileTask, onOpen: () -> Unit) {
    val accent = when (task.priority) { Priority.URGENT -> Color(0xFFDC4C4C); Priority.HIGH -> Color(0xFFF59E0B); Priority.NORMAL -> ShiftGreen }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(5.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Row { Box(Modifier.width(5.dp).height(190.dp).background(accent)); Column(Modifier.weight(1f).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { StatusBadge("Следующая", accent); Text(task.plannedStart, color = Color(0xFF60726A), fontSize = 13.sp) }
            Spacer(Modifier.height(12.dp)); Text(task.title, color = Color(0xFF10231B), fontSize = 20.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(7.dp)); Text("${task.operationTypeTitle} · прогресс ${task.progress}%", color = Color(0xFF60726A), fontSize = 14.sp); Spacer(Modifier.height(14.dp))
            Button(onOpen, Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(ShiftGreen)) { Text("Открыть задачу", fontWeight = FontWeight.ExtraBold) }
        } }
    }
}
