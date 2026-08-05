package com.example.ibtech.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateFacilityAdminInputUseCaseTest {

    @Test
    fun `blank name is BlankName`() {
        val result = ValidateFacilityAdminInputUseCase("  ", "1")

        assertEquals(FacilityAdminValidation.BlankName, result)
    }

    @Test
    fun `non-numeric floor is InvalidFloor`() {
        val result = ValidateFacilityAdminInputUseCase("어린이자료실", "일층")

        assertEquals(FacilityAdminValidation.InvalidFloor, result)
    }

    @Test
    fun `valid name and floor parses floor as Int`() {
        val result = ValidateFacilityAdminInputUseCase("어린이자료실", "2")

        assertEquals(FacilityAdminValidation.Valid(2), result)
    }
}
