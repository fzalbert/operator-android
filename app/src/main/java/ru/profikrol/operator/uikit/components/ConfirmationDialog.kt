package ru.profikrol.operator.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing
import ru.profikrol.operator.uikit.tokens.actionButtonShadowElevation
import ru.profikrol.operator.uikit.tokens.defaultPrimaryButtonHeight
import ru.profikrol.operator.uikit.tokens.dialogElevation
import ru.profikrol.operator.uikit.tokens.targetIconSize

private val ConfirmationDialogShape = RoundedCornerShape(Radii.xl)

@Composable
fun ConfirmationDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int? = null,
    cancelText: String? = null,
    confirmText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ConfirmationDialogContent(
            title = title,
            iconResId = iconResId,
            cancelText = cancelText ?: stringResource(R.string.cancel),
            confirmText = confirmText ?: stringResource(R.string.confirm),
            onDismiss = onDismiss,
            onConfirm = onConfirm,
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
fun ConfirmationDialogContent(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconResId: Int? = null,
    cancelText: String = stringResource(R.string.cancel),
    confirmText: String = stringResource(R.string.confirm),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xl),
        shape = ConfirmationDialogShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = dialogElevation,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Spacing.lg,
                vertical = Spacing.lg,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (iconResId != null) {
                    Icon(
                        modifier = Modifier.size(targetIconSize),
                        painter = painterResource(iconResId),
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            content()

            Spacer(Modifier.height(Spacing.xl))

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedButton(
                    text = cancelText,
                    onClick = onDismiss,
                    showDefaultIcon = false,
                    centerContent = true,
                    modifier = Modifier
                        .weight(1f)
                        .height(defaultPrimaryButtonHeight),
                )
                OutlinedButton(
                    text = confirmText,
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
