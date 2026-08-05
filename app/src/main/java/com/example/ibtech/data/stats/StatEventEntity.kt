package com.example.ibtech.data.stats

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 통계 이벤트 Room 저장 형태 (로드맵 11단계).
 *
 * [type]은 [com.example.ibtech.domain.model.StatEventType]의 `name`을 그대로 문자열로 저장한다
 * — 정수 코드로 바꾸면 앱 버전이 바뀔 때 enum 순서와 저장된 값이 어긋날 위험이 있어, 다른
 * 저장소의 enum 직렬화(`FacilityJsonMapper`의 `guideMode.name` 등)와 같은 방식을 쓴다.
 */
@Entity(tableName = "stat_events")
data class StatEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val label: String?,
    val correctCount: Int?,
    val totalCount: Int?,
    val timestamp: Long
)
