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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.OutlinedButton
import ru.profikrol.operator.uikit.components.OutlinedButtonVariant
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.theme.actionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.onActionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.primaryContainerLight
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing

private val ResultScreenHorizontalPadding = 24.dp
private val RabbitInfoCardElevation = 6.dp
private val RabbitInfoRowHeight = 48.dp
private val StatusChipShape = RoundedCornerShape(24.dp)

@Composable
fun RfidScanResultScreen(
    rfidCode: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            RfidScanResultTopBar(onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ResultScreenHorizontalPadding),
        ) {
            Spacer(Modifier.height(Spacing.xl))

            OutlinedButton(
                text = "Сканировать RFID-метку",
                iconResId = R.drawable.ic_scan_label,
                variant = OutlinedButtonVariant.Filled,
                centerContent = true,
                onClick = {},
            )

            Spacer(Modifier.height(36.dp))

            RabbitInfoCard(rfidCode = rfidCode)

            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = "Быстрые действия",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.lg))

            QuickActions()

            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RfidScanResultTopBar(
    onBack: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.rfid_scan_result_title),
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
            containerColor = actionButtonPrimaryLight,
            navigationIconContentColor = onActionButtonPrimaryLight,
            titleContentColor = onActionButtonPrimaryLight,
        ),
    )
}

@Composable
private fun RabbitInfoCard(
    rfidCode: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radii.md),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, actionButtonPrimaryLight),
        shadowElevation = RabbitInfoCardElevation,
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
                    text = rfidCode,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = actionButtonPrimaryLight,
                )
                StatusChip(text = "Здорова")
            }

            Spacer(Modifier.height(Spacing.lg))

            RabbitInfoRow(label = "Возраст", value = "8 мес")
            RabbitInfoRow(label = "Клетка", value = "A-12")
            RabbitInfoRow(label = "Вес", value = "3.2 кг")
            RabbitInfoRow(label = "Диагноз", value = "Здорова", showDivider = false)
        }
    }
}

@Composable
private fun StatusChip(
    text: String,
) {
    Box(
        modifier = Modifier
            .background(primaryContainerLight, StatusChipShape)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = actionButtonPrimaryLight,
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
                .height(RabbitInfoRowHeight),
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun QuickActions() {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        OutlinedButton(
            text = stringResource(R.string.weighing),
            iconResId = R.drawable.ic_weighing,
            onClick = {},
        )
        OutlinedButton(
            text = "Пальпация",
            iconResId = R.drawable.ic_palpation,
            onClick = {},
        )
        OutlinedButton(
            text = "Перемещение",
            iconResId = R.drawable.ic_moving,
            onClick = {},
        )
        OutlinedButton(
            text = "Осеменение",
            iconResId = R.drawable.ic_insemination_accept,
            onClick = {},
        )
        OutlinedButton(
            text = "Просмотр карточки",
            iconResId = R.drawable.ic_heart,
            onClick = {},
        )
        OutlinedButton(
            text = "Выбраковка",
            iconResId = R.drawable.ic_cancel,
            onClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun RfidScanResultScreenPreview() {
    ProfikrolTheme {
        RfidScanResultScreen(
            rfidCode = "RF-00247",
            onBack = {},
        )
    }
}
