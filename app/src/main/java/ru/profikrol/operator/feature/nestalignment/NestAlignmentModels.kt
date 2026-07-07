package ru.profikrol.operator.feature.nestalignment

import ru.profikrol.operator.domain.model.Rabbit

enum class NestAlignmentScanTarget(val routeValue: String) {
    Donor("donor"),
    Recipient("recipient"),
    ;

    companion object {
        fun fromRouteValue(value: String?): NestAlignmentScanTarget? =
            entries.firstOrNull { it.routeValue == value }
    }
}

data class NestAlignmentUiState(
    val donor: NestAlignmentNest? = null,
    val recipient: NestAlignmentNest? = null,
    val transferCount: Int = 1,
    val loadingTarget: NestAlignmentScanTarget? = null,
    val errorTarget: NestAlignmentScanTarget? = null,
) {
    val isDonorLoading: Boolean
        get() = loadingTarget == NestAlignmentScanTarget.Donor

    val isRecipientLoading: Boolean
        get() = loadingTarget == NestAlignmentScanTarget.Recipient

    val canConfirm: Boolean
        get() = donor != null &&
            recipient != null &&
            transferCount > 0 &&
            transferCount <= donor.rabbitsCount
}

data class NestAlignmentMoveDraft(
    val donorRfidCode: String,
    val recipientRfidCode: String,
    val rabbitsCount: Int,
)

data class NestAlignmentNest(
    val rfidCode: String,
    val cageLabel: String,
    val cageShortLabel: String,
    val rabbitsCount: Int,
)

fun Rabbit.toNestAlignmentNest(): NestAlignmentNest {
    val normalizedCage = cage.trim().ifBlank { "—" }
    val cageLabel = if (normalizedCage.startsWith("Клетка", ignoreCase = true)) {
        normalizedCage
    } else {
        "Клетка $normalizedCage"
    }

    return NestAlignmentNest(
        rfidCode = rfidCode,
        cageLabel = cageLabel,
        cageShortLabel = normalizedCage.removePrefix("Клетка").trim().ifBlank { normalizedCage },
        rabbitsCount = rabbitsInNest ?: 0,
    )
}

fun NestAlignmentUiState.toMoveDraft(): NestAlignmentMoveDraft? {
    val donor = donor ?: return null
    val recipient = recipient ?: return null
    if (!canConfirm) return null

    return NestAlignmentMoveDraft(
        donorRfidCode = donor.rfidCode,
        recipientRfidCode = recipient.rfidCode,
        rabbitsCount = transferCount,
    )
}
