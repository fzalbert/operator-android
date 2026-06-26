package ru.profikrol.operator.feature.rfidscanresult

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import ru.profikrol.operator.R
import ru.profikrol.operator.domain.model.Rabbit
import ru.profikrol.operator.uikit.components.ConfirmationDialog
import ru.profikrol.operator.uikit.components.ConfirmationDialogContent
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing

private val AnimalCardShape = RoundedCornerShape(Radii.lg)

@Composable
fun InseminationConfirmationDialog(
    rabbit: Rabbit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(R.string.insemination_dialog_title),
        iconResId = R.drawable.ic_insemination_accept,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    ) {
        InseminationDialogBody(rabbit = rabbit)
    }
}

@Composable
private fun AnimalInfoCard(
    rabbit: Rabbit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, AnimalCardShape)
            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.insemination_dialog_animal_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(Spacing.md))

        Text(
            text = rabbit.rfidCode,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(Spacing.xl))

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimalInfoValue(
                label = stringResource(R.string.rfid_scan_result_age_label),
                value = rabbit.age,
            )
            AnimalInfoValue(
                label = stringResource(R.string.rfid_scan_result_cage_label),
                value = rabbit.cage,
            )
        }
    }
}

@Composable
private fun AnimalInfoValue(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.insemination_dialog_field_format, label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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

@Preview(
    name = "InseminationDialog — обычный",
    showBackground = true,
    backgroundColor = 0xFF888888,
)
@Composable
private fun InseminationDialogPreview() {
    ProfikrolTheme {
        InseminationConfirmationDialogPreviewContent(
            rabbit = PreviewRabbit,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(
    name = "InseminationDialog — длинный RFID",
    showBackground = true,
    backgroundColor = 0xFF888888,
)
@Composable
private fun InseminationDialogLongCodePreview() {
    ProfikrolTheme {
        InseminationConfirmationDialogPreviewContent(
            rabbit = PreviewRabbit.copy(
                rfidCode = "RF-99999-XYZ-LONG-CODE",
                age = "12 мес",
                cage = "B-104",
            ),
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview(
    name = "InseminationDialog — тёмная тема",
    showBackground = true,
    backgroundColor = 0xFF000000,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun InseminationDialogDarkPreview() {
    ProfikrolTheme(darkTheme = true) {
        InseminationConfirmationDialogPreviewContent(
            rabbit = PreviewRabbit,
            onDismiss = {},
            onConfirm = {},
        )
    }
}

/**
 * Содержимое диалога без обёртки Dialog.
 * @Preview не умеет показывать настоящие Dialog'и (это другое окно ОС),
 * поэтому для превью извлекаем "лист" отдельно.
 */
@Composable
private fun InseminationConfirmationDialogPreviewContent(
    rabbit: Rabbit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmationDialogContent(
        title = stringResource(R.string.insemination_dialog_title),
        iconResId = R.drawable.ic_insemination_accept,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
    ) {
        InseminationDialogBody(rabbit = rabbit)
    }
}

@Composable
private fun InseminationDialogBody(
    rabbit: Rabbit,
) {
    AnimalInfoCard(rabbit = rabbit)
    Spacer(Modifier.height(Spacing.xl))

    Text(
        text = stringResource(R.string.insemination_dialog_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
