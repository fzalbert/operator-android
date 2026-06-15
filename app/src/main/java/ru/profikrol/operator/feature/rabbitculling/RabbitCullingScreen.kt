package ru.profikrol.operator.feature.rabbitculling

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.profikrol.operator.uikit.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RabbitCullingScreen(
    onBack: () -> Unit,
    viewModel: RabbitCullingViewModel = hiltViewModel(),
) {
    val cages by viewModel.cages.collectAsState()
    val reasons by viewModel.reasons.collectAsState()

    var isManualInputExpanded by remember { mutableStateOf(false) }

    var selectedCage by remember { mutableStateOf<Cage?>(null) }
    var selectedReason by remember { mutableStateOf<CullingReason?>(null) }

    var quantity by remember { mutableStateOf("1") }

    var cageExpanded by remember { mutableStateOf(false) }
    var reasonExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Выбраковка",
                onBack = onBack,
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            ActionButton(
                text = "Сканировать RFID-метку",
                icon = ActionButtonIcon.Scan,
                onClick = {},
            )

            StatusBanner(
                status = StatusBannerStatus.Warning,
                title = "Внимание!",
                text = "Это действие необратимо. Животное будет удалено из системы.",
            )

            ActionButton(
                text = if (isManualInputExpanded) "Скрыть ручной ввод"
                else "Ввести данные вручную",
                variant = ActionButtonVariant.Secondary,
                onClick = { isManualInputExpanded = !isManualInputExpanded },
            )

            if (isManualInputExpanded) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {

                        // ---------------- CAGE DROPDOWN ----------------
                        Box {
                            OutlinedTextField(
                                value = selectedCage?.name.orEmpty(),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                label = { Text("Клетка") },
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
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Количество") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true,
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "DEMO001",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Text(
                        text = "Самка • 8 мес • Клетка A-12",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // ---------------- REASON DROPDOWN ----------------
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Text(
                    text = "Причина выбраковки",
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
                        modifier = Modifier.fillMaxWidth(),
                        offset = DpOffset(0.dp, 4.dp)
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
                text = "Я подтверждаю выбраковку животного DEMO001",
                style = MaterialTheme.typography.bodyMedium,
            )

            ActionButton(
                text = "Подтвердить выбраковку",
                icon = ActionButtonIcon.Warning,
                variant = ActionButtonVariant.Warning,
                enabled = selectedReason != null,
                onClick = {
                    // viewModel.confirmCulling(...)
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