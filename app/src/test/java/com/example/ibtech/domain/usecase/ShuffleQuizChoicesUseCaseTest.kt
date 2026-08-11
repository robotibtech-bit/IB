package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.QuizQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShuffleQuizChoicesUseCaseTest {

    private fun fourChoiceQuestion() = QuizQuestion(
        id = "quiz_dinosaur_01",
        category = "공룡",
        question = "티라노사우루스의 주된 먹이는 무엇일까?",
        choices = listOf("나뭇잎", "풀", "고기", "과일"),
        correctIndex = 2,
        explanation = "육식 공룡이야!"
    )

    private fun twoChoiceQuestion() = QuizQuestion(
        id = "quiz_dinosaur_11",
        category = "공룡",
        question = "공룡의 발자국도 화석이 될 수 있을까?",
        choices = listOf("그렇다", "아니다"),
        correctIndex = 0,
        explanation = "흔적도 화석이 될 수 있어."
    )

    @Test
    fun `shuffled choices always keep the same correct answer text`() {
        val original = fourChoiceQuestion()
        val correctText = original.choices[original.correctIndex]

        repeat(50) {
            val shuffled = ShuffleQuizChoicesUseCase(original)
            assertEquals(correctText, shuffled.choices[shuffled.correctIndex])
        }
    }

    @Test
    fun `shuffle preserves the choice set and count`() {
        val original = fourChoiceQuestion()
        val shuffled = ShuffleQuizChoicesUseCase(original)

        assertEquals(original.choices.size, shuffled.choices.size)
        assertEquals(original.choices.toSet(), shuffled.choices.toSet())
    }

    @Test
    fun `two-choice question keeps correct mapping after shuffle`() {
        val original = twoChoiceQuestion()
        val correctText = original.choices[original.correctIndex]

        repeat(50) {
            val shuffled = ShuffleQuizChoicesUseCase(original)
            assertEquals(2, shuffled.choices.size)
            assertEquals(correctText, shuffled.choices[shuffled.correctIndex])
        }
    }

    @Test
    fun `original question is not mutated`() {
        val original = fourChoiceQuestion()
        val originalChoicesCopy = original.choices.toList()

        ShuffleQuizChoicesUseCase(original)

        assertEquals(originalChoicesCopy, original.choices)
        assertEquals(2, original.correctIndex)
    }

    @Test
    fun `shuffle can eventually produce a different order`() {
        val original = fourChoiceQuestion()
        val sawDifferentOrder = (1..100).any { ShuffleQuizChoicesUseCase(original).choices != original.choices }
        assertTrue("expected at least one shuffled order to differ from the original", sawDifferentOrder)
    }
}
