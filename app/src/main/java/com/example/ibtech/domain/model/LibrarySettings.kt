package com.example.ibtech.domain.model

/**
 * 관리자 설정값 (요구사항 명세서 2.2/4.2절).
 *
 * 도서관명은 "신트리도서관"으로 고정한다(2026-08-04 정정, `R.string.library_name`). 그래서
 * 설정값이 아니라 이 모델에 필드가 없다. 나머지 관리자 항목(음량, 비밀번호 등)은 10단계
 * 관리자 화면에서 이 모델에 추가한다.
 */
data class LibrarySettings(
    val welcomeMessage: String = DEFAULT_WELCOME_MESSAGE,
    val idleTimeoutSeconds: Int = DEFAULT_IDLE_TIMEOUT_SECONDS,
    /**
     * 로봇이 상주하며 직접 동행할 수 있는 기준층 (요구사항 명세서 4장, 8절 결정 사항 1번).
     * PDF 원문의 "1층 고정"을 일반화한 값으로, 도서관마다 다른 운영 층을 지원한다.
     */
    val baseFloor: Int = DEFAULT_BASE_FLOOR
) {
    companion object {
        const val DEFAULT_WELCOME_MESSAGE = "안녕하세요\n무엇을 도와드릴까요?"
        const val DEFAULT_IDLE_TIMEOUT_SECONDS = 60
        const val DEFAULT_BASE_FLOOR = 1
    }
}
