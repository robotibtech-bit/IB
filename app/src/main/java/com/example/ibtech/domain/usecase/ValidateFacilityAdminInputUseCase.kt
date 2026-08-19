package com.example.ibtech.domain.usecase

import com.example.ibtech.domain.model.Facility

/** 시설 관리 화면의 표시명/층 입력값 검증 (로드맵 10단계). */
object ValidateFacilityAdminInputUseCase {
    operator fun invoke(name: String, floorText: String): FacilityAdminValidation {
        if (name.isBlank()) return FacilityAdminValidation.BlankName
        // 층 드롭다운의 "미설정"(null) 선택지는 floorText를 빈 문자열로 넘긴다 — 이것도 유효한
        // 선택지인데 toIntOrNull()만 쓰면 항상 InvalidFloor로 걸려 저장 자체가 안 됐다(발견:
        // 층이 미설정인 기존 시설을 편집·저장하려 하면 조용히 실패하는 버그).
        if (floorText.isBlank()) return FacilityAdminValidation.Valid(Facility.UNSET_FLOOR)
        val floor = floorText.trim().toIntOrNull() ?: return FacilityAdminValidation.InvalidFloor
        return FacilityAdminValidation.Valid(floor)
    }
}

sealed interface FacilityAdminValidation {
    data class Valid(val floor: Int) : FacilityAdminValidation
    data object BlankName : FacilityAdminValidation
    data object InvalidFloor : FacilityAdminValidation
}
