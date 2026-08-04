package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.UsageTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveUsageCategoryItemsUseCaseTest {

    @Test
    fun `category with exactly one enabled child gets singleAnswerTopicId`() {
        val topics = listOf(
            UsageTopic(id = "membership", parentId = null, title = "회원가입"),
            UsageTopic(id = "membership_guide", parentId = "membership", title = "회원가입 안내")
        )

        val result = ResolveUsageCategoryItemsUseCase(topics)

        assertEquals(1, result.size)
        assertEquals("membership_guide", result.single().singleAnswerTopicId)
    }

    @Test
    fun `category with multiple children has null singleAnswerTopicId`() {
        val topics = listOf(
            UsageTopic(id = "loan", parentId = null, title = "책 대출·반납"),
            UsageTopic(id = "loan_count", parentId = "loan", title = "대출 가능 권수", sortOrder = 0),
            UsageTopic(id = "loan_period", parentId = "loan", title = "대출 기간", sortOrder = 1)
        )

        val result = ResolveUsageCategoryItemsUseCase(topics)

        assertEquals(1, result.size)
        assertNull(result.single().singleAnswerTopicId)
    }

    @Test
    fun `category with no enabled children has null singleAnswerTopicId`() {
        val topics = listOf(
            UsageTopic(id = "hours", parentId = null, title = "운영시간·휴관일"),
            UsageTopic(id = "hours_guide", parentId = "hours", title = "운영시간 안내", isEnabled = false)
        )

        val result = ResolveUsageCategoryItemsUseCase(topics)

        assertEquals(1, result.size)
        assertNull(result.single().singleAnswerTopicId)
    }

    @Test
    fun `disabled categories are excluded entirely`() {
        val topics = listOf(
            UsageTopic(id = "hidden", parentId = null, title = "숨김", isEnabled = false),
            UsageTopic(id = "hidden_child", parentId = "hidden", title = "숨김 답변")
        )

        val result = ResolveUsageCategoryItemsUseCase(topics)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `categories are sorted by sortOrder`() {
        val topics = listOf(
            UsageTopic(id = "second", parentId = null, title = "두번째", sortOrder = 1),
            UsageTopic(id = "first", parentId = null, title = "첫번째", sortOrder = 0)
        )

        val result = ResolveUsageCategoryItemsUseCase(topics)

        assertEquals(listOf("first", "second"), result.map { it.category.id })
    }
}
