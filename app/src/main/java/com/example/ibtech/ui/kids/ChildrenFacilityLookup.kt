package com.example.ibtech.ui.kids

import com.example.ibtech.domain.model.Facility

/**
 * 노출 가능한 시설 중 "어린이"가 이름에 들어간 첫 시설을 찾는다.
 *
 * 퀴즈 결과/추천도서 화면이 "어린이자료실 안내" 버튼을 보여줄지 정할 때 공용으로 쓴다 — 시설은
 * 로봇 POI 기반 동적 목록이라 고정 ID가 없으므로(4단계 `FacilityIcons.kt`와 동일한 이유)
 * 이름 키워드로 최선 추정한다. 매칭 실패 시 안전하게 null(버튼 숨김)로 폴백한다.
 */
fun List<Facility>.findChildrenFacility(): Facility? =
    firstOrNull { it.name.contains("어린이") }
