package com.rabbitmes.mobile.shift.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.rabbitmes.mobile.shift.data.FakeWorkplaceRepository
import ru.profikrol.operator.uikit.theme.ProfikrolTheme

private val previewHeader = ShiftHeaderState(roleTitle = "Оператор", unreadNotifications = 2, pendingSyncEvents = 0)

private fun activeState(withTask: Boolean, overdue: Boolean = false): ShiftUiState.Active {
    val workshop = FakeWorkplaceRepository.fakeWorkshops(3)[1]
    val hangar = workshop.hangars[1]
    return ShiftUiState.Active(
        userName = "Тестовый",
        startedAtMillis = System.currentTimeMillis() - 83 * 60_000L,
        workshop = workshop,
        hangar = hangar,
        task = if (withTask) FakeWorkplaceRepository.fakeTask(hangar, overdue) else null,
    )
}

@Composable
private fun HomePreview(state: ShiftUiState) = ProfikrolTheme {
    ShiftHomeScreen(state, previewHeader, {}, {}, {}, {})
}

@Composable
private fun PickPreview(workshops: Int, selectFirst: Boolean) = ProfikrolTheme {
    val list = FakeWorkplaceRepository.fakeWorkshops(workshops)
    PickHangarScreen(
        state = ShiftUiState.PickHangar(list, selected = if (selectFirst) list.first().hangars.last() else null),
        unreadNotifications = 2,
        onBack = {},
        onNotificationsClick = {},
        onSelect = {},
        onConfirm = {},
    )
}

@Preview(name = "Смена не начата", showBackground = true, heightDp = 720)
@Composable
private fun IdlePreview() = HomePreview(ShiftUiState.Idle("Тестовый"))

@Preview(name = "Выбор ангара · 1 цех", showBackground = true, heightDp = 720)
@Composable
private fun PickOneWorkshopPreview() = PickPreview(workshops = 1, selectFirst = false)

@Preview(name = "Выбор ангара · 5 цехов, выбран", showBackground = true, heightDp = 720)
@Composable
private fun PickFiveWorkshopsPreview() = PickPreview(workshops = 5, selectFirst = true)

@Preview(name = "Смена идёт · ожидание задачи", showBackground = true, heightDp = 720)
@Composable
private fun ActiveWaitingPreview() = HomePreview(activeState(withTask = false))

@Preview(name = "Смена идёт · задача", showBackground = true, heightDp = 720)
@Composable
private fun ActiveWithTaskPreview() = HomePreview(activeState(withTask = true))

@Preview(name = "Смена идёт · просроченная задача", showBackground = true, heightDp = 720)
@Composable
private fun ActiveOverdueTaskPreview() = HomePreview(activeState(withTask = true, overdue = true))
