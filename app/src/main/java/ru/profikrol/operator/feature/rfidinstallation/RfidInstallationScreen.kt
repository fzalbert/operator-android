package ru.profikrol.operator.feature.rfidinstallation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.PlaceholderScreen

@Composable
fun RfidInstallationScreen(onBack: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.rfid_installation_title),
        onBack = onBack,
    )
}
