package com.example.ibtech.data.repository

import android.content.Context
import com.example.ibtech.R
import com.example.ibtech.domain.model.LibraryEtiquetteTip
import com.example.ibtech.domain.model.QuizQuestion
import com.example.ibtech.domain.model.RecommendedBook

/**
 * 어린이 콘텐츠 최초 시드 (요구사항 로드맵 2.1절 "관리자 설정 또는 로컬 데이터 파일에서 변경
 * 가능" — 7단계 [DefaultUsageContent]와 같은 이유로 `strings.xml`에서 읽어 구성한다).
 *
 * 퀴즈는 동물·공룡·과학·동화에 대한 일반 상식이라 확신할 수 있는 사실만 담았다(도서관 고유
 * 정책이 아니므로 지어낼 필요가 없다). 추천도서는 실존하지 않는 책을 지어내 진짜 서지정보처럼
 * 보이게 하지 않기 위해, 저자 귀속이 확실한 유명 동화만 실었다. 위치정보는 청구기호 같은
 * 구체적 서가 정보를 지어내지 않고 "어린이자료실"처럼 일반적인 표현만 쓴다.
 */
object DefaultKidsContent {

    fun buildQuizQuestions(context: Context): List<QuizQuestion> {
        fun s(resId: Int) = context.getString(resId)
        fun choices(arrayResId: Int) = context.resources.getStringArray(arrayResId).toList()

        fun question(
            id: String,
            category: String,
            textRes: Int,
            choicesArrayRes: Int,
            correctIndex: Int,
            explanationRes: Int,
            sortOrder: Int,
            recommendedBookIds: List<String> = emptyList()
        ) = QuizQuestion(
            id = id,
            category = category,
            question = s(textRes),
            choices = choices(choicesArrayRes),
            correctIndex = correctIndex,
            explanation = s(explanationRes),
            recommendedBookIds = recommendedBookIds,
            sortOrder = sortOrder
        )

        val animal = s(R.string.quiz_category_animal)
        val dinosaur = s(R.string.quiz_category_dinosaur)
        val science = s(R.string.quiz_category_science)
        val fairytale = s(R.string.quiz_category_fairytale)

        // 퀴즈 주제와 실제로 어울리는 책만 연결한다(공룡·과학은 시드 도서 중 어울리는 책이
        // 없어 비워 둔다 — 억지로 끼워 맞춘 추천을 하지 않는다).
        val animalBooks = listOf("book_rainbow_fish", "book_dog_poop")
        val fairytaleBooks = listOf("book_cloud_bread", "book_moon_sherbet")

        return listOf(
            question("quiz_animal_1", animal, R.string.quiz_q_animal_1_text, R.array.quiz_q_animal_1_choices, 0, R.string.quiz_q_animal_1_explanation, 0, animalBooks),
            question("quiz_animal_2", animal, R.string.quiz_q_animal_2_text, R.array.quiz_q_animal_2_choices, 0, R.string.quiz_q_animal_2_explanation, 1, animalBooks),
            question("quiz_animal_3", animal, R.string.quiz_q_animal_3_text, R.array.quiz_q_animal_3_choices, 0, R.string.quiz_q_animal_3_explanation, 2, animalBooks),

            question("quiz_dinosaur_1", dinosaur, R.string.quiz_q_dinosaur_1_text, R.array.quiz_q_dinosaur_1_choices, 0, R.string.quiz_q_dinosaur_1_explanation, 0),
            question("quiz_dinosaur_2", dinosaur, R.string.quiz_q_dinosaur_2_text, R.array.quiz_q_dinosaur_2_choices, 0, R.string.quiz_q_dinosaur_2_explanation, 1),
            question("quiz_dinosaur_3", dinosaur, R.string.quiz_q_dinosaur_3_text, R.array.quiz_q_dinosaur_3_choices, 0, R.string.quiz_q_dinosaur_3_explanation, 2),

            question("quiz_science_1", science, R.string.quiz_q_science_1_text, R.array.quiz_q_science_1_choices, 0, R.string.quiz_q_science_1_explanation, 0),
            question("quiz_science_2", science, R.string.quiz_q_science_2_text, R.array.quiz_q_science_2_choices, 0, R.string.quiz_q_science_2_explanation, 1),
            question("quiz_science_3", science, R.string.quiz_q_science_3_text, R.array.quiz_q_science_3_choices, 0, R.string.quiz_q_science_3_explanation, 2),

            question("quiz_fairytale_1", fairytale, R.string.quiz_q_fairytale_1_text, R.array.quiz_q_fairytale_1_choices, 0, R.string.quiz_q_fairytale_1_explanation, 0, fairytaleBooks),
            question("quiz_fairytale_2", fairytale, R.string.quiz_q_fairytale_2_text, R.array.quiz_q_fairytale_2_choices, 0, R.string.quiz_q_fairytale_2_explanation, 1, fairytaleBooks),
            question("quiz_fairytale_3", fairytale, R.string.quiz_q_fairytale_3_text, R.array.quiz_q_fairytale_3_choices, 0, R.string.quiz_q_fairytale_3_explanation, 2, fairytaleBooks)
        )
    }

