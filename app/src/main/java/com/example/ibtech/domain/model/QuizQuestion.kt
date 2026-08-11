package com.example.ibtech.domain.model

/**
 * 어린이 콘텐츠 퀴즈 문제 (로드맵 6장, 요구사항 명세서 2.11~2.13절).
 *
 * [category]는 관리자가 등록한 값을 그대로 쓴다("동물/공룡/과학/동화"는 예시일 뿐 코드에
 * 고정하지 않는다 — [com.example.ibtech.ui.kids.QuizCategoryScreen]이 실제 등록된 문제에서
 * distinct category만 뽑아 카드를 만든다).
 */
data class QuizQuestion(
    val id: String,
    val category: String,
    val question: String,
    val choices: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val recommendedBookIds: List<String> = emptyList(),
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    /** `res/drawable`의 리소스 이름(확장자 제외, 예: "dinosaur_01"). 해당 이름의 PNG가 아직
     * 없으면(200문제 시드 중 88개는 의도적으로 비어 있음) 화면에서 공통 기본 이미지로 대체한다
     * — 나중에 같은 이름의 PNG를 `res/drawable`에 추가하기만 하면 코드 수정 없이 반영된다. */
    val imageKey: String? = null
)
