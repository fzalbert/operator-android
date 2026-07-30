package com.rabbitmes.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*

@Composable
fun TaskListScreen(tasks: List<MobileTask>, nextTask: MobileTask?, message: String?, shiftStarted: Boolean, onOpen: (String) -> Unit, onBack: () -> Unit, bottomBar: @Composable () -> Unit) {
    val open = tasks.filter { it.status != TaskStatus.DONE && it.status != TaskStatus.SENT && it.status != TaskStatus.SKIPPED }.sortedWith(compareBy<MobileTask> { it.priority.weight }.thenBy { it.plannedStart })
    val problems = open.sumOf { task -> task.checklist.count { it.status == ChecklistStatus.PROBLEM } }
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    Scaffold(bottomBar = bottomBar, containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { AppHeader("Мои задачи", "Профикроль", onBack) }
            item {
                if (largeFont) {
                    Text(
                        "${open.size} активных · ${open.count { it.priority == Priority.URGENT }} срочных · $problems проблем",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TaskMetric(open.size.toString(), "активных", Modifier.weight(1f)); TaskMetric(open.count { it.priority == Priority.URGENT }.toString(), "срочных", Modifier.weight(1f)); TaskMetric(problems.toString(), "проблем", Modifier.weight(1f))
                    }
                }
            }
            if (!shiftStarted) item { TaskEmpty("Начните смену, чтобы открыть задачи") }
            else if (open.isEmpty()) item { TaskEmpty("Задач нет") }
            else items(open, key = { it.id }) { task -> PrototypeTaskCard(task) { onOpen(task.id) } }
        }
    }
}

@Composable
fun TaskCard(task: MobileTask, isNext: Boolean = false, onClick: () -> Unit) = PrototypeTaskCard(task, onClick)

@Composable private fun PrototypeTaskCard(task: MobileTask, onClick: () -> Unit) {
    val accent = when (task.priority) { Priority.URGENT -> Color(0xFFDC4C4C); Priority.HIGH -> Color(0xFFF59E0B); Priority.NORMAL -> Color(0xFF1F8A5B) }
    val problems = task.checklist.count { it.status == ChecklistStatus.PROBLEM }
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(5.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).clickable(onClick = onClick)) {
        Row { Box(Modifier.width(5.dp).heightIn(min = 190.dp).background(accent)); Column(Modifier.weight(1f).padding(16.dp)) {
            if (largeFont) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskStatusBadge(task.status)
                    Text("${task.plannedStart} · ${task.plannedDurationMinutes} мин", color = Color(0xFF60726A), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { TaskStatusBadge(task.status); Text("${task.plannedStart} · ${task.plannedDurationMinutes} мин", color = Color(0xFF60726A), fontSize = 12.sp) }
            }
            Spacer(Modifier.height(12.dp)); Text(task.title, color = Color(0xFF10231B), fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(6.dp)); Text(task.operationType.title, color = Color(0xFF60726A), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            if (largeFont) {
                Text(
                    "${task.checklist.count { it.status == ChecklistStatus.DONE }}/${task.checklist.size} пунктов · $problems замечаний · приёмка: ${if (task.requiresAcceptance) "да" else "нет"}",
                    color = Color(0xFF60726A),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TaskMetric("${task.checklist.count { it.status == ChecklistStatus.DONE }}/${task.checklist.size}", "пунктов", Modifier.weight(1f)); TaskMetric(problems.toString(), "замечаний", Modifier.weight(1f)); TaskMetric(if (task.requiresAcceptance) "Да" else "Нет", "приёмка", Modifier.weight(1f)) }
            }
        } }
    }
}

@Composable private fun TaskMetric(value: String, label: String, modifier: Modifier) { Column(modifier.background(Color.White, RoundedCornerShape(15.dp)).padding(11.dp)) { Text(value, color = Color(0xFF10231B), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold); Text(label, color = Color(0xFF60726A), fontSize = 11.sp) } }
@Composable private fun TaskEmpty(text: String) { Surface(color = Color.White, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) { Text(text, color = Color(0xFF60726A), fontSize = 16.sp, modifier = Modifier.padding(24.dp)) } }
