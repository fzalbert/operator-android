package com.rabbitmes.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.domain.*
import com.rabbitmes.mobile.ui.components.*
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen

@Composable
fun HangarMapScreen(workshop: Workshop, tasks: List<MobileTask>, onOpenTask: (String) -> Unit, onBack: () -> Unit, bottomBar: @Composable () -> Unit) {
    val hangar = workshop.hangars.first()
    Scaffold(
        bottomBar = bottomBar,
        containerColor = Color.Transparent,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = MesSpacing.screenBottom)) {
            item { AppHeader("Карта ангара", "${workshop.name} · ${hangar.name}", onBack) }
            item { MesCard { Text("Клетки подсвечены по задачам смены", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap)) { StatusBadge("План", MaterialTheme.colorScheme.primary); StatusBadge("Готово", mobileSuccessGreen); StatusBadge("Проблема", MaterialTheme.colorScheme.error) } } }
            hangar.rows.forEach { row -> item { Text("Ряд ${row.number}", Modifier.padding(horizontal = MesSpacing.screenHorizontal, vertical = MesSpacing.smallGap), fontWeight = FontWeight.Bold) }; item { Row(Modifier.padding(horizontal = MesSpacing.screenHorizontal).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap)) { row.cages.forEach { cage -> val task = tasks.firstOrNull { it.checklist.any { item -> item.targetId == cage.id } }; val item = task?.checklist?.firstOrNull { it.targetId == cage.id }; val color = when(item?.status) { ChecklistStatus.DONE -> mobileSuccessGreen; ChecklistStatus.PROBLEM -> MaterialTheme.colorScheme.error; ChecklistStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant; ChecklistStatus.PENDING -> MaterialTheme.colorScheme.primary; null -> MaterialTheme.colorScheme.outlineVariant }; Box(Modifier.weight(1f).height(42.dp).background(color.copy(alpha=.18f), RoundedCornerShape(10.dp)).clickable(enabled = task != null) { if (task != null) onOpenTask(task.id) }.padding(MesSpacing.tinyGap)) { Text(cage.number.toString(), color = color, style = MaterialTheme.typography.labelSmall) } } } } }
        }
    }
}

@Composable
fun SyncQueueScreen(shift: ShiftState, tasks: List<MobileTask>, onSync: () -> Unit, onBack: () -> Unit, bottomBar: @Composable () -> Unit) {
    val pendingTasks = tasks.filter { it.offlineEvents > 0 }
    val statusColor = if (shift.isOnline) mobileSuccessGreen else Color(0xFFE98500)
    val hasPending = shift.pendingSyncEvents > 0 || pendingTasks.isNotEmpty()

    Scaffold(
        bottomBar = bottomBar,
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
                                Text("${task.plannedStart} · ${task.operationType.title}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
@OptIn(ExperimentalLayoutApi::class)
fun ProfileScreen(employee: Employee, tasks: List<MobileTask>, operations: List<OperationDefinition>, onLogout: () -> Unit, bottomBar: @Composable () -> Unit) {
    val allowedOperations = operations
        .filter { employee.role in it.allowedRoles }
        .distinctBy { it.type }
        .sortedBy { it.type.title }

    Scaffold(
        bottomBar = bottomBar,
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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MesSpacing.smallGap),
                        verticalArrangement = Arrangement.spacedBy(MesSpacing.smallGap),
                    ) {
                        allowedOperations.forEach { operation ->
                            OperationReadOnlyChip(operation.type.title)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationReadOnlyChip(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
        ),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
fun AnimalHistoryScreen(rabbit: Rabbit, cage: Cage?, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { AppHeader("История животного", rabbit.rfid, onBack) }
        item { MesCard { Text("Кролик ${rabbit.earNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Пол: ${rabbit.sex}"); Text("Возраст: ${rabbit.ageDays} дней"); Text("Вес: ${"%.2f".format(rabbit.lastWeightKg)} кг"); Text("Клетка: ${cage?.code ?: "неизвестно"}"); Text("Лактация: ${rabbit.lactationStatus}"); Text("Здоровье: ${rabbit.healthStatus}") } }
        item { MesCard { Text("Последние события", fontWeight = FontWeight.Bold); Text("Осеменение · ${rabbit.lastInseminationDaysAgo ?: "нет"} дней назад"); Text("Пальпация · ${rabbit.lastPalpation ?: "нет"}"); Text("Последнее взвешивание · ${"%.2f".format(rabbit.lastWeightKg)} кг") } }
    }
}
