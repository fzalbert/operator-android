package ru.profikrol.operator.feature.nestalignment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.theme.actionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.onActionButtonPrimaryLight
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing

private val CardMutedContainer = Color(0xFFF8F9FA)
private val CounterContainer = Color(0xFFFAFAFA)
private val MutedText = Color(0xFF6B7280)
private val SummaryDangerContainer = Color(0xFFFFF1F1)
private val SummaryDangerBorder = Color(0xFFFFD6D6)
private val SummaryDangerText = Color(0xFFE7000B)
private val SummarySuccessContainer = Color(0xFFEFFDF5)
private val SummarySuccessBorder = Color(0xFFD7F5E2)
private val SummarySuccessText = Color(0xFF16651F)
private val InfoContainer = Color(0xFFEFF6FF)
private val InfoBorder = Color(0xFFBEDBFF)
private val InfoContent = Color(0xFF155DFC)
private val LoadingContainer = Color(0xFFF8F9FA)
private val ErrorText = Color(0xFFE7000B)

@Composable
fun NestAlignmentScreen(
    onBack: () -> Unit,
    onScanRfid: (NestAlignmentScanTarget) -> Unit,
    scannedTarget: NestAlignmentScanTarget? = null,
    scannedCode: String? = null,
    onScannedConsumed: () -> Unit = {},
    viewModel: NestAlignmentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val draftReadyMessage = stringResource(R.string.nest_alignment_confirm_placeholder)

    LaunchedEffect(scannedTarget, scannedCode) {
        val target = scannedTarget
        val code = scannedCode
        if (target != null && !code.isNullOrBlank()) {
            viewModel.onRfidScanned(target = target, rfidCode = code)
            onScannedConsumed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is NestAlignmentEvent.MoveDraftReady -> {
                    snackbarHostState.showSnackbar(draftReadyMessage)
                }
            }
        }
    }

    NestAlignmentContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onScanDonor = { onScanRfid(NestAlignmentScanTarget.Donor) },
        onScanRecipient = { onScanRfid(NestAlignmentScanTarget.Recipient) },
        onTransferCountChange = viewModel::onTransferCountChange,
        onClearTarget = viewModel::onClearTarget,
        onConfirm = { viewModel.onConfirmClick() },
        onRestart = viewModel::onRestartClick,
    )
}

@Composable
private fun NestAlignmentContent(
    state: NestAlignmentUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onScanDonor: () -> Unit,
    onScanRecipient: () -> Unit,
    onTransferCountChange: (Int) -> Unit,
    onClearTarget: (NestAlignmentScanTarget) -> Unit,
    onConfirm: () -> Unit,
    onRestart: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nest_alignment_title),
                onBack = onBack,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        ) {
            InfoBanner(
                text = stringResource(R.string.nest_alignment_hint),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.lg))

            SectionTitle(text = stringResource(R.string.nest_alignment_from))

            Spacer(Modifier.height(Spacing.sm))

            when {
                state.donor != null -> {
                    NestCard(
                        nest = state.donor,
                        onClear = { onClearTarget(NestAlignmentScanTarget.Donor) },
                    )
                }

                state.isDonorLoading -> LoadingNestCard()
                else -> {
                    ScanButton(
                        text = stringResource(R.string.nest_alignment_scan_donor),
                        onClick = onScanDonor,
                    )
                    if (state.errorTarget == NestAlignmentScanTarget.Donor) {
                        ErrorMessage()
                    }
                }
            }

            if (state.donor != null) {
                FlowArrow()

                SectionTitle(text = stringResource(R.string.nest_alignment_to))

                Spacer(Modifier.height(Spacing.sm))

                when {
                    state.recipient != null -> {
                        NestCard(
                            nest = state.recipient,
                            onClear = { onClearTarget(NestAlignmentScanTarget.Recipient) },
                        )

                        Spacer(Modifier.height(Spacing.md))

                        SectionTitle(text = stringResource(R.string.nest_alignment_count))

                        Spacer(Modifier.height(Spacing.sm))

                        TransferCounter(
                            donor = state.donor,
                            recipient = state.recipient,
                            count = state.transferCount,
                            onDecrease = { onTransferCountChange(state.transferCount - 1) },
                            onIncrease = { onTransferCountChange(state.transferCount + 1) },
                        )

                        Spacer(Modifier.height(Spacing.md))

                        Button(
                            onClick = onConfirm,
                            enabled = state.canConfirm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(Radii.md),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = actionButtonPrimaryLight,
                                contentColor = onActionButtonPrimaryLight,
                            ),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_accept_moving),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.nest_alignment_confirm),
                                modifier = Modifier.padding(start = Spacing.xs),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Spacer(Modifier.height(Spacing.sm))

                        OutlinedButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(Radii.md),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Text(text = stringResource(R.string.nest_alignment_restart))
                        }
                    }

                    state.isRecipientLoading -> LoadingNestCard()

                    else -> {
                        ScanButton(
                            text = stringResource(R.string.nest_alignment_scan_recipient),
                            onClick = onScanRecipient,
                        )
                        if (state.errorTarget == NestAlignmentScanTarget.Recipient) {
                            ErrorMessage()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .border(1.dp, InfoBorder, RoundedCornerShape(Radii.md))
            .background(InfoContainer, RoundedCornerShape(Radii.md))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_birth),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = InfoContent,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = InfoContent,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ScanButton(
    text: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(Radii.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = actionButtonPrimaryLight,
            contentColor = onActionButtonPrimaryLight,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_scan_label),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = Spacing.xs),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun LoadingNestCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(LoadingContainer, RoundedCornerShape(Radii.md))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radii.md))
            .padding(Spacing.md),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.nest_alignment_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
        )
    }
}

