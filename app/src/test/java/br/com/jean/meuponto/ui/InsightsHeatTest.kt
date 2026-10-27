package br.com.jean.meuponto.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** Nível de preenchimento da grade do mês ([dayHeatLevel]): regra pura, sem Compose. */
class InsightsHeatTest {

    @Test
    fun zeroMinutesIsNoneWithOrWithoutTarget() {
        assertEquals(HeatLevel.NONE, dayHeatLevel(0, 480))
        assertEquals(HeatLevel.NONE, dayHeatLevel(0, null))
    }

    @Test
    fun belowHalfTargetIsLow() {
        assertEquals(HeatLevel.LOW, dayHeatLevel(100, 480))
        assertEquals(HeatLevel.LOW, dayHeatLevel(239, 480))
    }

    @Test
    fun fromHalfTargetUpToTargetIsMedium() {
        assertEquals(HeatLevel.MEDIUM, dayHeatLevel(240, 480))
        assertEquals(HeatLevel.MEDIUM, dayHeatLevel(300, 480))
        assertEquals(HeatLevel.MEDIUM, dayHeatLevel(479, 480))
    }

    @Test
    fun exactlyTargetIsFull() {
        assertEquals(HeatLevel.FULL, dayHeatLevel(480, 480))
    }

    @Test
    fun aboveTargetIsFull() {
        assertEquals(HeatLevel.FULL, dayHeatLevel(600, 480))
    }

    @Test
    fun nullTargetWithWorkedMinutesIsLowNotFull() {
        assertEquals(HeatLevel.LOW, dayHeatLevel(480, null))
        assertEquals(HeatLevel.LOW, dayHeatLevel(1, null))
    }

    @Test
    fun nonPositiveTargetIsTreatedAsNoTarget() {
        assertEquals(HeatLevel.LOW, dayHeatLevel(480, 0))
        assertEquals(HeatLevel.NONE, dayHeatLevel(0, 0))
    }
}
