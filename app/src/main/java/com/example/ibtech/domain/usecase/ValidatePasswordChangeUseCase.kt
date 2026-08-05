package com.example.ibtech.domain.usecase

/** 관리자 비밀번호 변경 입력값 검증 (로드맵 10단계 "입력값 검증과 저장 성공/실패 표시"). */
object ValidatePasswordChangeUseCase {
    const val MIN_LENGTH = 4

    operator fun invoke(newPassword: String, confirmPassword: String): PasswordChangeValidation = when {
        newPassword.length < MIN_LENGTH -> PasswordChangeValidation.TooShort
        newPassword != confirmPassword -> PasswordChangeValidation.Mismatch
        else -> PasswordChangeValidation.Valid
    }
}

sealed interface PasswordChangeValidation {
    data object Valid : PasswordChangeValidation
    data object TooShort : PasswordChangeValidation
    data object Mismatch : PasswordChangeValidation
}
