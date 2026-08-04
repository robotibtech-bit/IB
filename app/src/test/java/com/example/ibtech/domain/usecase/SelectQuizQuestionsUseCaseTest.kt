package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectQuizQuestionsUseCaseTest {

    private fun question(id: String, category: String, isEnabled: Boolean = true) = QuizQuestion(
        id = id,
        category = category,
        question = "질문 $id",
        choices = listOf("가", "나", "다"),
        correctIndex = 0,
        explanation = "설명",
        isEnabled = isEnabled
    )

    @Test
    fun `returns only questions from the requested category`() {
        val questions = listOf(
            question("a1", "동물"),
            question("a2", "동물"),
            question("b1", "공룡")
        )

        val result = SelectQuizQuestionsUseCase(questions, "동물")

        assertTrue(result.all { it.category == "동물" })
    }

    @Test
    fun `returns fewer than limit when the category has fewer questions`() {
        val questions = listOf(question("a1", "과학"), question("a2", "과학"))

        val result = SelectQuizQuestionsUseCase(questions, "과학", limit = 3)

        assertEquals(2, result.size)
    }

    @Test
    fun `caps the result at limit when more questions are available`() {
        val questions = (1..5).map { question("q$it", "동화") }

        val result = SelectQuizQuestionsUseCase(questions, "동화", limit = 3)

        assertEquals(3, result.size)
    }

    @Test
    fun `never returns duplicate questions`() {
        val questions = (1..5).map { question("q$it", "동화") }

        val result = SelectQuizQuestionsUseCase(questions, "동화", limit = 3)

        assertEquals(result.size, result.map { it.id }.toSet().size)
    }

    @Test
    fun `excludes disabled questions`() {
        val questions = listOf(
            question("a1", "동물", isEnabled = false),
            question("a2", "동물", isEnabled = true)
        )

        val result = SelectQuizQuestionsUseCase(questions, "동물")

        assertEquals(listOf("a2"), result.map { it.id })
    }

    @Test
    fun `returns empty list for a category with no questions`() {
        val questions = listOf(question("a1", "동물"))

        val result = SelectQuizQuestionsUseCase(questions, "없는주제")

        assertTrue(result.isEmpty())
    }
}
