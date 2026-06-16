package ru.profikrol.operator.feature.rabbitculling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ru.profikrol.operator.domain.model.Rabbit
import ru.profikrol.operator.uikit.components.*
import ru.profikrol.operator.R


sealed interface InputMode {
    data object Scan : InputMode
    data object Manual : InputMode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RabbitCullingScreen(
    rabbit: Rabbit? = null,
    onBack: () -> Unit,
    onScanAgain: () -> Unit,
    viewModel: RabbitCullingViewModel = hiltViewModel(),
) {

    var inputMode by remember {
        mutableStateOf<InputMode>(InputMode.Scan)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cages by viewModel.cages.collectAsState()
    val reasons by viewModel.reasons.collectAsState()

    var selectedCage by remember { mutableStateOf<Cage?>(null) }
    var selectedReason by remember { mutableStateOf<CullingReason?>(null) }

    var quantity by remember { mutableStateOf("") }
    var cageExpanded by remember { mutableStateOf(false) }
    var reasonExpanded by remember { mutableStateOf(false) }

    val isManualValid = selectedCage != null && quantity.isNotBlank()
    val isScanValid = rabbit != null

    val canConfirm =
        selectedReason != null &&
                ((inputMode is InputMode.Scan && isScanValid) ||
                        (inputMode is InputMode.Manual && isManualValid))
    val quantityFocusRequester = remember { FocusRequester() }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Выбраковка",
                onBack = onBack,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                onClick = {
                    inputMode = InputMode.Scan
                    onScanAgain()
                }
            )

            StatusBanner(
                status = StatusBannerStatus.Warning,
                title = "Внимание!",
                text = "Это действие необратимо. Животное будет удалено из системы.",
            )
            ActionButton(
                text = if (inputMode is InputMode.Manual)
                    "Скрыть ручной ввод"
                else
                    "Ввести данные вручную",
                variant = ActionButtonVariant.Secondary,
                onClick = {
                    inputMode = InputMode.Manual
                }
            )
            if (inputMode is InputMode.Scan && rabbit != null) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = rabbit.rfidCode,
                            style = MaterialTheme.typography.titleLarge,
                        )

                        Text(
                            text = buildString {
                                append(rabbit.status)

                                if (!rabbit.age.isNullOrBlank()) {
                                    append(" • ${rabbit.age}")
                                }

                                if (!rabbit.cage.isNullOrBlank()) {
                                    append(" • Клетка ${rabbit.cage}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (inputMode is InputMode.Manual) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {

                        Box {
                            OutlinedTextField(
                                value = selectedCage?.name.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.cage)) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { cageExpanded = true }
                                    ) {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = cageExpanded
                                        )
                                    }
                                }
                            )

                            DropdownMenu(
                                expanded = cageExpanded,
                                onDismissRequest = { cageExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                cages.forEach { cage ->
                                    DropdownMenuItem(
                                        text = { Text(cage.name) },
                                        onClick = {
                                            selectedCage = cage
                                            cageExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = {
                                quantity = it.filter(Char::isDigit)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(quantityFocusRequester),
                            label = { Text(stringResource(R.string.count)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true,
                        )
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.manual_culling),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Text("${stringResource(R.string.cage)}: ${selectedCage?.name ?: "—"}")
                        Text("${stringResource(R.string.count)}: $quantity")
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Text(
                    text = stringResource(R.string.reason_for_culling),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Box {

                    OutlinedTextField(
                        value = selectedReason?.title.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(
                                onClick = { reasonExpanded = true }
                            ) {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = reasonExpanded
                                )
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = reasonExpanded,
                        onDismissRequest = { reasonExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        reasons.forEach { reason ->
                            DropdownMenuItem(
                                text = { Text(reason.title) },
                                onClick = {
                                    selectedReason = reason
                                    reasonExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = if (inputMode is InputMode.Scan)
                    stringResource(
                        R.string.confirm_the_culling_of_the_animal,
                        rabbit?.rfidCode.orEmpty(),
                    )
                else
                    stringResource(R.string.confirm_the_rejection_of_the_manual_input),
                style = MaterialTheme.typography.bodyMedium,
            )

            ActionButton(
                text = "Подтвердить выбраковку",
                icon = ActionButtonIcon.Warning,
                variant = ActionButtonVariant.Warning,
                enabled = canConfirm,
                onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Выбраковка подтверждена"
                        )
                    }
                },
            )

            ActionButton(
                text = "Отмена",
                variant = ActionButtonVariant.Secondary,
                onClick = onBack,
            )
        }
    }
}