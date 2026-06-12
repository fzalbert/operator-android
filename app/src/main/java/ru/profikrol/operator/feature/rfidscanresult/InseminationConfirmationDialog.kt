package ru.profikrol.operator.feature.rfidscanresult

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import ru.profikrol.operator.R
import ru.profikrol.operator.domain.model.Rabbit
import ru.profikrol.operator.uikit.components.OutlinedButton
import ru.profikrol.operator.uikit.components.OutlinedButtonVariant
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing
import ru.profikrol.operator.uikit.tokens.actionButtonShadowElevation
import ru.profikrol.operator.uikit.tokens.defaultPrimaryButtonHeight
import ru.profikrol.operator.uikit.tokens.dialogElevation
import ru.profikrol.operator.uikit.tokens.targetIconSize
import ru.profikrol.operator.uikit.tokens.targetIconStrokeWidth

private val DialogShape = RoundedCornerShape(Radii.xl)
private val AnimalCardShape = RoundedCornerShape(Radii.lg)

@Composable
fun InseminationConfirmationDialog(
    rabbit: Rabbit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = DialogShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = dialogElevation,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Spacing.xl,
                    vertical = Spacing.xl,
                ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    TargetIcon(
                        modifier = Modifier.size(targetIconSize),
                    )
                    Text(
                        text = stringResource(R.string.insemination_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(Modifier.height(Spacing.xl))

                AnimalInfoCard(rabbit = rabbit)

                Spacer(Modifier.height(Spacing.xl))

                Text(
                    text = stringResource(R.string.insemination_dialog_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Spacing.xl))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                        showDefaultIcon = false,
                        centerContent = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(defaultPrimaryButtonHeight),
                    )
                    OutlinedButton(
                        text = stringResource(R.string.confirm),
                        onClick = onConfirm,
                        variant = OutlinedButtonVariant.Filled,
                        showDefaultIcon = false,
                        centerContent = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(defaultPrimaryButtonHeight)
                            .shadow(actionButtonShadowElevation, RoundedCornerShape(Radii.lg)),
                    )
                }
            }
        }
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(Spacing.md))

        Text(
            text = rabbit.rfidCode,
            style = MaterialTheme.typography.headlineSmall,
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
    Row {
        Text(
            text = stringResource(R.string.insemination_dialog_field_format, label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TargetIcon(
    modifier: Modifier = Modifier,
) {
    val color = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = targetIconStrokeWidth.toPx()
            val center = this.center
            val maxRadius = (size.minDimension - stroke) / 2

            drawCircle(color = color, radius = maxRadius, center = center, style = Stroke(stroke))
            drawCircle(color = color, radius = maxRadius * 0.58f, center = center, style = Stroke(stroke))
            drawCircle(color = color, radius = maxRadius * 0.22f, center = center, style = Stroke(stroke))
            drawCircle(color = color, radius = maxRadius * 0.08f, center = center)
        }
    }
}
