package com.example.ibtech.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.ibtech.data.datastore.EventDataStoreKeys
import com.example.ibtech.data.datastore.eventDataStore
import com.example.ibtech.domain.model.LibraryEvent
import com.example.ibtech.domain.model.LibraryNotice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 행사·공지(9단계)의 단일 접근 지점 (요구사항 명세서 3.5절).
 *
 * "신트리도서관"은 실제 운영 데이터가 없는 예시 도서관이라 [DefaultUsageContent]와 같은 이유로
 * 지어낸 행사·공지를 기본값으로 채우지 않는다. 저장된 값이 없으면 빈 목록을 반환하며, 이는
 * 로드맵 9단계가 요구하는 "행사 없음" 상태 그대로다. 실제 운영 데이터는 10단계 관리자 화면에서
 * 입력한다.
 */
class EventRepository private constructor(
    private val dataStore: DataStore<Preferences>
) {

    val events: Flow<List<LibraryEvent>> = dataStore.data.map { prefs ->
        EventJsonMapper.eventsFromJson(prefs[EventDataStoreKeys.EVENTS_JSON].orEmpty())
    }

    val notices: Flow<List<LibraryNotice>> = dataStore.data.map { prefs ->
        EventJsonMapper.noticesFromJson(prefs[EventDataStoreKeys.NOTICES_JSON].orEmpty())
    }

    suspend fun getEvent(id: String): LibraryEvent? =
        events.first().firstOrNull { it.id == id }

    /** 관리자 화면(10단계) 행사/공지 CRUD. id가 이미 있으면 갱신, 없으면 추가한다. */
    suspend fun upsertEvent(event: LibraryEvent) {
        val current = events.first()
        val updated = if (current.any { it.id == event.id }) {
            current.map { if (it.id == event.id) event else it }
        } else {
            current + event
        }
        saveEvents(updated)
    }

    suspend fun deleteEvent(id: String) {
        saveEvents(events.first().filterNot { it.id == id })
    }

    suspend fun upsertNotice(notice: LibraryNotice) {
        val current = notices.first()
        val updated = if (current.any { it.id == notice.id }) {
            current.map { if (it.id == notice.id) notice else it }
        } else {
            current + notice
        }
        saveNotices(updated)
    }

    suspend fun deleteNotice(id: String) {
        saveNotices(notices.first().filterNot { it.id == id })
    }

    /** [BackupRepository] 복구 전용: 백업 목록들로 행사/공지 전체를 대체한다. */
    suspend fun replaceAll(events: List<LibraryEvent>, notices: List<LibraryNotice>) {
        saveEvents(events)
        saveNotices(notices)
    }

    private suspend fun saveEvents(events: List<LibraryEvent>) {
        dataStore.edit { prefs -> prefs[EventDataStoreKeys.EVENTS_JSON] = EventJsonMapper.eventsToJson(events) }
    }

    private suspend fun saveNotices(notices: List<LibraryNotice>) {
        dataStore.edit { prefs -> prefs[EventDataStoreKeys.NOTICES_JSON] = EventJsonMapper.noticesToJson(notices) }
    }

    companion object {
        @Volatile
        private var instance: EventRepository? = null

        fun getInstance(context: Context): EventRepository =
            instance ?: synchronized(this) {
                instance ?: EventRepository(context.applicationContext.eventDataStore)
                    .also { instance = it }
            }
    }
}
