package ru.profikrol.operator.feature.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ru.profikrol.operator.R
import ru.profikrol.operator.uikit.components.AppTopBar

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: NotificationsViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.notifications_title),
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.notifications_empty))
        }
    }
}
