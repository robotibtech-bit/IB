package com.example.ibtech.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `same input hashes to the same value`() {
        assertEquals(PasswordHasher.hash("0000"), PasswordHasher.hash("0000"))
    }

    @Test
    fun `different input hashes to a different value`() {
        assertNotEquals(PasswordHasher.hash("0000"), PasswordHasher.hash("1234"))
    }

    @Test
    fun `hash does not contain the raw password`() {
        val hash = PasswordHasher.hash("0000")

        assertNotEquals("0000", hash)
    }
}
