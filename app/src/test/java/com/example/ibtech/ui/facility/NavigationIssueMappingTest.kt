package com.example.ibtech.ui.facility

import com.example.ibtech.R
import com.example.ibtech.robot.NavigationIssue
import org.junit.Assert.assertEquals
import org.junit.Test

/** [NavigationIssue]가 화면에 뿌릴 문자열 리소스로 빠짐없이, 겹치지 않게 매핑되는지 검증한다. */
class NavigationIssueMappingTest {

    @Test
    fun `each issue maps to its own message resource`() {
        assertEquals(R.string.navigation_stopped_by_user, NavigationIssue.USER_STOPPED.toMessageRes())
        assertEquals(R.string.navigation_issue_external, NavigationIssue.EXTERNAL_INTERRUPTION.toMessageRes())
        assertEquals(R.string.navigation_issue_timeout, NavigationIssue.TIMEOUT.toMessageRes())
        assertEquals(R.string.navigation_issue_sdk_error, NavigationIssue.SDK_ERROR.toMessageRes())
    }

    @Test
    fun `all four issues map to distinct resources`() {
        val mapped = NavigationIssue.entries.map { it.toMessageRes() }
        assertEquals(mapped.size, mapped.toSet().size)
    }
}
