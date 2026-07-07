package ru.profikrol.operator.feature.nestalignment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.profikrol.operator.domain.model.Rabbit

class NestAlignmentModelsTest {

    @Test
    fun `rabbit is mapped to nest alignment card model`() {
        val rabbit = Rabbit(
            rfidCode = "RF-00123",
            status = "Здорова",
            age = "8 мес",
            cage = "A-08",
            weight = "3.2 кг",
            diagnosis = "Здорова",
            rabbitsInNest = 8,
        )

        val nest = rabbit.toNestAlignmentNest()

        assertEquals("RF-00123", nest.rfidCode)
        assertEquals("Клетка A-08", nest.cageLabel)
        assertEquals("A-08", nest.cageShortLabel)
        assertEquals(8, nest.rabbitsCount)
    }

    @Test
    fun `valid state creates move draft for future repository call`() {
        val state = NestAlignmentUiState(
            donor = nest("RF-00123", rabbitsCount = 8),
            recipient = nest("RF-00089", rabbitsCount = 4),
            transferCount = 3,
        )

        val draft = state.toMoveDraft()

        assertEquals(
            NestAlignmentMoveDraft(
                donorRfidCode = "RF-00123",
                recipientRfidCode = "RF-00089",
                rabbitsCount = 3,
            ),
            draft,
        )
    }

    @Test
    fun `invalid transfer count does not create move draft`() {
        val state = NestAlignmentUiState(
            donor = nest("RF-00123", rabbitsCount = 2),
            recipient = nest("RF-00089", rabbitsCount = 4),
            transferCount = 3,
        )

        assertNull(state.toMoveDraft())
    }

    private fun nest(
        rfidCode: String,
        rabbitsCount: Int,
    ): NestAlignmentNest = NestAlignmentNest(
        rfidCode = rfidCode,
        cageLabel = "Клетка A-08",
        cageShortLabel = "A-08",
        rabbitsCount = rabbitsCount,
    )
}
