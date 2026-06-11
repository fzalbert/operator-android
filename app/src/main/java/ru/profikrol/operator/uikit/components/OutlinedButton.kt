package ru.profikrol.operator.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.uikit.theme.actionButtonDangerLight
import ru.profikrol.operator.uikit.theme.actionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.actionButtonWarningLight
import ru.profikrol.operator.uikit.theme.onActionButtonDangerLight
import ru.profikrol.operator.uikit.theme.onActionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.onActionButtonWarningLight
import ru.profikrol.operator.uikit.theme.onActionButtonDisabledLight
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.defaultPrimaryButtonHeight
import ru.profikrol.operator.uikit.tokens.outlinedButtonTextStyle

private val DefaultOutlinedButtonIconSize = 20.dp

enum class OutlinedButtonVariant {
    Outline,
    Filled,
    Danger,
}

@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: OutlinedButtonVariant = OutlinedButtonVariant.Outline,
    @DrawableRes iconResId: Int? = null,
    showDefaultIcon: Boolean = iconResId == null,
    centerContent: Boolean = false,
) {
    val shape = RoundedCornerShape(Radii.lg)
    val colors = variant.colors(enabled)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultPrimaryButtonHeight)
            .clip(shape)
            .background(color = colors.container, shape = shape)
            .border(width = colors.borderWidth, color = colors.border, shape = shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.content),
                onClick = onClick,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = if (centerContent) {
                Arrangement.Center
            } else {
                Arrangement.Start
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(DefaultOutlinedButtonIconSize),
                    tint = colors.content,
                )
            } else if (showDefaultIcon) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(DefaultOutlinedButtonIconSize),
                    tint = colors.content,
                )
            }
            Text(
                text = text,
                style = outlinedButtonTextStyle,
                color = colors.content,
                modifier = if (iconResId != null || showDefaultIcon) {
                    Modifier.padding(start = 8.dp)
                } else {
                    Modifier
                },
            )
        }
    }
}

private data class OutlinedButtonColors(
    val container: Color,
    val content: Color,
    val border: Color,
    val borderWidth: Dp,
)

private fun OutlinedButtonVariant.colors(enabled: Boolean): OutlinedButtonColors {
    if (!enabled) {
        return OutlinedButtonColors(
            container = Color.Transparent,
            content = onActionButtonDisabledLight,
            border = onActionButtonDisabledLight,
            borderWidth = 2.dp,
        )
    }

    return when (this) {
        OutlinedButtonVariant.Outline -> OutlinedButtonColors(
            container = Color.Transparent,
            content = actionButtonPrimaryLight,
            border = actionButtonPrimaryLight,
            borderWidth = 2.dp,
        )

        OutlinedButtonVariant.Filled -> OutlinedButtonColors(
            container = actionButtonPrimaryLight,
            content = onActionButtonPrimaryLight,
            border = Color.Transparent,
            borderWidth = 0.dp,
        )

        OutlinedButtonVariant.Danger -> OutlinedButtonColors(
            container = actionButtonDangerLight,
            content = onActionButtonDangerLight,
            border = Color.Transparent,
            borderWidth = 0.dp,
        )
    }
}

@Preview(showBackground = true, widthDp = 1304)
@Composable
private fun OutlinedButtonPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            OutlinedButton(
                text = "Перемещение",
                onClick = {},
            )
        }
    }
}
