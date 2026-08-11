package com.example.ibtech.data.repository

import android.content.Context
import com.example.ibtech.R
import com.example.ibtech.domain.model.BookTasteOption
import com.example.ibtech.domain.model.BookTasteQuestion

/**
 * "나에게 맞는 책" 취향 퀴즈 6문항 (기획 문서 "2. 나에게 맞는 책" 절의 질문 예시 1~4 +
 * 선택 질문 2개를 고정 6문항으로 구성). 질문/보기 점수를 화면 코드와 분리하기 위해 여기 둔다
 * — [com.example.ibtech.domain.usecase.BookTasteEngine]과 [RecommendedBook.tags]만으로
 * 채점하므로 문항 내용이 늘어나도 화면·채점 로직은 그대로다.
 */
object DefaultBookTasteQuestions {

    fun build(context: Context): List<BookTasteQuestion> {
        fun s(resId: Int) = context.getString(resId)

        return listOf(
            BookTasteQuestion(
                id = "taste_q1_world",
                question = s(R.string.kids_book_taste_q1_title),
                options = listOf(
                    BookTasteOption(s(R.string.kids_book_taste_q1_opt_fantasy), mapOf("genre:fantasy" to 3, "world:imaginative" to 2)),
                    BookTasteOption(s(R.string.kids_book_taste_q1_opt_mystery), mapOf("genre:mystery" to 3, "mood:curious" to 2)),
                    BookTasteOption(s(R.string.kids_book_taste_q1_opt_humor), mapOf("genre:humor" to 3, "mood:funny" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q1_opt_animal), mapOf("genre:animal" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q1_opt_science), mapOf("genre:science" to 3, "mood:curious" to 2)),
                    BookTasteOption(s(R.string.kids_book_taste_q1_opt_history), mapOf("genre:history" to 3, "world:realistic" to 1))
                )
            ),
            BookTasteQuestion(
                id = "taste_q2_mood",
                question = s(R.string.kids_book_taste_q2_title),
                options = listOf(
                    BookTasteOption(s(R.string.kids_book_taste_q2_opt_exciting), mapOf("mood:exciting" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q2_opt_curious), mapOf("mood:curious" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q2_opt_funny), mapOf("mood:funny" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q2_opt_warm), mapOf("mood:warm" to 3, "mood:touching" to 1))
                )
            ),
            BookTasteQuestion(
                id = "taste_q3_character",
                question = s(R.string.kids_book_taste_q3_title),
                options = listOf(
                    BookTasteOption(s(R.string.kids_book_taste_q3_opt_adventure), mapOf("genre:adventure" to 3, "mood:exciting" to 1)),
                    BookTasteOption(s(R.string.kids_book_taste_q3_opt_humor), mapOf("genre:humor" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q3_opt_animal), mapOf("genre:animal" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q3_opt_fantasy), mapOf("genre:fantasy" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q3_opt_mystery), mapOf("genre:mystery" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q3_opt_science), mapOf("genre:science" to 2, "genre:adventure" to 2))
                )
            ),
            BookTasteQuestion(
                id = "taste_q4_length",
                question = s(R.string.kids_book_taste_q4_title),
                options = listOf(
                    BookTasteOption(s(R.string.kids_book_taste_q4_opt_short), mapOf("length:short" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q4_opt_medium), mapOf("length:medium" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q4_opt_long), mapOf("length:long" to 3))
                )
            ),
            BookTasteQuestion(
                id = "taste_q5_illustration",
                question = s(R.string.kids_book_taste_q5_title),
                options = listOf(
                    BookTasteOption(s(R.string.kids_book_taste_q5_opt_many), mapOf("illustration:many" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q5_opt_medium), mapOf("illustration:medium" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q5_opt_few), mapOf("illustration:few" to 3))
                )
            ),
            BookTasteQuestion(
                id = "taste_q6_world",
                question = s(R.string.kids_book_taste_q6_title),
                options = listOf(
                    BookTasteOption(s(R.string.kids_book_taste_q6_opt_realistic), mapOf("world:realistic" to 3)),
                    BookTasteOption(s(R.string.kids_book_taste_q6_opt_imaginative), mapOf("world:imaginative" to 3))
                )
            )
        )
    }
}
