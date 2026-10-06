package com.rabbitmes.mobile.shift.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.shift.model.AssignedTask
import com.rabbitmes.mobile.shift.model.Hangar
import com.rabbitmes.mobile.shift.model.Workshop
import com.rabbitmes.mobile.ui.components.overdueLabel
import kotlinx.coroutines.delay
import ru.profikrol.operator.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------- 1. Top app bar ----------

/** Шапка смены: дата и «Профикроль · роль», справа уведомления и синхронизация. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftTopAppBar(
    header: ShiftHeaderState,
    onNotificationsClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = {
            Column {
                Text(todayTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.shift_subtitle, header.roleTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            NotificationsAction(header.unreadNotifications, onNotificationsClick)
            SyncStatus(header.isOnline, header.pendingSyncEvents)
        },
        scrollBehavior = scrollBehavior,
    )
}

/** Шапка выбора ангара: «назад» и заголовок. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickHangarTopAppBar(
    unreadNotifications: Int,
    onBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.shift_pick_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.shift_back))
            }
        },
        actions = { NotificationsAction(unreadNotifications, onNotificationsClick) },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun NotificationsAction(unread: Int, onClick: () -> Unit) {
    val description = if (unread > 0) {
        pluralStringResource(R.plurals.shift_notifications_unread, unread, unread)
    } else {
        stringResource(R.string.shift_notifications)
    }
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        BadgedBox(badge = { if (unread > 0) Badge() }) {
            Icon(Icons.Filled.Notifications, contentDescription = null)
        }
    }
}

/** Состояние связи, не нажимается: нет сети, есть неотправленное или всё отправлено. */
@Composable
private fun SyncStatus(isOnline: Boolean, pending: Int) {
    val colors = MaterialTheme.colorScheme
    val (icon, description, tint) = when {
        !isOnline -> Triple(Icons.Filled.CloudOff, stringResource(R.string.shift_offline), colors.onSurfaceVariant)
        pending > 0 -> Triple(
            Icons.Filled.CloudUpload,
            pluralStringResource(R.plurals.shift_sync_pending, pending, pending),
            colors.onSurfaceVariant,
        )
        else -> Triple(Icons.Filled.CloudDone, stringResource(R.string.shift_synced), colors.primary)
    }
    // Размер как у IconButton, чтобы иконки в шапке стояли ровно.
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

// ---------- 2. Карточка смены ----------

/** Градиентная карточка смены, общая для «не начата» и «идёт». */
@Composable
private fun ShiftCard(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.secondary,
        contentColor = colors.onSecondary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(colors.secondary, colors.onPrimaryContainer)))
                .padding(start = 20.dp, top = 22.dp, end = 20.dp, bottom = 20.dp),
        ) { content() }
    }
}

/** Смена не начата: приветствие и «Начать смену». */
@Composable
fun StartShiftCard(userName: String, onStartShift: () -> Unit) {
    ShiftCard {
        ShiftStatusChip(stringResource(R.string.shift_status_not_started), live = false)
        ShiftGreeting(userName, Modifier.padding(top = 14.dp, bottom = 6.dp))
        Text(
            stringResource(R.string.shift_idle_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = onCardMuted(),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onStartShift,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.shift_start), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Смена идёт: таймер, где работаю и «Закрыть смену» с подтверждением. */
@Composable
fun ActiveShiftCard(
    userName: String,
    startedAtMillis: Long,
    workshop: Workshop,
    hangar: Hangar,
    onCloseShift: () -> Unit,
) {
    var confirmClose by rememberSaveable { mutableStateOf(false) }
    val onCard = MaterialTheme.colorScheme.onSecondary
    ShiftCard {
        ShiftStatusChip(stringResource(R.string.shift_status_live, shiftDuration(startedAtMillis)), live = true)
        ShiftGreeting(userName, Modifier.padding(top = 14.dp, bottom = 18.dp))
        WorkplacePlate(workshop, hangar)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { confirmClose = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, onCard.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = onCard),
        ) {
            Text(stringResource(R.string.shift_close), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
    if (confirmClose) {
        CloseShiftDialog(
            onConfirm = {
                confirmClose = false
                onCloseShift()
            },
            onDismiss = { confirmClose = false },
        )
    }
}

/** Подтверждение закрытия смены. */
@Composable
private fun CloseShiftDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shift_close_confirm_title)) },
        text = { Text(stringResource(R.string.shift_close_confirm_text)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.shift_close)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.shift_close_cancel)) } },
    )
}

