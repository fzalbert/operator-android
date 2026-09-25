
package com.rabbitmes.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rabbitmes.mobile.domain.*
import ru.profikrol.operator.uikit.theme.mobileSuccessGreen

private val MesWarning = Color(0xFFE98500)

object MesSpacing {
    val screenHorizontal = 18.dp
    val screenBottom = 24.dp
    val cardVertical = 8.dp
    val cardInner = 18.dp
    val headerHorizontal = 18.dp
    val headerVertical = 8.dp
    val contentGap = 12.dp
    val smallGap = 8.dp
    val tinyGap = 4.dp
}
val LocalMesCardBorderEnabled = compositionLocalOf { true }

@Composable
fun isAccessibilityFontScale(): Boolean = LocalDensity.current.fontScale >= 1.3f

@Composable
fun MesCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showBorder: Boolean = LocalMesCardBorderEnabled.current,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickable = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        clickable.padding(horizontal = MesSpacing.screenHorizontal, vertical = MesSpacing.cardVertical).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (showBorder) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(Modifier.padding(MesSpacing.cardInner), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    label: String,
    modifier: Modifier = Modifier,
    openImmediately: Boolean = false,
) {
    if (options.size > 100) {
        SearchableSelectionDropdown(value, onValueChange, options, label, modifier, openImmediately)
        return
    }
    var expanded by remember { mutableStateOf(openImmediately && options.isNotEmpty()) }
    LaunchedEffect(openImmediately, options) {
        if (openImmediately && options.isNotEmpty() && value.isBlank()) expanded = true
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchableSelectionDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    label: String,
    modifier: Modifier,
    openImmediately: Boolean,
) {
    var dialogOpen by remember { mutableStateOf(openImmediately && options.isNotEmpty()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(openImmediately, options) {
        if (openImmediately && options.isNotEmpty() && value.isBlank()) dialogOpen = true
    }
    val filteredOptions = remember(options, query) {
        options.asSequence()
            .filter { query.isBlank() || it.matchesCellSearch(query) }
            .take(100)
            .toList()
    }

    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable {
                    query = ""
                    dialogOpen = true
                },
        )
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            icon = { Icon(Icons.Default.GridView, contentDescription = null, tint = mobileSuccessGreen) },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Выберите клетку", fontWeight = FontWeight.ExtraBold)
                    Text(
                        label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Номер ряда, клетки или ID") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить поиск")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 460.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        filteredOptions.forEach { option ->
                            item(key = option) {
                                val cell = option.toCellSelectionOption()
                                val selected = option == value
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                        onValueChange(option)
                                        dialogOpen = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) {
                                        mobileSuccessGreen.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) mobileSuccessGreen else MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(cell.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            if (cell.subtitle.isNotBlank()) {
                                                Text(
                                                    cell.subtitle,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 13.sp,
                                                )
                                            }
                                        }
                                        Text(
                                            cell.id,
                                            color = mobileSuccessGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        )
                                        if (selected) {
                                            Spacer(Modifier.width(6.dp))
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Выбрано",
                                                tint = mobileSuccessGreen,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (filteredOptions.isEmpty()) {
                        Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { dialogOpen = false }) { Text("Закрыть") }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
        )
    }
}

private data class CellSelectionOption(
    val title: String,
    val subtitle: String,
    val id: String,
)

private fun String.matchesCellSearch(rawQuery: String): Boolean {
    val query = rawQuery.trim().lowercase().replace('ё', 'е')
    if (query.isBlank()) return true

    val parts = split('·').map { it.trim().lowercase().replace('ё', 'е') }
    val id = parts.firstOrNull { it.startsWith("id ") }?.substringAfter("id ")?.trim()
    val row = parts.firstOrNull { it.startsWith("ряд ") }?.substringAfter("ряд ")?.trim()
    val cell = parts.firstOrNull { it.startsWith("клетка ") }?.substringAfter("клетка ")?.trim()

    if (query.all(Char::isDigit)) return cell == query || id == query

    val normalized = parts.joinToString(" ").replace('_', ' ')
    val tokens = query.replace('_', ' ').split(Regex("\\s+")).filter(String::isNotBlank)
    val numericTokens = tokens.filter { it.all(Char::isDigit) }
    val textTokens = tokens.filterNot { it.all(Char::isDigit) }

    val numbersMatch = when {
        numericTokens.isEmpty() -> true
        query.contains("id") -> numericTokens.all { it == id }
        query.contains("ряд") && query.contains("клет") && numericTokens.size >= 2 ->
            numericTokens[0] == row && numericTokens[1] == cell
        query.contains("ряд") -> numericTokens.all { it == row }
        query.contains("клет") -> numericTokens.all { it == cell }
        numericTokens.size >= 2 -> numericTokens[0] == row && numericTokens[1] == cell
        else -> numericTokens.all { it == cell || it == id }
    }
    return numbersMatch && textTokens.all { token ->
        token in normalized ||
            (token.startsWith("верх") && "верх" in normalized) ||
            (token.startsWith("ниж") && "ниж" in normalized) ||
            (token.startsWith("лев") && "лев" in normalized) ||
            (token.startsWith("прав") && "прав" in normalized)
    }
}

private fun String.toCellSelectionOption(): CellSelectionOption {
    val parts = split('·').map(String::trim)
    val id = parts.firstOrNull()?.takeIf { it.startsWith("ID ", ignoreCase = true) }.orEmpty()
    val row = parts.firstOrNull { it.startsWith("ряд ", ignoreCase = true) }.orEmpty()
    val cell = parts.firstOrNull { it.startsWith("клетка ", ignoreCase = true) }.orEmpty()
    val position = parts.lastOrNull()
        ?.takeUnless { it == cell || it == row || it == id }
        ?.replace('_', ' ')
        .orEmpty()
        .lowercase()
        .replaceFirstChar { it.uppercase() }
    return if (row.isNotBlank() || cell.isNotBlank()) {
        CellSelectionOption(
            title = listOf(row, cell).filter(String::isNotBlank).joinToString(" · ")
                .replaceFirstChar { it.uppercase() },
            subtitle = position,
            id = id,
        )
    } else {
        CellSelectionOption(title = this, subtitle = "", id = "")
    }
}

@Composable
fun AppHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val accessibilityFontScale = isAccessibilityFontScale()

    Surface(
        color = Color(0xFF0B2F24),
        contentColor = Color.White,
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = MesSpacing.headerHorizontal, vertical = MesSpacing.headerVertical),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                if (subtitle != null) {
                    Text(
                        subtitle,
                        color = Color(0xFFA7DCC2),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = if (accessibilityFontScale) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(content = trailing)
        }
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Surface(
        modifier = Modifier.widthIn(min = 44.dp),
        shape = RoundedCornerShape(99.dp),
        color = color.copy(alpha = .12f),
    ) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            softWrap = true,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
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
    OperationType.INSEMINATION, OperationType.PALPATION, OperationType.WEIGHING, OperationType.WEIGHING_CAGE, OperationType.WEIGHING_RABBIT, OperationType.ANIMAL_TRANSFER, OperationType.FEMALE_DELIVERY -> MaterialTheme.colorScheme.primary
    OperationType.NEST_PREPARATION, OperationType.NEST_CONTROL, OperationType.NEST_SELECTION, OperationType.OKROL, OperationType.LACTATION_CONTROL -> MaterialTheme.colorScheme.tertiary
    OperationType.WASHING, OperationType.DISINFECTION, OperationType.CLEANING, OperationType.DAILY_CLEANING, OperationType.HANGAR_ACCEPTANCE -> mobileSuccessGreen
    OperationType.LIGHT_STIMULATION, OperationType.LIGHTING_CHECK, OperationType.FEED_CHECK, OperationType.WATER_CHECK, OperationType.DEWORMING_DOSATRON -> MesWarning
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable fun ProgressLine(done: Int, total: Int) {
    if (total <= 0) return
    val pct = done.toFloat() / total
    Box(Modifier.fillMaxWidth().height(9.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))) {
        Box(Modifier.fillMaxWidth(pct).height(9.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)))
    }
    Spacer(Modifier.height(MesSpacing.tinyGap)); Text("$done / $total", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
}

@Composable fun MetricTile(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier.padding(MesSpacing.cardVertical),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(MesSpacing.cardInner), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(MesSpacing.smallGap)); Column { Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge); Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
fun BottomNav(current: String, syncCount: Int = 0, onSelect: (String) -> Unit) {
    val accessibilityFontScale = isAccessibilityFontScale()
    val items = listOf(
        Triple("shift", "Смена", Icons.Default.Home),
        Triple("tasks", "Задачи", Icons.AutoMirrored.Filled.Assignment),
        Triple("accept", "Приёмка", Icons.Default.Verified),
        Triple("sync", if (syncCount > 0) "Синхр. · $syncCount" else "Синхр.", Icons.Default.Sync),
        Triple("profile", "Профиль", Icons.Default.Person),
    )
    Surface(color = Color.White, shadowElevation = 12.dp, modifier = Modifier.fillMaxWidth()) {
        if (accessibilityFontScale) {
            Column(
                Modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowItems.forEach { (key, label, icon) ->
                            BottomNavItem(
                                key = key,
                                label = label,
                                icon = icon,
                                active = current == key,
                                accessibilityFontScale = true,
                                onSelect = onSelect,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Row(
                Modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEach { (key, label, icon) ->
                    BottomNavItem(
                        key = key,
                        label = label,
                        icon = icon,
                        active = current == key,
                        accessibilityFontScale = false,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    key: String,
    label: String,
    icon: ImageVector,
    active: Boolean,
    accessibilityFontScale: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = if (accessibilityFontScale) 64.dp else 54.dp)
            .background(if (active) Color(0xFFE4F5EC) else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable { onSelect(key) }
            .padding(horizontal = 4.dp, vertical = if (accessibilityFontScale) 8.dp else 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            null,
            tint = if (active) Color(0xFF1F8A5B) else Color(0xFF60726A),
            modifier = Modifier.size(if (accessibilityFontScale) 24.dp else 21.dp),
        )
        Text(
            label,
            color = if (active) Color(0xFF1F8A5B) else Color(0xFF60726A),
            style = if (accessibilityFontScale) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            maxLines = if (accessibilityFontScale) 1 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
