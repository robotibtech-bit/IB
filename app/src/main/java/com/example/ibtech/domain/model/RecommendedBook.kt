package com.example.ibtech.domain.model

/**
 * 어린이 추천도서 (로드맵 6장, 요구사항 명세서 2.14절).
 *
 * [ageGroup]/[topic]은 기존 "직접 골라볼래요"(BROWSE_ALL) 필터 화면이 그대로 쓰는 자유 문구다.
 * [tags]는 새로 추가한 취향 퀴즈 채점용 태그로, `"차원:값"` 형식 문자열이다(예:
 * `"genre:fantasy"`, `"mood:touching"`, `"length:short"`, `"illustration:many"`,
 * `"world:imaginative"`, `"age:middle"`) — [com.example.ibtech.domain.model.BookTasteQuestion]의
 * 선택지 점수([BookTasteOption.scoreDelta])와 같은 키로 매칭해 점수를 매긴다. 관리자가 추가한
 * 책은 태그가 비어 있을 수 있고, 그 경우 취향 점수가 0이라 추천 상위권에는 잘 안 뽑히지만
 * "직접 골라볼래요"에서는 기존과 동일하게 보인다(둘은 서로 다른 경로라 태그 유무가 필터
 * 탐색을 막지 않는다).
 */
data class RecommendedBook(
    val id: String,
    val title: String,
    val author: String,
    val ageGroup: String? = null,
    val topic: String? = null,
    val description: String = "",
    val coverPath: String? = null,
    val locationText: String? = null,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0,
    val tags: List<String> = emptyList()
)
