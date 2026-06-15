package ru.profikrol.operator.feature.rfidscan

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
private const val RfidScanAnimationDurationMillis = 1_100
private const val RfidScanLineAlpha = 0.92f
private const val RfidScanBandAlpha = 0.18f

@Composable
fun RfidScanScreen(
    onBack: () -> Unit,
    onScanned: (code: String) -> Unit,
    viewModel: RfidScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(context) {
        val activity = context.findActivity()
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

        if (activity != null && nfcAdapter != null && nfcAdapter.isEnabled) {
            nfcAdapter.enableReaderMode(
                activity,
                { tag -> viewModel.onNfcTagScanned(tag.toScannedPayload()) },
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V,
                null,
            )
        }

        onDispose {
            if (activity != null && nfcAdapter != null) {
                nfcAdapter.disableReaderMode(activity)
            }
        }
    }

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
            isDemoScanInProgress = state.isDemoScanInProgress,
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
    isDemoScanInProgress: Boolean,
    onDemoScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RfidScanArea(
            isScanning = isScanning,
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
            enabled = !isDemoScanInProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xl),
        )
    }
}

@Composable
fun RfidScanArea(
    isScanning: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Radii.lg)
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val scanAreaColor = MaterialTheme.colorScheme.primaryContainer.copy(
        alpha = RfidScanAreaBackgroundAlpha,
    )
    val borderColor = contentColor.copy(alpha = RfidScanAreaBackgroundAlpha)
    val transition = rememberInfiniteTransition(label = "rfid_scan_transition")
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = RfidScanAnimationDurationMillis),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rfid_scan_progress",
    )

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
        if (isScanning) {
            RfidScanLine(
                progress = scanProgress,
                color = contentColor,
                modifier = Modifier.matchParentSize(),
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_square),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(RfidScanAreaIconSize),
        )
    }
}

@Composable
private fun RfidScanLine(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val lineY = size.height * progress
        val bandHeight = size.height * 0.18f
        val bandTop = (lineY - bandHeight / 2).coerceIn(0f, size.height)

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = RfidScanBandAlpha),
                    Color.Transparent,
                ),
                startY = bandTop,
                endY = bandTop + bandHeight,
            ),
            topLeft = Offset(0f, bandTop),
            size = Size(width = size.width, height = bandHeight),
        )

        drawLine(
            color = color.copy(alpha = RfidScanLineAlpha),
            start = Offset(0f, lineY),
            end = Offset(size.width, lineY),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round,
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

private fun Tag.toScannedPayload(): String =
    readNdefText().orEmpty().ifBlank { id.toHexString() }

private fun Tag.readNdefText(): String? {
    val ndef = Ndef.get(this) ?: return null
    return runCatching {
        ndef.connect()
        val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
        message
            ?.records
            ?.mapNotNull(NdefRecord::toText)
            ?.joinToString(separator = "\n")
    }.also {
        runCatching { ndef.close() }
    }.getOrNull()
}

private fun NdefRecord.toText(): String? {
    return when {
        tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_TEXT) -> {
            val status = payload.firstOrNull()?.toInt() ?: return null
            val languageCodeLength = status and 0x3F
            val textStart = 1 + languageCodeLength
            if (payload.size <= textStart) return null

            val charset = if (status and 0x80 == 0) Charsets.UTF_8 else Charsets.UTF_16
            String(payload, textStart, payload.size - textStart, charset)
        }

        tnf == NdefRecord.TNF_MIME_MEDIA && String(type) == "text/plain" -> {
            String(payload, Charsets.UTF_8)
        }

        else -> null
    }
}

private fun ByteArray.toHexString(): String =
    joinToString(separator = "") { byte ->
        "%02X".format(byte)
    }

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
