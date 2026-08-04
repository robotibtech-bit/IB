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
    val sortOrder: Int = 0
)