    fun buildBooks(context: Context): List<RecommendedBook> {
        fun s(resId: Int) = context.getString(resId)
        val location = s(R.string.kids_book_location_generic)

        return listOf(
            RecommendedBook(
                id = "book_cloud_bread",
                title = s(R.string.kids_book_title_cloud_bread),
                author = s(R.string.kids_book_author_cloud_bread),
                ageGroup = s(R.string.kids_book_age_cloud_bread),
                topic = s(R.string.kids_book_topic_cloud_bread),
                description = s(R.string.kids_book_desc_cloud_bread),
                locationText = location,
                sortOrder = 0
            ),
            RecommendedBook(
                id = "book_dog_poop",
                title = s(R.string.kids_book_title_dog_poop),
                author = s(R.string.kids_book_author_dog_poop),
                ageGroup = s(R.string.kids_book_age_dog_poop),
                topic = s(R.string.kids_book_topic_dog_poop),
                description = s(R.string.kids_book_desc_dog_poop),
                locationText = location,
                sortOrder = 1
            ),
            RecommendedBook(
                id = "book_rainbow_fish",
                title = s(R.string.kids_book_title_rainbow_fish),
                author = s(R.string.kids_book_author_rainbow_fish),
                ageGroup = s(R.string.kids_book_age_rainbow_fish),
                topic = s(R.string.kids_book_topic_rainbow_fish),
                description = s(R.string.kids_book_desc_rainbow_fish),
                locationText = location,
                sortOrder = 2
            ),
            RecommendedBook(
                id = "book_moon_sherbet",
                title = s(R.string.kids_book_title_moon_sherbet),
                author = s(R.string.kids_book_author_moon_sherbet),
                ageGroup = s(R.string.kids_book_age_moon_sherbet),
                topic = s(R.string.kids_book_topic_moon_sherbet),
                description = s(R.string.kids_book_desc_moon_sherbet),
                locationText = location,
                sortOrder = 3
            )
        )
    }

    fun buildEtiquetteTips(context: Context): List<LibraryEtiquetteTip> {
        fun s(resId: Int) = context.getString(resId)

        return listOf(
            LibraryEtiquetteTip("etiquette_1", s(R.string.kids_etiquette_tip_1), sortOrder = 0),
            LibraryEtiquetteTip("etiquette_2", s(R.string.kids_etiquette_tip_2), sortOrder = 1),
            LibraryEtiquetteTip("etiquette_3", s(R.string.kids_etiquette_tip_3), sortOrder = 2),
            LibraryEtiquetteTip("etiquette_4", s(R.string.kids_etiquette_tip_4), sortOrder = 3)
        )
    }
}
