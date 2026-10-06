package com.rabbitmes.mobile.shift.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rabbitmes.mobile.shift.model.AssignedTask
import com.rabbitmes.mobile.shift.model.Hangar
import ru.profikrol.operator.R

/** Вкладка «Смена»: смена не начата или идёт. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftHomeScreen(
    state: ShiftUiState,
    header: ShiftHeaderState,
    onNotificationsClick: () -> Unit,
    onStartShift: () -> Unit,
    onCloseShift: () -> Unit,
    onStartTask: (AssignedTask) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { ShiftTopAppBar(header, onNotificationsClick, scrollBehavior) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when (state) {
                is ShiftUiState.Idle -> {
                    item { StartShiftCard(state.userName, onStartShift) }
                    item { ShiftStepsSection() }
                }
                is ShiftUiState.Active -> {
                    item {
                        ActiveShiftCard(state.userName, state.startedAtMillis, state.workshop, state.hangar, onCloseShift)
                    }
                    item { TaskSection(state.task, onStartTask) }
                }
                // Выбор ангара — отдельный экран; вкладка в это время показывает прежнее состояние.
                is ShiftUiState.PickHangar -> Unit
            }
        }
    }
}

/** Выбор ангара: отдельный экран без нижней навигации. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickHangarScreen(
    state: ShiftUiState.PickHangar,
    unreadNotifications: Int,
    onBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSelect: (Hangar) -> Unit,
    onConfirm: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { PickHangarTopAppBar(unreadNotifications, onBack, onNotificationsClick, scrollBehavior) },
        bottomBar = {
            Box(Modifier.navigationBarsPadding().padding(16.dp)) {
                SolidButton(
                    text = stringResource(R.string.shift_pick_confirm),
                    onClick = onConfirm,
                    enabled = state.selected != null,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.shift_pick_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { WorkshopList(state.workshops, state.selected, onSelect) }
        }
    }
}
