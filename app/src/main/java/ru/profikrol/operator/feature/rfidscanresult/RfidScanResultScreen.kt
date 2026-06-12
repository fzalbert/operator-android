package ru.profikrol.operator.feature.rfidscanresult

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.profikrol.operator.R
import ru.profikrol.operator.domain.model.Rabbit
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.components.OutlinedButton
import ru.profikrol.operator.uikit.components.OutlinedButtonVariant
import ru.profikrol.operator.uikit.tokens.Alpha
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing
import ru.profikrol.operator.uikit.tokens.cardElevation
import ru.profikrol.operator.uikit.tokens.defaultBorderWidth
import ru.profikrol.operator.uikit.tokens.defaultInfoRowHeight

@Composable
fun RfidScanResultScreen(
    rfidCode: String,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    viewModel: RfidScanResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(rfidCode) {
        viewModel.loadRabbit(rfidCode)
    }

    RfidScanResultContent(
        state = state,
        onBack = onBack,
        onScanAgain = onScanAgain,
    )
}

@Composable
private fun RfidScanResultContent(
    state: RfidScanResultUiState,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.rfid_scan_result_title),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl),
        ) {
            Spacer(Modifier.height(Spacing.xl))

            OutlinedButton(
                text = stringResource(R.string.rfid_scan_result_scan_button),
                iconResId = R.drawable.ic_scan_label,
                variant = OutlinedButtonVariant.Filled,
                centerContent = true,
                onClick = onScanAgain,
            )

            Spacer(Modifier.height(Spacing.xxl))

            when {
                state.isLoading -> {
                    Text(
                        text = stringResource(R.string.rfid_scan_result_loading),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                state.isError || state.rabbit == null -> {
                    Text(
                        text = stringResource(R.string.rfid_scan_result_load_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                else -> {
                    RabbitInfoCard(rabbit = state.rabbit)
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = stringResource(R.string.rfid_scan_result_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.lg))

            QuickActions(
                rabbit = state.rabbit,
            )

            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun RabbitInfoCard(
    rabbit: Rabbit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radii.md),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(defaultBorderWidth, MaterialTheme.colorScheme.primary),
        shadowElevation = cardElevation,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = Spacing.xl,
                vertical = Spacing.lg,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = rabbit.rfidCode,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                StatusChip(text = rabbit.status)
            }

            Spacer(Modifier.height(Spacing.lg))

            RabbitInfoRow(
                label = stringResource(R.string.rfid_scan_result_age_label),
                value = rabbit.age,
            )
            RabbitInfoRow(
                label = stringResource(R.string.rfid_scan_result_cage_label),
                value = rabbit.cage,
            )
            RabbitInfoRow(
                label = stringResource(R.string.rfid_scan_result_weight_label),
                value = rabbit.weight,
            )
            RabbitInfoRow(
                label = stringResource(R.string.rfid_scan_result_diagnosis_label),
                value = rabbit.diagnosis,
                showDivider = false,
            )
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(Radii.xl))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun RabbitInfoRow(
    label: String,
    value: String,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(defaultInfoRowHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Alpha.divider))
        }
    }
}

@Composable
private fun QuickActions(
    rabbit: Rabbit?,
) {
    var showInseminationDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        OutlinedButton(
            text = stringResource(R.string.weighing),
            iconResId = R.drawable.ic_weighing,
            onClick = {},
        )
        OutlinedButton(
            text = stringResource(R.string.palpation),
            iconResId = R.drawable.ic_palpation,
            onClick = {},
        )
        OutlinedButton(
            text = stringResource(R.string.moving),
            iconResId = R.drawable.ic_moving,
            onClick = {},
        )
        OutlinedButton(
            text = stringResource(R.string.insemination),
            iconResId = R.drawable.ic_insemination_accept,
            enabled = rabbit != null,
            onClick = { showInseminationDialog = true },
        )
        OutlinedButton(
            text = stringResource(R.string.view_card),
            iconResId = R.drawable.ic_heart,
            onClick = {},
        )
        OutlinedButton(
            text = stringResource(R.string.culling),
            iconResId = R.drawable.ic_cancel,
            onClick = {},
        )
    }

    if (showInseminationDialog && rabbit != null) {
        InseminationConfirmationDialog(
            rabbit = rabbit,
            onDismiss = { showInseminationDialog = false },
            onConfirm = { showInseminationDialog = false },
        )
    }
}
