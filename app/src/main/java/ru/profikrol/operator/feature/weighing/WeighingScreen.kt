package ru.profikrol.operator.feature.weighing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.ActionButton
import ru.profikrol.operator.uikit.components.ActionButtonIcon
import ru.profikrol.operator.uikit.components.RabbitSummaryCard
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing
import ru.profikrol.operator.uikit.tokens.defaultPrimaryButtonHeight

@Composable
fun WeighingScreen(
    rfidCode: String,
    onBack: () -> Unit,
    onScanRfid: () -> Unit,
    viewModel: WeighingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    WeighingContent(
        rfidCode = rfidCode,
        state = state,
        onBack = onBack,
        onScanRfid = onScanRfid,
        onSaveWeight = viewModel::saveWeight,
    )
}

@Composable
private fun WeighingContent(
    rfidCode: String,
    state: WeighingUiState,
    onBack: () -> Unit,
    onScanRfid: () -> Unit,
    onSaveWeight: (rfidCode: String, weight: String) -> Unit,
) {
    var weight by remember { mutableStateOf("3.5") }

    Scaffold(
        topBar = {
            OperationTopBar(
                title = stringResource(R.string.weighing_title),
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
                .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        ) {
            OperationScanButton(
                text = stringResource(R.string.weighing_scan_rfid),
                onClick = onScanRfid,
            )

            Spacer(Modifier.height(Spacing.xl))

            RabbitSummaryCard(
                rfidCode = rfidCode,
                details = stringResource(R.string.weighing_rabbit_details),
            )

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = stringResource(R.string.weighing_weight_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.sm))

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(Spacing.lg))

            OperationSaveButton(
                text = stringResource(R.string.save),
                onClick = { onSaveWeight(rfidCode, weight.trim()) },
                enabled = weight.isNotBlank() && !state.isSaving,
            )

            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = stringResource(R.string.weighing_history_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.sm))

            HistoryRow(date = "20.03.2026", value = "3.2 кг")
            HistoryRow(date = "15.03.2026", value = "3.1 кг")
            HistoryRow(date = "10.03.2026", value = "3.0 кг")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OperationTopBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
internal fun OperationScanButton(
    text: String,
    onClick: () -> Unit,
) {
    ActionButton(
        text = text,
        icon = ActionButtonIcon.Scan,
        onClick = onClick,
    )
}

@Composable
internal fun OperationSaveButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(defaultPrimaryButtonHeight),
        shape = RoundedCornerShape(Radii.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = "✓", style = MaterialTheme.typography.titleMedium)
        Text(
            text = text,
            modifier = Modifier.padding(start = Spacing.sm),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun HistoryRow(
    date: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(Radii.md),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(Radii.md),
            )
            .padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    Spacer(Modifier.height(Spacing.xs))
}

@Preview(showBackground = true)
@Composable
private fun WeighingScreenPreview() {
    ProfikrolTheme {
        WeighingContent(
            rfidCode = "RF-00247",
            state = WeighingUiState(),
            onBack = {},
            onScanRfid = {},
            onSaveWeight = { _, _ -> },
        )
    }
}
