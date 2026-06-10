package ru.profikrol.operator.feature.rfidscan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.components.PrimaryButton
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing

private const val RfidScanAreaAspectRatio = 1f
private const val RfidScanAreaBackgroundAlpha = 0.56f
private val RfidScanAreaBorderWidth = 2.dp
private val RfidScanAreaDashLength = 14.dp
private val RfidScanAreaDashGap = 10.dp
private val RfidScanAreaIconSize = 110.dp

@Composable
fun RfidScanScreen(
    onBack: () -> Unit,
    onScanned: (code: String) -> Unit,
    viewModel: RfidScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is RfidScanEvent.Scanned -> onScanned(event.code)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.rfid_scan_title),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        RfidScanContent(
            isScanning = state.isScanning,
            onDemoScanClick = viewModel::onDemoScanClick,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.xxl),
        )
    }
}

@Composable
private fun RfidScanContent(
    isScanning: Boolean,
    onDemoScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RfidScanArea(
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = stringResource(R.string.rfid_scan_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        PrimaryButton(
            text = stringResource(R.string.rfid_scan_demo_button),
            onClick = onDemoScanClick,
            enabled = !isScanning,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xl),
        )
    }
}

@Composable
fun RfidScanArea(
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radii.lg)
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val scanAreaColor = MaterialTheme.colorScheme.primaryContainer.copy(
        alpha = RfidScanAreaBackgroundAlpha,
    )
    val borderColor = contentColor.copy(alpha = RfidScanAreaBackgroundAlpha)

    Box(
        modifier = modifier
            .aspectRatio(RfidScanAreaAspectRatio)
            .clip(shape)
            .background(scanAreaColor)
            .dashedBorder(
                color = borderColor,
                cornerRadius = Radii.lg,
                strokeWidth = RfidScanAreaBorderWidth,
                dashLength = RfidScanAreaDashLength,
                dashGap = RfidScanAreaDashGap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_square),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(RfidScanAreaIconSize),
            )
    }
}

private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp,
    dashLength: Dp,
    dashGap: Dp,
): Modifier = drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    val halfStrokeWidthPx = strokeWidthPx / 2
    val cornerRadiusPx = cornerRadius.toPx()

    drawRoundRect(
        color = color,
        topLeft = Offset(halfStrokeWidthPx, halfStrokeWidthPx),
        size = size.copy(
            width = size.width - strokeWidthPx,
            height = size.height - strokeWidthPx,
        ),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashLength.toPx(), dashGap.toPx()),
            ),
        ),
    )
}
