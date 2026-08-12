package com.example.ibtech.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.ibtech.data.datastore.UsageDataStoreKeys
import com.example.ibtech.data.datastore.usageDataStore
import com.example.ibtech.domain.model.UsageTopic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * [UsageTopic] 목록의 단일 접근 지점 (요구사항 명세서 2.7~2.9절).
 *
 * [FacilityRepository]와 같은 이유로 싱글턴 + 인터페이스 없는 직접 클래스로 둔다. 저장 방식은
 * 이 클래스 안에만 캡슐화되어 있어, 이후 Room으로 바꾸더라도 참조하는 ViewModel 코드는
 * 바뀌지 않는다.
 */
class UsageRepository private constructor(
    private val dataStore: DataStore<Preferences>
) {

    val topics: Flow<List<UsageTopic>> = dataStore.data.map { prefs ->
        UsageJsonMapper.fromJson(prefs[UsageDataStoreKeys.TOPICS_JSON].orEmpty())
    }

    /** 저장된 콘텐츠가 없으면(최초 실행) 기본 시드로 1회 채운다. 이미 있으면 아무 것도 하지 않는다. */
    suspend fun ensureSeeded(context: Context) {
        if (topics.first().isNotEmpty()) return
        save(DefaultUsageContent.build(context))
    }

    /**
     * "실시간 좌석 현황" 항목을 아직 병합해보지 않았으면 1회 추가하고, 열람실·자료실 이용
     * 카테고리 안에서 첫 번째 항목이 되도록 나머지 항목들의 순서도 함께 맞춘다. [ensureSeeded]
     * 이후에 생긴 항목이라, 이미 콘텐츠가 채워진 기존 설치에도 [KidsContentRepository.ensureQuizBank200]과
     * 같은 방식(플래그 확인)으로 반영한다. 관리자가 이미 같은 id로 직접 추가했다면 새로 만들지
     * 않고 순서만 정리한다.
     */
    suspend fun ensureReadingSeatStatusTopic(context: Context) {
        val alreadyMerged = dataStore.data.first()[UsageDataStoreKeys.READING_SEAT_STATUS_MERGED] ?: false
        if (alreadyMerged) return

        // 열람실·자료실 이용 카테고리 안에서의 의도한 순서 — 실시간 좌석 현황이 0(첫 번째).
        val desiredOrder = mapOf(
            DefaultUsageContent.READING_SEAT_STATUS_TOPIC_ID to 0,
            "reading_seat" to 1,
            "reading_rules" to 2,
            "materials_general_kids" to 3,
            "materials_digital" to 4
        )

        val current = topics.first()
        val withSeatStatus = if (current.none { it.id == DefaultUsageContent.READING_SEAT_STATUS_TOPIC_ID }) {
            current + DefaultUsageContent.buildReadingSeatStatusTopic(context)
        } else {
            current
        }
        val reordered = withSeatStatus.map { topic ->
            desiredOrder[topic.id]?.let { topic.copy(sortOrder = it) } ?: topic
        }
        save(reordered)
        dataStore.edit { prefs -> prefs[UsageDataStoreKeys.READING_SEAT_STATUS_MERGED] = true }
    }

    suspend fun getTopic(id: String): UsageTopic? =
        topics.first().firstOrNull { it.id == id }

    /** 관리자 화면(10단계)의 세부 항목 추가/수정에 쓴다. id가 이미 있으면 갱신, 없으면 추가한다. */
    suspend fun upsertTopic(topic: UsageTopic) {
        val current = topics.first()
        val updated = if (current.any { it.id == topic.id }) {
            current.map { if (it.id == topic.id) topic else it }
        } else {
            current + topic
        }
        save(updated)
    }

    suspend fun deleteTopic(id: String) {
        save(topics.first().filterNot { it.id == id })
    }

    /** [BackupRepository] 복구 전용: 백업 목록으로 전체를 대체한다. */
    suspend fun replaceAll(topics: List<UsageTopic>) {
        save(topics)
    }

    private suspend fun save(topics: List<UsageTopic>) {
        dataStore.edit { prefs ->
            prefs[UsageDataStoreKeys.TOPICS_JSON] = UsageJsonMapper.toJson(topics)
        }
    }

    companion object {
        @Volatile
        private var instance: UsageRepository? = null

        fun getInstance(context: Context): UsageRepository =
            instance ?: synchronized(this) {
                instance ?: UsageRepository(context.applicationContext.usageDataStore)
                    .also { instance = it }
            }
    }
}
