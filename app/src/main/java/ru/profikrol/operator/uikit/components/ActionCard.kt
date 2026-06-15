package ru.profikrol.operator.uikit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.theme.ProfikrolTheme

private val ActionCardMinHeight = 56.dp
private val ActionCardElevation = 4.dp
private val ActionCardBorderWidth = 1.dp
private val ActionCardContentPaddingHorizontal = 12.dp
private val ActionCardContentPaddingVertical = 8.dp
private val ActionCardIconTitleSpacing = 12.dp

/**
 * Карточка-действие: иконка слева (в нейтральном квадрате), справа — title и description.
 * Используется для блоков выбора операций.
 */
@Composable
fun ActionCard(
    title: String,
    description: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ActionCardMinHeight),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(
            width = ActionCardBorderWidth,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ActionCardElevation,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = ActionCardContentPaddingHorizontal,
                    vertical = ActionCardContentPaddingVertical,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ActionCardIconTitleSpacing),
        ) {
            IconBox(
                icon = icon,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Overload, если иконка — `ImageVector`. */
@Composable
fun ActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ActionCard(
        title = title,
        description = description,
        icon = rememberVectorPainter(icon),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

// ---------- Previews ----------

@Preview(name = "ActionCard — обычный", showBackground = true)
@Composable
private fun ActionCardPreview() {
    ProfikrolTheme {
        ActionCard(
            title = "Подготовка гнёзд",
            description = "Подготовка ячеек к закладке",
            icon = Icons.Default.Info,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "ActionCard — длинное описание", showBackground = true)
@Composable
private fun ActionCardLongTextPreview() {
    ProfikrolTheme {
        ActionCard(
            title = "Выбраковка кроликов",
            description = "Отметить особей, которые не пойдут в дальнейшее разведение по решению технолога",
            icon = Icons.Default.Check,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "ActionCard — c drawable", showBackground = true)
@Composable
private fun ActionCardWithPainterPreview() {
    ProfikrolTheme {
        ActionCard(
            title = "На главную",
            description = "Вернуться на главный экран",
            icon = painterResource(R.drawable.ic_home_button),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(
    name = "ActionCard — тёмная тема",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ActionCardDarkPreview() {
    ProfikrolTheme(darkTheme = true) {
        ActionCard(
            title = "Установка RFID",
            description = "Установить RFID-метки на отмеченных особей",
            icon = Icons.Default.Add,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
