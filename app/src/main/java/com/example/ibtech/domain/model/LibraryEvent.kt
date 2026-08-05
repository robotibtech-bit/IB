package com.example.ibtech.domain.model

/**
 * 오늘의 행사·프로그램 항목 (요구사항 명세서 3.5절, 로드맵 6장/9단계).
 *
 * [startDate]/[endDate]는 "yyyy-MM-dd" 형식 날짜만 담아 오늘/예정 분류에 쓴다. 정확한 시간은
 * 자유 형식 문구인 [timeText]로 따로 둔다 — `minSdk 24`라 desugaring 없이 `java.time`을 쓸 수
 * 없고, 날짜 비교는 문자열 사전식 비교만으로 충분해 별도 파싱이 필요 없다.
 */
data class LibraryEvent(
    val id: String,
    val title: String,
    val startDate: String,
    val endDate: String? = null,
    val timeText: String? = null,
    val place: String,
    val target: String? = null,
    val description: String = "",
    val qrUrl: String? = null,
    val relatedFacilityId: String? = null,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)
