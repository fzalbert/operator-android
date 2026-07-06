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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.ActionButton
import ru.profikrol.operator.uikit.components.ActionButtonIcon
import ru.profikrol.operator.uikit.components.ActionButtonVariant
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.components.StatusBanner
import ru.profikrol.operator.uikit.components.StatusBannerStatus

@Composable
fun RfidInstallationScreen(
    onScanRfid: () -> Unit,
    onBack: () -> Unit,
    viewModel: RfidInstallationViewModel = hiltViewModel()
) {

    val scannedRfid by viewModel.scannedRfid.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            ActionButton(
                text = "Сканировать RFID-метку",
                icon = ActionButtonIcon.Scan,
                onClick = onScanRfid,
            )
            scannedRfid?.let { code ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = code,
                            style = MaterialTheme.typography.titleLarge,
                        )

                        Text(
                            text = "RFID-метка считана",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Text("Расположение")

            OutlinedTextField(
                value = hangar,
                onValueChange = { hangar = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ангар") },
                singleLine = true,
            )

            Text("Информация о кролике")

            OutlinedTextField(
                value = breed,
                onValueChange = { breed = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Порода") },
                singleLine = true,
            )

            if (scannedRfid == null) {
                StatusBanner(
                    status = StatusBannerStatus.Warning,
                    text = "Сначала отсканируйте RFID-метку"
                )
            }

            ActionButton(
                text = if (installed) "Метка установлена!" else "Зарегистрировать",
                icon = if (installed)
                    ActionButtonIcon.CheckCircle
                else
                    ActionButtonIcon.Check,
                variant = ActionButtonVariant.Success,
                enabled = installed || canRegister,
                onClick = { installed = true }
            )

            ActionButton(
                text = "Отмена",
                variant = ActionButtonVariant.Secondary,
                onClick = onBack,
            )
        }
    }
}