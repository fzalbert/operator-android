package ru.profikrol.operator.uikit.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import ru.profikrol.operator.uikit.theme.statusBannerInfoContainerLight
import ru.profikrol.operator.uikit.theme.statusBannerInfoLight
import ru.profikrol.operator.uikit.theme.statusBannerInfoOutlineLight
import ru.profikrol.operator.uikit.theme.statusBannerWarningContainerLight
import ru.profikrol.operator.uikit.theme.statusBannerWarningLight
import ru.profikrol.operator.uikit.theme.statusBannerWarningOutlineLight
import ru.profikrol.operator.uikit.theme.statusBannerWarningTitleLight
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing
import ru.profikrol.operator.uikit.tokens.statusBannerWarningTextStyle
import ru.profikrol.operator.uikit.tokens.statusBannerWarningTitleTextStyle

private val StatusBannerInfoIconSize = 40.dp
private val StatusBannerInfoBorderWidth = 1.dp
private val StatusBannerInfoShape = RoundedCornerShape(Radii.md)
private val StatusBannerWarningIconSize = 42.dp
private val StatusBannerWarningBorderWidth = 2.dp
private val StatusBannerWarningShape = RoundedCornerShape(14.dp)

enum class StatusBannerStatus {
    Info,
    Warning,
}

@Composable
fun StatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    status: StatusBannerStatus = StatusBannerStatus.Info,
    title: String? = null,
) {
    val colors = status.colors()
    val metrics = status.metrics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(metrics.borderWidth, colors.border),
                shape = metrics.shape,
            )
            .background(
                color = colors.background,
                shape = metrics.shape,
            )
            .padding(
                start = metrics.startPadding,
                top = metrics.topPadding,
                end = metrics.endPadding,
                bottom = metrics.bottomPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(metrics.iconTextSpacing),
        verticalAlignment = if (title.isNullOrBlank()) {
            Alignment.CenterVertically
        } else {
            Alignment.Top
        },
    ) {
        StatusBannerIcon(
            status = status,
            tint = colors.icon,
            modifier = Modifier.size(metrics.iconSize),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(metrics.titleTextSpacing),
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = status.titleTextStyle(),
                    color = colors.title,
                )
            }

            Text(
                text = text,
                style = status.textStyle(),
                color = colors.text,
            )
        }
    }
}

private data class StatusBannerColors(
    val background: Color,
    val border: Color,
    val icon: Color,
    val title: Color,
    val text: Color,
)

private data class StatusBannerMetrics(
    val borderWidth: Dp,
    val shape: RoundedCornerShape,
    val iconSize: Dp,
    val iconTextSpacing: Dp,
    val titleTextSpacing: Dp,
    val startPadding: Dp,
    val topPadding: Dp,
    val endPadding: Dp,
    val bottomPadding: Dp,
)

private fun StatusBannerStatus.colors(): StatusBannerColors =
    when (this) {
        StatusBannerStatus.Info -> StatusBannerColors(
            background = statusBannerInfoContainerLight,
            border = statusBannerInfoOutlineLight,
            icon = statusBannerInfoLight,
            title = statusBannerInfoLight,
            text = statusBannerInfoLight,
        )

        StatusBannerStatus.Warning -> StatusBannerColors(
            background = statusBannerWarningContainerLight,
            border = statusBannerWarningOutlineLight,
            icon = statusBannerWarningLight,
            title = statusBannerWarningTitleLight,
            text = statusBannerWarningLight,
        )
    }

private fun StatusBannerStatus.metrics(): StatusBannerMetrics =
    when (this) {
        StatusBannerStatus.Info -> StatusBannerMetrics(
            borderWidth = StatusBannerInfoBorderWidth,
            shape = StatusBannerInfoShape,
            iconSize = StatusBannerInfoIconSize,
            iconTextSpacing = Spacing.lg,
            titleTextSpacing = Spacing.sm,
            startPadding = Spacing.xl,
            topPadding = Spacing.lg,
            endPadding = Spacing.xl,
            bottomPadding = Spacing.lg,
        )

        StatusBannerStatus.Warning -> StatusBannerMetrics(
            borderWidth = StatusBannerWarningBorderWidth,
            shape = StatusBannerWarningShape,
            iconSize = StatusBannerWarningIconSize,
            iconTextSpacing = 36.dp,
            titleTextSpacing = Spacing.sm,
            startPadding = 36.dp,
            topPadding = 28.dp,
            endPadding = 36.dp,
            bottomPadding = 28.dp,
        )
    }

@Composable
private fun StatusBannerStatus.titleTextStyle(): TextStyle =
    when (this) {
        StatusBannerStatus.Info -> MaterialTheme.typography.headlineMedium
        StatusBannerStatus.Warning -> statusBannerWarningTitleTextStyle
    }

@Composable
private fun StatusBannerStatus.textStyle(): TextStyle =
    when (this) {
        StatusBannerStatus.Info -> MaterialTheme.typography.headlineSmall
        StatusBannerStatus.Warning -> statusBannerWarningTextStyle
    }

@Composable
private fun StatusBannerIcon(
    status: StatusBannerStatus,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    when (status) {
        StatusBannerStatus.Info -> Icon(
            painter = painterResource(R.drawable.ic_birth),
            contentDescription = null,
            tint = tint,
            modifier = modifier,
        )

        StatusBannerStatus.Warning -> Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = tint,
            modifier = modifier,
        )
    }
}

@Preview(widthDp = 1016)
@Composable
private fun StatusBannerWarningPreview() {
    StatusBanner(
        status = StatusBannerStatus.Info,
        title = "Внимание!",
        text = "Это действие необратимо. Животное будет удалено из системы.",
    )
}
