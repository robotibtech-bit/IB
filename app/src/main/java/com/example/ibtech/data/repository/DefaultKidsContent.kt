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

    /** 200문제 시드(공룡/과학/동물/동화 각 50문제)의 자산 파일명. [KidsJsonMapper.quizToJson]과
     * 동일한 JSON 모양이라 그대로 [KidsJsonMapper.quizFromJson]으로 읽는다 — 200개를 Kotlin
     * 코드나 `strings.xml`에 직접 박지 않고 자산으로 분리했다(문서
     * `docs/CLAUDE_QUIZ_IMAGE_HANDOFF_OPTIMIZED_112_OF_200` 인계본 기준). */
    private const val QUIZ_SEED_ASSET = "quiz_seed_200.json"

    /** 60권 이상 추천도서 풀(기획 문서 "3. 초기 추천 도서 풀") 자산 파일명. 기존 시드 4권 중
     * 3권(구름빵/무지개 물고기/달샤베트)은 같은 id로 이 안에 포함되어 있어 마이그레이션 시
     * 병합돼도 중복되지 않는다 — "강아지똥"만 새 풀의 동일 도서(취향 태그 포함)로 대체됐다. */
    private const val BOOK_POOL_ASSET = "book_pool_60.json"

    fun buildQuizQuestions(context: Context): List<QuizQuestion> {
        val json = context.assets.open(QUIZ_SEED_ASSET).bufferedReader().use { it.readText() }
        return KidsJsonMapper.quizFromJson(json)
    }

    fun buildBooks(context: Context): List<RecommendedBook> {
        val json = context.assets.open(BOOK_POOL_ASSET).bufferedReader().use { it.readText() }
        return KidsJsonMapper.booksFromJson(json)
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
