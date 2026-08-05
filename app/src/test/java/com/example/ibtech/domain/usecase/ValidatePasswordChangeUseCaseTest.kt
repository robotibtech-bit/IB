package com.example.ibtech.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidatePasswordChangeUseCaseTest {

    @Test
    fun `password shorter than minimum length is TooShort`() {
        val result = ValidatePasswordChangeUseCase("123", "123")

        assertEquals(PasswordChangeValidation.TooShort, result)
    }

    @Test
    fun `mismatched confirmation is Mismatch`() {
        val result = ValidatePasswordChangeUseCase("1234", "5678")

        assertEquals(PasswordChangeValidation.Mismatch, result)
    }

    @Test
    fun `matching password of sufficient length is Valid`() {
        val result = ValidatePasswordChangeUseCase("1234", "1234")

        assertEquals(PasswordChangeValidation.Valid, result)
    }
}
