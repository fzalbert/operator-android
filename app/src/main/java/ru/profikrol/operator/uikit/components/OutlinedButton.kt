package ru.profikrol.operator.uikit.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.uikit.theme.actionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.onActionButtonDisabledLight
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.defaultPrimaryButtonHeight
import ru.profikrol.operator.uikit.tokens.outlinedButtonTextStyle

private val DefaultOutlinedButtonIconSize = 20.dp

@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(Radii.lg)
    val contentColor = if (enabled) actionButtonPrimaryLight else onActionButtonDisabledLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(defaultPrimaryButtonHeight)
            .clip(shape)
            .border(width = 2.dp, color = contentColor, shape = shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = actionButtonPrimaryLight),
                onClick = onClick,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(DefaultOutlinedButtonIconSize),
                tint = contentColor,
            )
            Text(
                text = text,
                style = outlinedButtonTextStyle,
                color = contentColor,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
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
