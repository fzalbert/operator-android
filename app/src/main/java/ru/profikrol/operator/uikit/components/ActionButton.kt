package ru.profikrol.operator.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.theme.actionButtonDisabledLight
import ru.profikrol.operator.uikit.theme.actionButtonMutedSuccessLight
import ru.profikrol.operator.uikit.theme.actionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.actionButtonSecondaryLight
import ru.profikrol.operator.uikit.theme.actionButtonSuccessLight
import ru.profikrol.operator.uikit.theme.actionButtonWarningLight
import ru.profikrol.operator.uikit.theme.onActionButtonDisabledLight
import ru.profikrol.operator.uikit.theme.onActionButtonMutedSuccessLight
import ru.profikrol.operator.uikit.theme.onActionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.onActionButtonSecondaryLight
import ru.profikrol.operator.uikit.theme.onActionButtonSuccessLight
import ru.profikrol.operator.uikit.theme.onActionButtonWarningLight
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing
import ru.profikrol.operator.uikit.tokens.actionButtonCompactTextStyle
import ru.profikrol.operator.uikit.tokens.defaultPrimaryButtonHeight

private val ActionButtonCompactIconSize = 20.dp
private val ActionButtonHorizontalPadding = Spacing.xs

enum class ActionButtonVariant {
    Primary,
    Secondary,
    Success,
    Warning,
}

enum class ActionButtonSize {
    Compact,
}

enum class ActionButtonIcon(@DrawableRes val resId: Int) {
    Scan(R.drawable.ic_scan_label),
    Sawdust(R.drawable.ic_nest_preparation),
    Check(R.drawable.ic_accept_button),
    CheckCircle(R.drawable.ic_accept_moving),
    Warning(R.drawable.ic_warning),
}

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ActionButtonVariant = ActionButtonVariant.Primary,
    size: ActionButtonSize = ActionButtonSize.Compact,
    icon: ActionButtonIcon? = null,
    enabled: Boolean = true,
) {
    val colors = variant.colors()
    val metrics = size.metrics()
    val elevation = variant.elevation(metrics)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .padding(horizontal = ActionButtonHorizontalPadding)
            .fillMaxWidth()
            .defaultMinSize(minHeight = metrics.height),
        shape = metrics.shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.container,
            contentColor = colors.content,
            disabledContainerColor = colors.disabledContainer,
            disabledContentColor = colors.disabledContent,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = elevation.default,
            pressedElevation = elevation.pressed,
            disabledElevation = elevation.default,
        ),
        contentPadding = metrics.contentPadding,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                space = metrics.iconTextSpacing,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon.resId),
                    contentDescription = null,
                    modifier = Modifier.size(metrics.iconSize),
                )
            }

            Text(
                text = text,
                style = metrics.textStyle,
            )
        }
    }
}

private data class ActionButtonColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color = actionButtonDisabledLight,
    val disabledContent: Color = onActionButtonDisabledLight,
)

private data class ActionButtonMetrics(
    val height: Dp,
    val iconSize: Dp,
    val iconTextSpacing: Dp,
    val shape: RoundedCornerShape,
    val elevation: Dp,
    val pressedElevation: Dp,
    val contentPadding: PaddingValues,
    val textStyle: TextStyle,
)

private data class ActionButtonElevation(
    val default: Dp,
    val pressed: Dp,
)

private fun ActionButtonVariant.colors(): ActionButtonColors =
    when (this) {
        ActionButtonVariant.Primary -> ActionButtonColors(
            container = actionButtonPrimaryLight,
            content = onActionButtonPrimaryLight,
        )

        ActionButtonVariant.Secondary -> ActionButtonColors(
            container = actionButtonSecondaryLight,
            content = onActionButtonSecondaryLight,
        )

        ActionButtonVariant.Success -> ActionButtonColors(
            container = actionButtonSuccessLight,
            content = onActionButtonSuccessLight,
            disabledContainer = actionButtonMutedSuccessLight,
            disabledContent = onActionButtonMutedSuccessLight,
        )

        ActionButtonVariant.Warning -> ActionButtonColors(
            container = actionButtonWarningLight,
            content = onActionButtonWarningLight,
        )

    }

private fun ActionButtonVariant.elevation(metrics: ActionButtonMetrics): ActionButtonElevation =
    when (this) {
        ActionButtonVariant.Secondary -> ActionButtonElevation(
            default = 0.dp,
            pressed = 0.dp,
        )

        else -> ActionButtonElevation(
            default = metrics.elevation,
            pressed = metrics.pressedElevation,
        )
    }

@Composable
private fun ActionButtonSize.metrics(): ActionButtonMetrics =
    when (this) {
        ActionButtonSize.Compact -> ActionButtonMetrics(
            height = defaultPrimaryButtonHeight,
            iconSize = ActionButtonCompactIconSize,
            iconTextSpacing = Spacing.xs,
            shape = RoundedCornerShape(Radii.sm),
            elevation = 6.dp,
            pressedElevation = 2.dp,
            contentPadding = PaddingValues(horizontal = Spacing.xl),
            textStyle = actionButtonCompactTextStyle,
        )
    }

@Preview(widthDp = 600)
@Composable
private fun ActionButtonScanPreview() {
    MaterialTheme {
        ActionButton(
            text = "Сканировать метку реципиента",
            icon = ActionButtonIcon.Scan,
            onClick = {},
        )
    }
}

@Preview(widthDp = 680)
@Composable
private fun ActionButtonAllCompactStatesPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ActionButton(
                text = "Сканировать метку реципиента",
                icon = ActionButtonIcon.Scan,
                onClick = {},
            )
            ActionButton(
                text = "Завершить",
                icon = ActionButtonIcon.Check,
                variant = ActionButtonVariant.Success,
                enabled = false,
                onClick = {},
            )
            ActionButton(
                text = "Метка установлена!",
                icon = ActionButtonIcon.CheckCircle,
                variant = ActionButtonVariant.Success,
                onClick = {},
            )
            ActionButton(
                text = "Подтвердить выбраковку",
                icon = ActionButtonIcon.Warning,
                variant = ActionButtonVariant.Warning,
                onClick = {},
            )
            ActionButton(
                text = "Редактировать профиль",
                variant = ActionButtonVariant.Secondary,
                onClick = {},
            )
            ActionButton(
                text = "Зарегистрировать",
                icon = ActionButtonIcon.Check,
                enabled = false,
                onClick = {},
            )
        }
    }
}

@Preview(widthDp = 1016)
@Composable
private fun ActionButtonSecondaryPreview() {
    MaterialTheme {
        ActionButton(
            text = "Редактировать профиль",
            variant = ActionButtonVariant.Secondary,
            onClick = {},
        )
    }
}

@Preview(widthDp = 1016)
@Composable
private fun ActionButtonSawdustPreview() {
    MaterialTheme {
        ActionButton(
            text = "Засыпать опилки",
            icon = ActionButtonIcon.Sawdust,
            onClick = {},
        )
    }
}

@Preview(widthDp = 680)
@Composable
private fun ActionButtonStatesPreview() {
    MaterialTheme {
        ActionButton(
            text = "Завершить",
            icon = ActionButtonIcon.Check,
            variant = ActionButtonVariant.Success,
            enabled = false,
            onClick = {},
        )
    }
}
