package ru.profikrol.operator.feature.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.profikrol.operator.R

/**
 * Идентификатор действия на главном экране.
 * Чистая enum — используется фабриками и в маппинге id → Route в NavGraph.
 */
enum class HomeActionId {
    NestPreparation,
    NestAlignment,
    RfidInstallation,
    RabbitCulling,
}

/**
 * UI-описание действия: id + ресурсы для отрисовки.
 * Sealed-объекты, чтобы каждое действие несло свою title/desc/icon.
 *
 * Лежит в feature/home, потому что использует R-ресурсы (Android-зависимость).
 * Фабрики ролей оперируют только [HomeActionId] и не зависят от этого файла.
 */
sealed class HomeAction(
    val id: HomeActionId,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
) {
    data object NestPreparation : HomeAction(
        id = HomeActionId.NestPreparation,
        titleRes = R.string.home_action_nest_preparation,
        descriptionRes = R.string.home_action_nest_preparation_desc,
        iconRes = R.drawable.ic_nest_preparation,
    )

    data object NestAlignment : HomeAction(
        id = HomeActionId.NestAlignment,
        titleRes = R.string.home_action_nest_alignment,
        descriptionRes = R.string.home_action_nest_alignment_desc,
        iconRes = R.drawable.ic_moving,
    )

    data object RfidInstallation : HomeAction(
        id = HomeActionId.RfidInstallation,
        titleRes = R.string.home_action_rfid_installation,
        descriptionRes = R.string.home_action_rfid_installation_desc,
        iconRes = R.drawable.ic_scan_label,
    )

    data object RabbitCulling : HomeAction(
        id = HomeActionId.RabbitCulling,
        titleRes = R.string.home_action_rabbit_culling,
        descriptionRes = R.string.home_action_rabbit_culling_desc,
        iconRes = R.drawable.ic_culling_of_rabbits,
    )

    companion object {
        /** Резолв id → UI-описание. Используется когда фабрика отдала только id. */
        fun byId(id: HomeActionId): HomeAction = when (id) {
            HomeActionId.NestPreparation -> NestPreparation
            HomeActionId.NestAlignment -> NestAlignment
            HomeActionId.RfidInstallation -> RfidInstallation
            HomeActionId.RabbitCulling -> RabbitCulling
        }
    }
}
