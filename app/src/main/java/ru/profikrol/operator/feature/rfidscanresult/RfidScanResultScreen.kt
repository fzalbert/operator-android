package ru.profikrol.operator.feature.rfidscanresult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import ru.profikrol.operator.uikit.tokens.Spacing


@Composable
fun RfidScanResultScreen(
    rfidCode: String,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    onWeighing: (String) -> Unit,
    onMoving: (String) -> Unit,
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
        onWeighing = onWeighing,
        onMoving = onMoving,
    )
}

@Composable
private fun RfidScanResultContent(
    state: RfidScanResultUiState,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    onWeighing: (String) -> Unit,
    onMoving: (String) -> Unit,
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

            when {
                state.isError -> {
                    Text(
                        text = stringResource(R.string.rfid_scan_result_load_error),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }

                else -> {
                    RabbitInfoCard(
                        isLoading = state.isLoading,
                        rabbit = state.rabbit,
                    )
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
                actions = state.actions,
                rabbit = state.rabbit,
                onWeighing = onWeighing,
                onMoving = onMoving,
            )

            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun QuickActions(
    actions: List<RabbitAction>,
    rabbit: Rabbit?,
    onWeighing: (String) -> Unit,
    onMoving: (String) -> Unit,
) {
    var showInseminationDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        actions.forEach { action ->
            OutlinedButton(
                text = stringResource(action.titleRes),
                iconResId = action.iconRes,
                enabled = rabbit != null,
                onClick = {
                    when (action.id) {
                        RabbitActionId.Weighing -> rabbit?.rfidCode?.let(onWeighing)
                        RabbitActionId.Moving -> rabbit?.rfidCode?.let(onMoving)
                        RabbitActionId.Insemination -> showInseminationDialog = true
                        else -> Unit
                    }
                },
            )
        }
    }

    if (showInseminationDialog && rabbit != null) {
        InseminationConfirmationDialog(
            rabbit = rabbit,
            onDismiss = { showInseminationDialog = false },
            onConfirm = { showInseminationDialog = false },
        )
    }
}
