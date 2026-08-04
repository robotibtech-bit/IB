package com.example.ibtech.domain.model

/**
 * 이용방법 안내 항목 (로드맵 6장, 요구사항 명세서 2.7~2.9절).
 *
 * [parentId]가 null이면 1차 메뉴 카테고리(예: "책 대출·반납")이고, 값이 있으면 그 카테고리의
 * 하위 항목이며 실제 답변([shortAnswer])을 갖는다. 계층은 카테고리 → 하위 항목 2단계뿐이다.
 */
data class UsageTopic(
    val id: String,
    val parentId: String?,
    val title: String,
    val shortAnswer: String? = null,
    val qrUrl: String? = null,
    val relatedFacilityId: String? = null,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)
