package com.example.ibtech.ui.kids

import com.example.ibtech.domain.model.Facility

/**
 * 노출 가능한 시설 중 "어린이자료실"에 해당할 첫 시설을 찾는다.
 *
 * 퀴즈 결과/추천도서 화면이 "어린이자료실 안내" 버튼을 보여줄지 정할 때 공용으로 쓴다 — 시설은
 * 로봇 POI 기반 동적 목록이라 고정 ID가 없으므로(4단계 `FacilityIcons.kt`와 동일한 이유)
 * 이름 키워드로 최선 추정한다. 매칭 실패 시 안전하게 null(버튼 숨김)로 폴백한다.
 *
 * "어린이"만으로 찾으면 "장애인·어린이 화장실" 같은 시설이 실제 "어린이자료실"보다 먼저 잡혀
 * 잘못된 목적지로 안내될 수 있다. "어린이"와 "자료실"을 모두 포함하는 이름을 우선 찾고, 그런
 * 이름이 하나도 없을 때만(예: POI 이름이 다르게 붙은 도서관) 기존 방식대로 넓게 찾는다.
 */
fun List<Facility>.findChildrenFacility(): Facility? =
    firstOrNull { it.name.contains("어린이") && it.name.contains("자료실") }
        ?: firstOrNull { it.name.contains("어린이") }
