package ru.profikrol.operator.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.ActionButton
import ru.profikrol.operator.uikit.components.ActionButtonIcon
import ru.profikrol.operator.uikit.components.ActionCard
import ru.profikrol.operator.uikit.components.MainTopBar
import ru.profikrol.operator.uikit.theme.ProfikrolTheme
import ru.profikrol.operator.uikit.tokens.Spacing

/**
 * Контейнер: тащит VM и навигацию.
 * Из колбэка `onActionClick(id)` NavGraph сам решает, куда вести.
 */
@Composable
fun HomeScreen(
    onOpenNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onScanRfid: () -> Unit = {},
    onActionClick: (HomeActionId) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onOpenNotifications = onOpenNotifications,
        onOpenSettings = onOpenSettings,
        onScanRfid = onScanRfid,
        onActionClick = onActionClick,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onScanRfid: () -> Unit,
    onActionClick: (HomeActionId) -> Unit,
) {
    Scaffold(
        topBar = {
            MainTopBar(
                title = stringResource(R.string.home_title),
                subtitle = stringResource(R.string.home_subtitle),
                onNotificationsClick = onOpenNotifications,
                onSettingsClick = onOpenSettings,
                hasUnreadNotifications = state.hasUnreadNotifications,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
                .padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Кнопка сканирования — доступна всем (вне фабрики ролей).
            ActionButton(
                text = stringResource(R.string.home_action_scan_rfid),
                icon = ActionButtonIcon.Scan,
                onClick = onScanRfid,
            )

            // Карточки операций — формируются фабрикой роли.
            // Технологу прилетит пустой список → ничего не рисуется.
            state.actions.forEach { action ->
                ActionCard(
                    title = stringResource(action.titleRes),
                    description = stringResource(action.descriptionRes),
                    icon = painterResource(action.iconRes),
                    onClick = { onActionClick(action.id) },
                )
            }
        }
    }
}

// ---------- Previews ----------

@Preview(name = "Home — оператор (4 карточки)", showBackground = true)
@Composable
private fun HomeContentOperatorPreview() {
    ProfikrolTheme {
        HomeContent(
            state = HomeUiState(
                unreadNotificationsCount = 3,
                actions = listOf(
                    HomeAction.NestPreparation,
                    HomeAction.NestAlignment,
                    HomeAction.RfidInstallation,
                    HomeAction.RabbitCulling,
                ),
            ),
            onOpenNotifications = {},
            onOpenSettings = {},
            onScanRfid = {},
            onActionClick = {},
        )
    }
}

@Preview(name = "Home — технолог (без карточек)", showBackground = true)
@Composable
private fun HomeContentTechnologistPreview() {
    ProfikrolTheme {
        HomeContent(
            state = HomeUiState(actions = emptyList()),
            onOpenNotifications = {},
            onOpenSettings = {},
            onScanRfid = {},
            onActionClick = {},
        )
    }
}

@Preview(
    name = "Home — оператор, тёмная тема",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeContentDarkPreview() {
    ProfikrolTheme(darkTheme = true) {
        HomeContent(
            state = HomeUiState(
                unreadNotificationsCount = 1,
                actions = listOf(
                    HomeAction.NestPreparation,
                    HomeAction.NestAlignment,
                    HomeAction.RfidInstallation,
                    HomeAction.RabbitCulling,
                ),
            ),
            onOpenNotifications = {},
            onOpenSettings = {},
            onScanRfid = {},
            onActionClick = {},
        )
    }
}
