package com.example.ibtech.ui.kids

import com.example.ibtech.domain.model.Facility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChildrenFacilityLookupTest {

    private fun facility(id: String, name: String) = Facility(
        id = id,
        sourcePoiName = id,
        name = name,
        floor = 1,
        isEnabled = true
    )

    @Test
    fun `finds a facility whose name contains 어린이`() {
        val facilities = listOf(facility("f1", "종합자료실"), facility("f2", "어린이자료실"))

        val result = facilities.findChildrenFacility()

        assertEquals("f2", result?.id)
    }

    @Test
    fun `returns null when no facility matches`() {
        val facilities = listOf(facility("f1", "종합자료실"), facility("f2", "열람실"))

        val result = facilities.findChildrenFacility()

        assertNull(result)
    }

    @Test
    fun `returns the first match when several facilities match`() {
        val facilities = listOf(facility("f1", "어린이자료실"), facility("f2", "어린이놀이터"))

        val result = facilities.findChildrenFacility()

        assertEquals("f1", result?.id)
    }
}
