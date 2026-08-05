package com.example.ibtech.domain.model

/** 주요 공지 항목 (요구사항 명세서 3.5절) — 짧은 한 줄 문구만 다룬다. */
data class LibraryNotice(
    val id: String,
    val text: String,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)
