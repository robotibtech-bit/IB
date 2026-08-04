package com.example.ibtech.domain.model

/** 어린이 추천도서 (로드맵 6장, 요구사항 명세서 2.14절). */
data class RecommendedBook(
    val id: String,
    val title: String,
    val author: String,
    val ageGroup: String? = null,
    val topic: String? = null,
    val description: String = "",
    val coverPath: String? = null,
    val locationText: String? = null,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)
