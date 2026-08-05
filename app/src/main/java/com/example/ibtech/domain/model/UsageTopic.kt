package com.example.ibtech.domain.model

/**
 * 이용방법 안내 항목 (로드맵 6장, 요구사항 명세서 2.7~2.9절).
 *
 * [parentId]가 null이면 1차 메뉴 카테고리(예: "책 대출·반납")이고, 값이 있으면 그 카테고리의
 * 하위 항목이며 실제 답변([shortAnswer])을 갖는다. 계층은 카테고리 → 하위 항목 2단계뿐이다.
 *
 * [tableData]는 운영시간표·대출규정표처럼 표로 보여주는 게 더 명확한 항목에만 채운다.
 * 형식은 행마다 줄바꿈, 칸마다 `|`로 구분한 평문이며 첫 줄이 헤더다(예: "구분|평일|주말\n종합자료실|09:00|09:00").
 * 새 직렬화 형식을 들이지 않고 [shortAnswer]와 같은 방식으로 다루기 위한 선택이다.
 */
data class UsageTopic(
    val id: String,
    val parentId: String?,
    val title: String,
    val shortAnswer: String? = null,
    val tableData: String? = null,
    val qrUrl: String? = null,
    val relatedFacilityId: String? = null,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)
