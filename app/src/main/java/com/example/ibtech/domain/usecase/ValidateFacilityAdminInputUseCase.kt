package com.example.ibtech.domain.usecase

/** 시설 관리 화면의 표시명/층 입력값 검증 (로드맵 10단계). */
object ValidateFacilityAdminInputUseCase {
    operator fun invoke(name: String, floorText: String): FacilityAdminValidation {
        if (name.isBlank()) return FacilityAdminValidation.BlankName
        val floor = floorText.trim().toIntOrNull() ?: return FacilityAdminValidation.InvalidFloor
        return FacilityAdminValidation.Valid(floor)
    }
}

sealed interface FacilityAdminValidation {
    data class Valid(val floor: Int) : FacilityAdminValidation
    data object BlankName : FacilityAdminValidation
    data object InvalidFloor : FacilityAdminValidation
}
