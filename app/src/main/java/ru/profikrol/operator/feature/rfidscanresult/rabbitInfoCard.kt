package ru.profikrol.operator.feature.rfidscanresult

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R
import ru.profikrol.operator.domain.model.Rabbit
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Alpha
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing
import ru.profikrol.operator.uikit.tokens.cardElevation
import ru.profikrol.operator.uikit.tokens.defaultBorderWidth
import ru.profikrol.operator.uikit.tokens.defaultInfoRowHeight

/** Заглушка, чтобы во время загрузки Column рисовался и задавал высоту карточки. */
private val PlaceholderRabbit = Rabbit(
    rfidCode = "—",
    status = "—",
    age = "—",
    cage = "—",
    weight = "—",
    diagnosis = "—",
)

@Composable
fun RabbitInfoCard(
    isLoading: Boolean,
    rabbit: Rabbit?,
) {
    // При загрузке/отсутствии кролика рисуем плейсхолдер с теми же полями.
    // Так Column всегда занимает свою «реальную» высоту, а loading совпадает с loaded.
    val displayRabbit = rabbit ?: PlaceholderRabbit

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radii.md),
        border = BorderStroke(defaultBorderWidth, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = cardElevation,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Контент — рендерится всегда, во время загрузки прячется alpha(0f).
            // Высоту карточки задаёт он.
            Column(
                modifier = Modifier
                    .alpha(if (isLoading) 0f else 1f)
                    .padding(
                        horizontal = Spacing.xl,
                        vertical = Spacing.lg,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = displayRabbit.rfidCode,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    StatusChip(text = displayRabbit.status)
                }

                Spacer(Modifier.height(Spacing.lg))

                RabbitInfoRow(
                    label = stringResource(R.string.rfid_scan_result_age_label),
                    value = displayRabbit.age,
                )
                RabbitInfoRow(
                    label = stringResource(R.string.rfid_scan_result_cage_label),
                    value = displayRabbit.cage,
                )
                RabbitInfoRow(
                    label = stringResource(R.string.rfid_scan_result_weight_label),
                    value = displayRabbit.weight,
                )
                RabbitInfoRow(
                    label = stringResource(R.string.rfid_scan_result_diagnosis_label),
                    value = displayRabbit.diagnosis,
                    showDivider = false,
                )
            }

            // Прогресс — поверх контента, по центру.
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(Radii.xl))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun RabbitInfoRow(
    label: String,
    value: String,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(defaultInfoRowHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider))
        }
    }
}

// ---------- Previews ----------

private val PreviewRabbit = Rabbit(
    rfidCode = "RF-00247",
    status = "Здоров",
    age = "8 мес",
    cage = "A-12",
    weight = "3.4 кг",
    diagnosis = "Без замечаний",
)

@Preview(name = "RabbitInfoCard — обычный", showBackground = true)
@Composable
private fun RabbitInfoCardPreview() {
    ProfikrolTheme {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            RabbitInfoCard(isLoading = false, rabbit = PreviewRabbit)
        }
    }
}

@Preview(name = "RabbitInfoCard — loading", showBackground = true)
@Composable
private fun RabbitInfoCardLoadingPreview() {
    ProfikrolTheme {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            RabbitInfoCard(isLoading = true, rabbit = null)
        }
    }
}

@Preview(name = "RabbitInfoCard — длинный код", showBackground = true)
@Composable
private fun RabbitInfoCardLongCodePreview() {
    ProfikrolTheme {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            RabbitInfoCard(
                isLoading = false,
                rabbit = PreviewRabbit.copy(
                    rfidCode = "RF-99999-XYZ",
                    status = "Карантин",
                    diagnosis = "Конъюнктивит, наблюдение",
                ),
            )
        }
    }
}

@Preview(
    name = "RabbitInfoCard — тёмная тема",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun RabbitInfoCardDarkPreview() {
    ProfikrolTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(Spacing.lg)) {
            RabbitInfoCard(isLoading = false, rabbit = PreviewRabbit)
        }
    }
}
