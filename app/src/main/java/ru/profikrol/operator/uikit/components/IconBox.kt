package ru.profikrol.operator.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.ElementSizes


/**
 * Квадрат с иконкой внутри — переиспользуемый «логотип»/декоративная иконка.
 * Используется в MainTopBar (brand colors) и в ActionCard (нейтральные colors).
 *
 * Параметры цвета/формы вынесены наружу — компонент сам по себе нейтрален.
 */
@Composable
fun IconBox(
    icon: Painter,
    modifier: Modifier = Modifier,
    size: Dp = ElementSizes.IconBox,
    iconSize: Dp = ElementSizes.IconBoxIcon,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Overload для ImageVector. */
@Composable
fun IconBox(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = ElementSizes.IconBox,
    iconSize: Dp = ElementSizes.IconBoxIcon,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = MaterialTheme.shapes.small,
) {
    IconBox(
        icon = rememberVectorPainter(icon),
        modifier = modifier,
        size = size,
        iconSize = iconSize,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        shape = shape,
    )
}

// ---------- Previews ----------

@Preview(name = "IconBox — primary", showBackground = true)
@Composable
private fun IconBoxBrandPreview() {
    ProfikrolTheme {
        IconBox(icon = Icons.Default.Home)
    }
}

@Preview(name = "IconBox — нейтральный (surfaceVariant)", showBackground = true)
@Composable
private fun IconBoxNeutralPreview() {
    ProfikrolTheme {
        IconBox(
            icon = Icons.Default.Home,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
        )
    }
}
