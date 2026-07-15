package com.rabbitmes.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.domain.*
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen

private val MesWarning = Color(0xFFE98500)

@Composable
fun MesCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val clickable = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        clickable.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun AppHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(content = trailing)
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(99.dp), color = color.copy(alpha = .12f)) { Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) }
}

@Composable fun TaskStatusBadge(status: TaskStatus) {
    val c = when(status) {
        TaskStatus.NEW -> MaterialTheme.colorScheme.primary
        TaskStatus.IN_PROGRESS -> MesWarning
        TaskStatus.BLOCKED -> MaterialTheme.colorScheme.error
        TaskStatus.DONE -> mobileSuccessGreen
        TaskStatus.SENT -> MaterialTheme.colorScheme.tertiary
        TaskStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    StatusBadge(status.title, c)
}

@Composable fun PriorityBadge(priority: Priority) {
    val c = when(priority) {
        Priority.URGENT -> MaterialTheme.colorScheme.error
        Priority.HIGH -> MesWarning
        Priority.NORMAL -> MaterialTheme.colorScheme.primary
    }
    StatusBadge(priority.title, c)
}

@Composable
fun operationAccent(type: OperationType): Color = when(type) {
    OperationType.INSEMINATION, OperationType.PALPATION, OperationType.WEIGHING, OperationType.ANIMAL_TRANSFER, OperationType.FEMALE_DELIVERY -> MaterialTheme.colorScheme.primary
    OperationType.NEST_PREPARATION, OperationType.NEST_CONTROL, OperationType.NEST_SELECTION, OperationType.OKROL, OperationType.LACTATION_CONTROL -> MaterialTheme.colorScheme.tertiary
    OperationType.WASHING, OperationType.DISINFECTION, OperationType.CLEANING, OperationType.DAILY_CLEANING, OperationType.HANGAR_ACCEPTANCE -> mobileSuccessGreen
    OperationType.LIGHT_STIMULATION, OperationType.LIGHTING_CHECK, OperationType.FEED_CHECK, OperationType.WATER_CHECK, OperationType.DEWORMING_DOSATRON -> MesWarning
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable fun ProgressLine(done: Int, total: Int) {
    val pct = if (total == 0) 0f else done.toFloat() / total
    Box(Modifier.fillMaxWidth().height(9.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))) {
        Box(Modifier.fillMaxWidth(pct).height(9.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)))
    }
    Spacer(Modifier.height(4.dp)); Text("$done / $total", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
}

@Composable fun MetricTile(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier.padding(8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp)); Column { Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge); Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable fun BottomNav(current: String, syncCount: Int = 0, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(selected = current == "shift", onClick = { onSelect("shift") }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Смена") })
        NavigationBarItem(selected = current == "tasks", onClick = { onSelect("tasks") }, icon = { Icon(Icons.AutoMirrored.Filled.Assignment, null) }, label = { Text("Задачи") })
        NavigationBarItem(selected = current == "accept", onClick = { onSelect("accept") }, icon = { Icon(Icons.Default.Verified, null) }, label = { Text("Приемка") })
        NavigationBarItem(selected = current == "sync", onClick = { onSelect("sync") }, icon = { BadgedBox(badge = { if(syncCount > 0) Badge { Text(syncCount.toString()) } }) { Icon(Icons.Default.Sync, null) } }, label = { Text("Sync") })
    }
}
