package com.example.ibtech.domain.model

/** 도서관 예절 카드 한 장 (요구사항 명세서 2.15절 — 행동 중심의 짧은 문구). */
data class LibraryEtiquetteTip(
    val id: String,
    val text: String,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)
