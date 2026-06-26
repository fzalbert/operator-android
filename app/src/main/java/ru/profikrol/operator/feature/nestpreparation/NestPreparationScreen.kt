package ru.profikrol.operator.feature.nestpreparation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.theme.actionButtonMutedSuccessLight
import ru.profikrol.operator.uikit.theme.actionButtonPrimaryLight
import ru.profikrol.operator.uikit.theme.onActionButtonMutedSuccessLight
import ru.profikrol.operator.uikit.theme.onActionButtonPrimaryLight
import ru.profikrol.operator.uikit.tokens.Spacing

private val FieldHeight = 56.dp
private val ActionHeight = 66.dp
private val SelectedCageContainer = Color(0xFFF0F7F0)
private val DisabledActionContainer = Color(0xFFF3F4F6)
private val DisabledActionContent = Color(0xFF99A1AF)

@Composable
fun NestPreparationScreen(
    onBack: () -> Unit,
    viewModel: NestPreparationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val requestFailedMessage = stringResource(R.string.nest_preparation_request_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                NestPreparationEvent.Finished -> onBack()
                NestPreparationEvent.RequestFailed -> {
                    snackbarHostState.showSnackbar(requestFailedMessage)
                }
            }
        }
    }

    NestPreparationContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onRowChange = viewModel::onRowChange,
        onCageChange = viewModel::onCageChange,
        onAddSawdust = viewModel::onAddSawdustClick,
        onInstallNest = viewModel::onInstallNestClick,
        onFinish = viewModel::onFinishClick,
        onBack = onBack,
    )
}

@Composable
private fun NestPreparationContent(
    state: NestPreparationUiState,
    snackbarHostState: SnackbarHostState,
    onRowChange: (String) -> Unit,
    onCageChange: (String) -> Unit,
    onAddSawdust: () -> Unit,
    onInstallNest: () -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.nest_preparation_title),
                onBack = onBack,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.nest_preparation_cage_selection),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.nest_preparation_cage_selection_hint),
                modifier = Modifier.padding(top = Spacing.xs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                CageField(
                    label = stringResource(R.string.nest_preparation_row),
                    value = state.row,
                    onValueChange = onRowChange,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Right) },
                    ),
                )

                CageField(
                    label = stringResource(R.string.nest_preparation_cage),
                    value = state.cage,
                    onValueChange = onCageChange,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                )
            }

            Spacer(Modifier.height(Spacing.lg))

            if (state.isCageSelected) {
                SelectedCageCard(
                    cageName = state.selectedCageName,
                )
                Spacer(Modifier.height(Spacing.lg))
            }

            PreparationActionButton(
                text = stringResource(R.string.nest_preparation_add_sawdust),
                iconRes = R.drawable.ic_nest_preparation,
                enabled = state.isCageSelected &&
                    !state.isSawdustAdded &&
                    !state.isLoading,
                completed = state.isSawdustAdded,
                onClick = onAddSawdust,
            )

            Spacer(Modifier.height(Spacing.md))

            PreparationActionButton(
                text = stringResource(R.string.nest_preparation_install_nest),
                iconRes = R.drawable.ic_cube,
                enabled = state.isCageSelected &&
                    !state.isNestInstalled &&
                    !state.isLoading,
                completed = state.isNestInstalled,
                onClick = onInstallNest,
            )

            Spacer(Modifier.height(Spacing.lg))

            FinishButton(
                enabled = state.canFinish,
                onClick = onFinish,
            )
        }
    }
}

@Composable
private fun CageField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(FieldHeight),
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = actionButtonPrimaryLight,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

@Composable
private fun SelectedCageCard(cageName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = SelectedCageContainer),
        border = BorderStroke(1.dp, actionButtonPrimaryLight),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nest_preparation),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = actionButtonPrimaryLight,
            )
            Column {
                Text(
                    text = stringResource(R.string.nest_preparation_selected_cage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = cageName,
                    style = MaterialTheme.typography.titleMedium,
                    color = actionButtonPrimaryLight,
                )
            }
        }
    }
}

@Composable
private fun PreparationActionButton(
    text: String,
    iconRes: Int,
    enabled: Boolean,
    completed: Boolean,
    onClick: () -> Unit,
) {
    val isActive = enabled || completed
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionHeight),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = actionButtonPrimaryLight,
            contentColor = onActionButtonPrimaryLight,
            disabledContainerColor = if (completed) {
                actionButtonPrimaryLight
            } else {
                DisabledActionContainer
            },
            disabledContentColor = if (completed) {
                onActionButtonPrimaryLight
            } else {
                DisabledActionContent
            },
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isActive) 4.dp else 0.dp,
            disabledElevation = if (completed) 4.dp else 0.dp,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            if (completed) {
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.ic_accept_button),
                    contentDescription = stringResource(
                        R.string.nest_preparation_action_completed,
                    ),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun FinishButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = actionButtonPrimaryLight,
            contentColor = onActionButtonPrimaryLight,
            disabledContainerColor = actionButtonMutedSuccessLight,
            disabledContentColor = onActionButtonMutedSuccessLight,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_accept_button),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = stringResource(R.string.nest_preparation_finish),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NestPreparationEmptyPreview() {
    ProfikrolTheme {
        NestPreparationContent(
            state = NestPreparationUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onRowChange = {},
            onCageChange = {},
            onAddSawdust = {},
            onInstallNest = {},
            onFinish = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun NestPreparationSelectedPreview() {
    ProfikrolTheme {
        NestPreparationContent(
            state = NestPreparationUiState(
                row = "А",
                cage = "3",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onRowChange = {},
            onCageChange = {},
            onAddSawdust = {},
            onInstallNest = {},
            onFinish = {},
            onBack = {},
        )
    }
}