/** Плашка «цех / ангар». */
@Composable
private fun WorkplacePlate(workshop: Workshop, hangar: Hangar) {
    val onCard = MaterialTheme.colorScheme.onSecondary
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(onCard.copy(alpha = 0.1f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(onCard.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Warehouse, contentDescription = null)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(workshop.name, style = MaterialTheme.typography.bodyMedium, color = onCardMuted())
            Text(hangar.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ShiftStatusChip(text: String, live: Boolean) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .clip(CircleShape)
            .background(colors.onSecondary.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (live) colors.tertiary else colors.onSecondary.copy(alpha = 0.6f)),
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = onCardMuted())
    }
}

@Composable
private fun ShiftGreeting(userName: String, modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.shift_greeting, stringResource(greetingRes()), userName),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

/** Второстепенный текст на градиентной карточке. */
@Composable
private fun onCardMuted() = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)

// ---------- 3. Как проходит смена ----------

@Composable
fun ShiftStepsSection() {
    Column {
        Text(
            stringResource(R.string.shift_steps_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        listOf(R.string.shift_step_start, R.string.shift_step_pick, R.string.shift_step_task).forEachIndexed { index, text ->
            ShiftStep(number = index + 1, text = stringResource(text))
        }
    }
}

@Composable
private fun ShiftStep(number: Int, text: String) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(2.dp, colors.outlineVariant),
            color = colors.surface,
            contentColor = colors.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
}

// ---------- 4. Цех с ангарами ----------

/** Карточка цеха: название и ангары плитками в ряд. Выбор один на весь экран. */
@Composable
fun WorkshopCard(
    workshop: Workshop,
    selected: Hangar?,
    onSelect: (Hangar) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 12.dp, top = 14.dp, end = 12.dp, bottom = 12.dp)) {
            Text(
                workshop.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                workshop.hangars.forEach { hangar ->
                    HangarTile(hangar, selected = hangar == selected, onClick = { onSelect(hangar) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HangarTile(hangar: Hangar, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerLowest,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        border = BorderStroke(2.dp, if (selected) colors.primary else colors.outlineVariant),
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(hangar.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) colors.primary else colors.outlineVariant,
            )
        }
    }
}

/** Все цеха с общей radio-группой. */
@Composable
fun WorkshopList(workshops: List<Workshop>, selected: Hangar?, onSelect: (Hangar) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        workshops.forEach { workshop -> WorkshopCard(workshop, selected, onSelect) }
    }
}

// ---------- 5. Задача ----------

/** «Ваша задача»: ожидание назначения или карточка задачи. */
@Composable
fun TaskSection(task: AssignedTask?, onStartTask: (AssignedTask) -> Unit) {
    Column {
        Text(
            stringResource(R.string.shift_task_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        if (task == null) TaskWaiting() else AssignedTaskCard(task, onStart = { onStartTask(task) })
    }
}

@Composable
private fun TaskWaiting() {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .dashedBorder(colors.outline, radius = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceContainerLowest)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = colors.primary,
            trackColor = colors.primaryContainer,
            strokeWidth = 3.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.shift_task_waiting_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.shift_task_waiting_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Карточка задачи. Просроченная — с красной рамкой и датой вместо «Новая задача». */
@Composable
private fun AssignedTaskCard(task: AssignedTask, onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val overdueDate = task.overdueDate
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLowest,
        border = if (overdueDate != null) BorderStroke(2.dp, colors.error) else BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Text(
                (if (overdueDate != null) overdueLabel(overdueDate) else stringResource(R.string.shift_task_label)).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (overdueDate != null) colors.error else colors.primary,
            )
            Text(
                task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )
            Text(
                task.dueTime?.let { stringResource(R.string.shift_task_meta, task.hangarName, it) } ?: task.hangarName,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            SolidButton(stringResource(R.string.shift_task_begin), onClick = onStart)
        }
    }
}

/** Основная кнопка экрана (тёмно-зелёная, 56dp). */
@Composable
fun SolidButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

// ---------- helpers ----------

private fun Modifier.dashedBorder(color: androidx.compose.ui.graphics.Color, radius: androidx.compose.ui.unit.Dp) = drawBehind {
    val stroke = 1.dp.toPx()
    drawRoundRect(
        color = color,
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        cornerRadius = CornerRadius(radius.toPx()),
    )
}

@Composable
private fun todayTitle(): String {
    val text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru")))
    return text.replaceFirstChar { it.titlecase(Locale("ru")) }
}

private fun greetingRes(hour: Int = LocalTime.now().hour): Int = when (hour) {
    in 5..11 -> R.string.shift_greeting_morning
    in 12..17 -> R.string.shift_greeting_day
    in 18..22 -> R.string.shift_greeting_evening
    else -> R.string.shift_greeting_night
}

/** Длительность смены «ЧЧ:ММ», обновляется раз в секунду. */
@Composable
private fun shiftDuration(startedAtMillis: Long): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val elapsed = now - startedAtMillis
    val minutes = (elapsed / 60_000).coerceAtLeast(0)
    return "%02d:%02d".format(minutes / 60, minutes % 60)
}