@Composable
private fun ErrorMessage() {
    Text(
        text = stringResource(R.string.nest_alignment_load_error),
        modifier = Modifier.padding(top = Spacing.sm),
        style = MaterialTheme.typography.bodySmall,
        color = ErrorText,
    )
}

@Composable
private fun NestCard(
    nest: NestAlignmentNest,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radii.md),
        border = BorderStroke(1.dp, actionButtonPrimaryLight),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nest.rfidCode,
                        style = MaterialTheme.typography.titleMedium,
                        color = actionButtonPrimaryLight,
                    )
                    Text(
                        text = nest.cageLabel,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                    )
                }
                Box {
                    Icon(
                        painter = painterResource(R.drawable.ic_birth),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 10.dp, end = 2.dp)
                            .size(28.dp),
                        tint = actionButtonPrimaryLight,
                    )
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.nest_alignment_clear_nest),
                            modifier = Modifier.size(16.dp),
                            tint = MutedText,
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardMutedContainer, RoundedCornerShape(Radii.md))
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.nest_alignment_rabbits_in_nest),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                )
                Text(
                    text = nest.rabbitsCount.toString(),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = actionButtonPrimaryLight,
                )
            }
        }
    }
}

@Composable
private fun FlowArrow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFF4F4F5), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = actionButtonPrimaryLight,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun TransferCounter(
    donor: NestAlignmentNest,
    recipient: NestAlignmentNest,
    count: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(Radii.md))
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(CounterContainer, RoundedCornerShape(Radii.md))
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CounterButton(
                text = "−",
                enabled = count > 1,
                onClick = onDecrease,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = actionButtonPrimaryLight,
                )
                Text(
                    text = stringResource(R.string.nest_alignment_move),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                )
            }
            CounterButton(
                text = "+",
                enabled = count < donor.rabbitsCount,
                onClick = onIncrease,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TransferSummaryCard(
                title = stringResource(R.string.nest_alignment_remaining),
                value = (donor.rabbitsCount - count).coerceAtLeast(0),
                subtitle = donor.cageShortLabel,
                containerColor = SummaryDangerContainer,
                borderColor = SummaryDangerBorder,
                contentColor = SummaryDangerText,
                modifier = Modifier.weight(1f),
            )
            TransferSummaryCard(
                title = stringResource(R.string.nest_alignment_will_be),
                value = recipient.rabbitsCount + count,
                subtitle = recipient.cageShortLabel,
                containerColor = SummarySuccessContainer,
                borderColor = SummarySuccessBorder,
                contentColor = SummarySuccessText,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CounterButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (enabled) actionButtonPrimaryLight else Color.White
    val contentColor = if (enabled) onActionButtonPrimaryLight else Color(0xFFB5BDC8)
    val borderColor = if (enabled) Color.Transparent else MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(Radii.md))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(Radii.md))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TransferSummaryCard(
    title: String,
    value: Int,
    subtitle: String,
    containerColor: Color,
    borderColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(76.dp)
            .background(containerColor, RoundedCornerShape(Radii.md))
            .border(1.dp, borderColor, RoundedCornerShape(Radii.md)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = contentColor,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NestAlignmentContentPreview() {
    ProfikrolTheme {
        NestAlignmentContent(
            state = NestAlignmentUiState(
                donor = NestAlignmentNest(
                    rfidCode = "RF-00123",
                    cageLabel = "Клетка A-08",
                    cageShortLabel = "A-08",
                    rabbitsCount = 8,
                ),
                recipient = NestAlignmentNest(
                    rfidCode = "RF-00089",
                    cageLabel = "Клетка Б-05",
                    cageShortLabel = "Б-05",
                    rabbitsCount = 4,
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onScanDonor = {},
            onScanRecipient = {},
            onTransferCountChange = {},
            onClearTarget = {},
            onConfirm = {},
            onRestart = {},
        )
    }
}
