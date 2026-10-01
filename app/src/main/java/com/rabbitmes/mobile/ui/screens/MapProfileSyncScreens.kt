package com.rabbitmes.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen

@Composable
fun SyncQueueScreen(shift: ShiftState, tasks: List<MobileTask>, onSync: () -> Unit, onBack: () -> Unit) {
    val pendingTasks = tasks.filter { it.offlineEvents > 0 }
    val statusColor = if (shift.isOnline) mobileSuccessGreen else Color(0xFFE98500)
    val hasPending = shift.pendingSyncEvents > 0 || pendingTasks.isNotEmpty()

    Scaffold(
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = MesSpacing.screenBottom),
        ) {
            item {
                AppHeader(
                    "Оффлайн-синхронизация",
                    "Статус: ${if (shift.isOnline) "онлайн" else "оффлайн"}",
                    onBack,
                )
            }
            item {
                MesCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text("Состояние", fontWeight = FontWeight.Bold)
                            Text(
                                if (shift.isOnline) "Онлайн: можно отправить накопленные изменения" else "Оффлайн: действия сохраняются локально",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusBadge(if (shift.isOnline) "Онлайн" else "Оффлайн", statusColor)
                    }
                    Spacer(Modifier.height(MesSpacing.contentGap))
                    Text("В очереди: ${shift.pendingSyncEvents} событий", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Задач к синхронизации: ${pendingTasks.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(MesSpacing.contentGap))
                    if (!shift.isOnline) {
                        Text(
                            "Синхронизация появится автоматически, когда устройство снова будет онлайн.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(MesSpacing.smallGap))
                    }
                    Button(
                        onClick = onSync,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = shift.isOnline && hasPending,
                    ) {
                        Icon(Icons.Default.Sync, null)
                        Spacer(Modifier.width(MesSpacing.smallGap))
                        Text(if (shift.isOnline) "Синхронизировать" else "Синхронизация недоступна офлайн")
                    }
                }
            }
            item {
                Text(
                    "Задачи на синхронизацию",
                    modifier = Modifier.padding(horizontal = MesSpacing.screenHorizontal, vertical = MesSpacing.contentGap),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (pendingTasks.isEmpty()) {
                item {
                    MesCard {
                        Text("Нет задач, ожидающих синхронизации", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(pendingTasks, key = { it.id }) { task ->
                    MesCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(task.title, fontWeight = FontWeight.Bold)
                                Text("${task.plannedStart} · ${task.operationTypeTitle}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TaskStatusBadge(task.status)
                        }
                        Spacer(Modifier.height(MesSpacing.smallGap))
                        StatusBadge("Ожидает отправки: ${task.offlineEvents}", Color(0xFFE98500))
                        Spacer(Modifier.height(MesSpacing.smallGap))
                        ProgressLine(task.checklist.count { it.status != ChecklistStatus.PENDING }, task.checklist.size)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(employee: Employee, tasks: List<MobileTask>, operations: List<OperationDefinition>, onLogout: () -> Unit) {
    val allowedOperations = operations
        .filter { employee.role in it.allowedRoles }
        .distinctBy { it.type }
        .sortedBy { it.type.title }

    Scaffold(
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { AppHeader("Профиль", employee.fullName, trailing = { TextButton(onClick = onLogout) { Text("Выйти") } }) }
            item { MesCard { Text(employee.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); StatusBadge(employee.role.title, MaterialTheme.colorScheme.primary); Spacer(Modifier.height(MesSpacing.contentGap)); Text("Выполнено: ${tasks.count { it.status == TaskStatus.SENT || it.status == TaskStatus.DONE }}"); Text("Проблемы: ${tasks.sumOf { it.checklist.count { item -> item.status == ChecklistStatus.PROBLEM } }}") } }
            item {
                MesCard {
                    Text(
                        "Допустимые операции",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(MesSpacing.contentGap))
                    AllowedOperationsDropdown(allowedOperations.map { it.type.title })
                }
            }
        }
    }
}

@Composable
private fun AllowedOperationsDropdown(operations: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = operations.isNotEmpty()) { expanded = !expanded },
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${operations.size} операций",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    if (operations.isEmpty()) {
                        Text(
                            text = "Нет доступных операций",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (operations.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(MesSpacing.smallGap))
                operations.forEach { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 5.dp),
                    )
                }
            }
        }
    }
}
