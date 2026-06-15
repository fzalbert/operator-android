package ru.profikrol.operator.feature.rfidscanresult

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.profikrol.operator.R

/**
 * Идентификатор действия над кроликом.
 * Фабрики ролей оперируют только этим типом — никаких Android-зависимостей.
 */
enum class RabbitActionId {
    Weighing,
    Palpation,
    Moving,
    Insemination,
    ViewCard,
    Culling,
}

/**
 * UI-описание действия: id + ресурсы + флаги поведения.
 *
 * [requiresRabbit] — нужно ли загруженного кролика для активации кнопки.
 * Сейчас true только у Insemination (логика технолога), но конструкция
 * расширяемая: новые действия могут требовать кролика без правок экрана.
 */
sealed class RabbitAction(
    val id: RabbitActionId,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    data object Weighing : RabbitAction(
        id = RabbitActionId.Weighing,
        titleRes = R.string.weighing,
        iconRes = R.drawable.ic_weighing,
    )

    data object Palpation : RabbitAction(
        id = RabbitActionId.Palpation,
        titleRes = R.string.palpation,
        iconRes = R.drawable.ic_palpation,
    )

    data object Moving : RabbitAction(
        id = RabbitActionId.Moving,
        titleRes = R.string.moving,
        iconRes = R.drawable.ic_moving,
    )

    data object Insemination : RabbitAction(
        id = RabbitActionId.Insemination,
        titleRes = R.string.insemination,
        iconRes = R.drawable.ic_insemination_accept,
    )

    data object ViewCard : RabbitAction(
        id = RabbitActionId.ViewCard,
        titleRes = R.string.view_card,
        iconRes = R.drawable.ic_heart,
    )

    data object Culling : RabbitAction(
        id = RabbitActionId.Culling,
        titleRes = R.string.culling,
        iconRes = R.drawable.ic_cancel,
    )

    companion object {
        fun byId(id: RabbitActionId): RabbitAction = when (id) {
            RabbitActionId.Weighing -> Weighing
            RabbitActionId.Palpation -> Palpation
            RabbitActionId.Moving -> Moving
            RabbitActionId.Insemination -> Insemination
            RabbitActionId.ViewCard -> ViewCard
            RabbitActionId.Culling -> Culling
        }
    }
}
