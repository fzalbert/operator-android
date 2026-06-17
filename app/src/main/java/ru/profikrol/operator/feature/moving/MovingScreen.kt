package ru.profikrol.operator.feature.moving

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R
import ru.profikrol.operator.feature.weighing.OperationRabbitCard
import ru.profikrol.operator.feature.weighing.OperationSaveButton
import ru.profikrol.operator.feature.weighing.OperationScanButton
import ru.profikrol.operator.feature.weighing.OperationTopBar
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing

@Composable
fun MovingScreen(
    rfidCode: String,
    onBack: () -> Unit,
    onScanRfid: () -> Unit,
) {
    var selectedAction by remember { mutableStateOf<MovingAction?>(null) }

    Scaffold(
        topBar = {
            OperationTopBar(
                title = stringResource(R.string.moving_title),
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
                text = stringResource(R.string.moving_scan_rfid),
                onClick = onScanRfid,
            )

            Spacer(Modifier.height(Spacing.xl))

            OperationRabbitCard(
                rfidCode = rfidCode,
                details = stringResource(R.string.moving_rabbit_details),
            )

            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = stringResource(R.string.moving_action_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.sm))

            MovingActionRow(
                action = MovingAction.Up,
                selected = selectedAction == MovingAction.Up,
                onClick = { selectedAction = MovingAction.Up },
            )

            Spacer(Modifier.height(Spacing.sm))

            MovingActionRow(
                action = MovingAction.Down,
                selected = selectedAction == MovingAction.Down,
                onClick = { selectedAction = MovingAction.Down },
            )

            Spacer(Modifier.height(Spacing.sm))

            MovingActionRow(
                action = MovingAction.AnotherHangar,
                selected = selectedAction == MovingAction.AnotherHangar,
                onClick = { selectedAction = MovingAction.AnotherHangar },
            )

            Spacer(Modifier.height(Spacing.lg))

            OperationSaveButton(
                text = stringResource(R.string.confirm),
                onClick = {},
                enabled = selectedAction != null,
            )
        }
    }
}

private enum class MovingAction {
    Up,
    Down,
    AnotherHangar,
}

@Composable
private fun MovingActionRow(
    action: MovingAction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (selected) 2.dp else 1.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(Radii.md),
            )
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(Radii.md),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(
                when (action) {
                    MovingAction.Up -> R.drawable.ic_arrow_up
                    MovingAction.Down -> R.drawable.ic_arrow_down
                    MovingAction.AnotherHangar -> R.drawable.ic_hangar
                },
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = when (action) {
                MovingAction.Up -> stringResource(R.string.moving_action_up)
                MovingAction.Down -> stringResource(R.string.moving_action_down)
                MovingAction.AnotherHangar -> stringResource(R.string.moving_action_another_hangar)
            },
            modifier = Modifier.padding(start = Spacing.sm),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MovingScreenPreview() {
    ProfikrolTheme {
        MovingScreen(
            rfidCode = "RF-00247",
            onBack = {},
            onScanRfid = {},
        )
    }
}
