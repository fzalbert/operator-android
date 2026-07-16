package ru.profikrol.operator.feature.rfidinstallation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.ActionButton
import ru.profikrol.operator.uikit.components.ActionButtonIcon
import ru.profikrol.operator.uikit.components.ActionButtonVariant
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.components.StatusBanner
import ru.profikrol.operator.uikit.components.StatusBannerStatus
import ru.profikrol.operator.uikit.tokens.Spacing

@Composable
fun RfidInstallationScreen(
    scannedRfidResult: String? = null,
    onScanRfid: () -> Unit,
    onBack: () -> Unit,
    viewModel: RfidInstallationViewModel = hiltViewModel()
) {
    LaunchedEffect(scannedRfidResult) {
        scannedRfidResult
            ?.takeIf(String::isNotBlank)
            ?.let(viewModel::setRfid)
    }

    val scannedRfid by viewModel.scannedRfid.collectAsStateWithLifecycle()

    var hangar by rememberSaveable { mutableStateOf("") }
    var breed by rememberSaveable { mutableStateOf("") }
    var installed by rememberSaveable { mutableStateOf(false) }

    val canRegister =
        !scannedRfid.isNullOrBlank() &&
                hangar.isNotBlank() &&
                breed.isNotBlank()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.rfid_installation_title),
                onBack = onBack,
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {

            ActionButton(
                text = "Сканировать RFID-метку",
                icon = ActionButtonIcon.Scan,
                onClick = onScanRfid,
            )

            if (scannedRfid.isNullOrBlank()) {
                StatusBanner(
                    status = StatusBannerStatus.Info,
                    text = "Сначала отсканируйте RFID-метку для регистрации нового кролика.",
                    iconSize = 24.dp
                )
            } else {
                ScannedRfidCard(rfidCode = scannedRfid.orEmpty())
            }

            Text(
                text = stringResource(R.string.location),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = hangar,
                onValueChange = { hangar = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.hangar)) },
                singleLine = true,
            )

            Text(
                text = stringResource(R.string.rabbit_information),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.breed)) },
                singleLine = true,
            )

            ActionButton(
                text = if (installed)
                    stringResource(R.string.tag_installed)
                else
                    stringResource(R.string.register),
                icon = if (installed)
                    ActionButtonIcon.CheckCircle
                else
                    ActionButtonIcon.Check,
                variant = ActionButtonVariant.Success,
                enabled = installed || canRegister,
                onClick = { installed = true }
            )

            ActionButton(
                text = stringResource(R.string.cancel),
                variant = ActionButtonVariant.Secondary,
                onClick = onBack,
            )
        }
    }
}

@Composable
private fun ScannedRfidCard(
    rfidCode: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = "RFID-метка считана",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )

            Text(
                text = rfidCode,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = "Номер закрепится за кроликом после регистрации.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
