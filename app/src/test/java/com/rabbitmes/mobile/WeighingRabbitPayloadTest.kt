package com.rabbitmes.mobile

import com.rabbitmes.mobile.data.reference.OperationDefinitions
import com.rabbitmes.mobile.domain.operations.parseWeighingRabbitWeights
import com.rabbitmes.mobile.domain.OperationType
import org.junit.Assert.assertEquals
import org.junit.Test

class WeighingRabbitPayloadTest {
    @Test
    fun `parses all entered rabbit weights for one cage target`() {
        assertEquals(
            listOf(1800, 1950, 2100),
            parseWeighingRabbitWeights("1800, 1950,2100"),
        )
    }

    @Test
    fun `ignores empty and non-positive weight values`() {
        assertEquals(
            listOf(1800),
            parseWeighingRabbitWeights("0,,abc,-10,1800"),
        )
    }

    @Test
    fun `cage weighing uses backend weightGrams field`() {
        val definition = OperationDefinitions.of(OperationType.WEIGHING_CAGE)

        assertEquals("weightGrams", definition.fields.first().id)
    }
}
