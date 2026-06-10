package ru.profikrol.operator.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.MainTopBar
import ru.profikrol.operator.uikit.components.PrimaryButton
import ru.profikrol.operator.uikit.tokens.Spacing

@Composable
fun HomeScreen(
    onOpenNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onScanRfid: () -> Unit = {},
    onOpenNestPreparation: () -> Unit = {},
    onOpenNestAlignment: () -> Unit = {},
    onOpenRfidInstallation: () -> Unit = {},
    onOpenRabbitCulling: () -> Unit = {},
    lastRfidCode: String? = null,
    @Suppress("UNUSED_PARAMETER") viewModel: HomeViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            MainTopBar(
                title = stringResource(R.string.home_title),
                onNotificationsClick = onOpenNotifications,
                onSettingsClick = onOpenSettings,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Верхняя кнопка-акцент.
            PrimaryButton(
                text = stringResource(R.string.home_action_scan_rfid),
                onClick = onScanRfid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.lg),
            )

            if (lastRfidCode != null) {
                Text(stringResource(R.string.home_last_rfid, lastRfidCode))
            }

            // 4 одинаковые кнопки операций.
            FilledTonalButton(
                onClick = onOpenNestPreparation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_action_nest_preparation))
            }
            FilledTonalButton(
                onClick = onOpenNestAlignment,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_action_nest_alignment))
            }
            FilledTonalButton(
                onClick = onOpenRfidInstallation,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_action_rfid_installation))
            }
            FilledTonalButton(
                onClick = onOpenRabbitCulling,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_action_rabbit_culling))
            }
        }
    }
}
