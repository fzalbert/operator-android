package ru.profikrol.operator.feature.rabbitprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.AppTopBar
import ru.profikrol.operator.uikit.components.ActionButton
import ru.profikrol.operator.uikit.components.ActionButtonIcon
import ru.profikrol.operator.uikit.components.ActionButtonVariant
import ru.profikrol.operator.uikit.tokens.Radii
import ru.profikrol.operator.uikit.tokens.Spacing

@Composable
fun RabbitProfileScreen(
    rfidCode: String,
    onBack: () -> Unit,
    onWeighing: (String) -> Unit,
    onMoving: (String) -> Unit,
    onCulling: (RabbitProfileUiModel) -> Unit,
    viewModel: RabbitProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(rfidCode) {
        viewModel.loadProfile(rfidCode)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.rabbit_profile_title),
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.isError -> Text(
                    text = stringResource(R.string.rabbit_profile_load_error),
                    color = MaterialTheme.colorScheme.error,
                )
                state.profile != null -> RabbitProfileContent(
                    profile = requireNotNull(state.profile),
                    onWeighing = { onWeighing(rfidCode) },
                    onMoving = { onMoving(rfidCode) },
                    onCulling = onCulling,
                )
            }
        }
    }
}

@Composable
private fun RabbitProfileContent(
    profile: RabbitProfileUiModel,
    onWeighing: () -> Unit,
    onMoving: () -> Unit,
    onCulling: (RabbitProfileUiModel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = profile.code,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            profile.status?.let { status ->
                Text(
                    text = status,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(Radii.xl),
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(Radii.md),
                )
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Классификация",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = profile.classification,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        profile.sections.forEach { section ->
            RabbitProfileSectionCard(section = section)
        }

        RabbitProfileActions(
            onWeighing = onWeighing,
            onMoving = onMoving,
            onCulling = { onCulling(profile) },
        )

        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun RabbitProfileActions(
    onWeighing: () -> Unit,
    onMoving: () -> Unit,
    onCulling: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.rabbit_profile_actions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        ActionButton(
            text = stringResource(R.string.weighing),
            icon = ActionButtonIcon.Scan,
            onClick = onWeighing,
        )
        ActionButton(
            text = stringResource(R.string.moving),
            variant = ActionButtonVariant.Secondary,
            onClick = onMoving,
        )
        ActionButton(
            text = stringResource(R.string.culling),
            icon = ActionButtonIcon.Warning,
            variant = ActionButtonVariant.Warning,
            onClick = onCulling,
        )
    }
}
