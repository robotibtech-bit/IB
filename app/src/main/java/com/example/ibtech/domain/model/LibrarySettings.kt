package com.example.ibtech.domain.model

/**
 * 관리자 설정값 (요구사항 명세서 2.2/4.2절, 로드맵 10단계).
 *
 * 도서관명은 "신트리도서관"으로 고정한다(2026-08-04 정정, `R.string.library_name`). 그래서
 * 설정값이 아니라 이 모델에 필드가 없다.
 *
 * [adminPasswordHash]가 빈 문자열이면 아직 관리자가 비밀번호를 한 번도 바꾸지 않은 상태다 —
 * 이때는 [DEFAULT_ADMIN_PASSWORD]로 로그인할 수 있다([SettingsRepository.verifyAdminPassword]).
 * 평문은 저장하지 않고 [PasswordHasher]로 해시만 저장한다.
 */
data class LibrarySettings(
    val welcomeMessage: String = DEFAULT_WELCOME_MESSAGE,
    val idleTimeoutSeconds: Int = DEFAULT_IDLE_TIMEOUT_SECONDS,
    /**
     * 로봇이 상주하며 직접 동행할 수 있는 기준층 (요구사항 명세서 4장, 8절 결정 사항 1번).
     * PDF 원문의 "1층 고정"을 일반화한 값으로, 도서관마다 다른 운영 층을 지원한다.
     */
    val baseFloor: Int = DEFAULT_BASE_FLOOR,
    /** 안내 음성 재생 음량, 0~100 (요구사항 3.7절 "음량을 관리자 설정으로 조절"). */
    val volume: Int = DEFAULT_VOLUME,
    val adminPasswordHash: String = ""
) {
    companion object {
        const val DEFAULT_WELCOME_MESSAGE = "안녕하세요\n무엇을 도와드릴까요?"
        const val DEFAULT_IDLE_TIMEOUT_SECONDS = 60
        const val DEFAULT_BASE_FLOOR = 1
        const val DEFAULT_VOLUME = 70
        const val MIN_VOLUME = 0
        const val MAX_VOLUME = 100

        /** 최초 배포 시 기본 관리자 비밀번호 (로드맵 13단계 "관리자 초기 비밀번호 전달 절차"). */
        const val DEFAULT_ADMIN_PASSWORD = "0000"
    }
}
