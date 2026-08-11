package com.example.ibtech.domain.model

/**
 * "나에게 맞는 책" 취향 퀴즈 한 문항 (기획 문서 "2. 나에게 맞는 책" 절).
 *
 * 질문/책/태그를 화면 코드와 분리하기 위한 순수 데이터 모델이다 — [BookTasteEngine]이 이
 * 모델과 [RecommendedBook.tags]만으로 채점하므로 UI(BookRecommendationScreen)는 문항 내용을
 * 전혀 몰라도 된다.
 */
data class BookTasteQuestion(
    val id: String,
    val question: String,
    val options: List<BookTasteOption>
)

/**
 * [scoreDelta]의 키는 [RecommendedBook.tags]와 같은 `"차원:값"` 형식이다. 한 선택지가 여러
 * 차원에 동시에 점수를 줄 수 있다(기획 예: "사건 해결 → mystery +3, curious +2").
 */
data class BookTasteOption(
    val label: String,
    val scoreDelta: Map<String, Int>
)
