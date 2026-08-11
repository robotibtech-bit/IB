package com.example.ibtech.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.ibtech.data.datastore.AppSettingsKeys
import com.example.ibtech.data.datastore.appSettingsDataStore
import com.example.ibtech.domain.model.LibrarySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * [LibrarySettings]의 단일 접근 지점.
 *
 * `robot/TemiRepository`와 같은 이유로 싱글턴 + 인터페이스 없는 직접 클래스로 둔다:
 * 구현체가 하나뿐이고, DataStore 자체가 이미 테스트 가능한 순수 Flow API라 별도 대역이
 * 필요 없다. UI/ViewModel은 이 클래스를 통해서만 설정을 읽고 쓴다.
 */
class SettingsRepository private constructor(
    private val dataStore: DataStore<Preferences>
) {

    val settings: Flow<LibrarySettings> = dataStore.data.map { prefs ->
        LibrarySettings(
            welcomeMessage = prefs[AppSettingsKeys.WELCOME_MESSAGE]
                ?: LibrarySettings.DEFAULT_WELCOME_MESSAGE,
            idleTimeoutSeconds = prefs[AppSettingsKeys.IDLE_TIMEOUT_SECONDS]
                ?: LibrarySettings.DEFAULT_IDLE_TIMEOUT_SECONDS,
            baseFloor = prefs[AppSettingsKeys.BASE_FLOOR]
                ?: LibrarySettings.DEFAULT_BASE_FLOOR,
            volume = prefs[AppSettingsKeys.VOLUME] ?: LibrarySettings.DEFAULT_VOLUME,
            volumeLocked = prefs[AppSettingsKeys.VOLUME_LOCKED] ?: LibrarySettings.DEFAULT_VOLUME_LOCKED,
            adminPasswordHash = prefs[AppSettingsKeys.ADMIN_PASSWORD_HASH].orEmpty(),
            featuredFacilityCount = prefs[AppSettingsKeys.FEATURED_FACILITY_COUNT]
                ?: LibrarySettings.DEFAULT_FEATURED_FACILITY_COUNT,
            eventNoticeUrl = prefs[AppSettingsKeys.EVENT_NOTICE_URL]
                ?: LibrarySettings.DEFAULT_EVENT_NOTICE_URL
        )
    }

    /**
     * 환영문구·무입력시간·기준층·음량·대표 장소 개수·행사 안내 URL을 저장한다.
     * [LibrarySettings.adminPasswordHash]는 의도적으로 쓰지 않는다 — 비밀번호는
     * [changeAdminPassword]로만 바꿀 수 있어, 일반 설정 저장이 실수로 비밀번호를 초기화하는
     * 일이 없다.
     */
    suspend fun updateSettings(settings: LibrarySettings) {
        dataStore.edit { prefs ->
            prefs[AppSettingsKeys.WELCOME_MESSAGE] = settings.welcomeMessage
            prefs[AppSettingsKeys.IDLE_TIMEOUT_SECONDS] = settings.idleTimeoutSeconds
            prefs[AppSettingsKeys.BASE_FLOOR] = settings.baseFloor
            prefs[AppSettingsKeys.VOLUME] = settings.volume
            prefs[AppSettingsKeys.VOLUME_LOCKED] = settings.volumeLocked
            prefs[AppSettingsKeys.FEATURED_FACILITY_COUNT] = settings.featuredFacilityCount
            prefs[AppSettingsKeys.EVENT_NOTICE_URL] = settings.eventNoticeUrl
        }
    }

    /** 아직 비밀번호를 바꾼 적이 없으면([LibrarySettings.adminPasswordHash]가 비어 있으면) 기본
     * 비밀번호([LibrarySettings.DEFAULT_ADMIN_PASSWORD])와 비교한다. */
    suspend fun verifyAdminPassword(rawPassword: String): Boolean {
        val expectedHash = settings.first().adminPasswordHash
            .ifBlank { PasswordHasher.hash(LibrarySettings.DEFAULT_ADMIN_PASSWORD) }
        return PasswordHasher.hash(rawPassword) == expectedHash
    }

    suspend fun changeAdminPassword(newRawPassword: String) {
        dataStore.edit { prefs ->
            prefs[AppSettingsKeys.ADMIN_PASSWORD_HASH] = PasswordHasher.hash(newRawPassword)
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext.appSettingsDataStore)
                    .also { instance = it }
            }
    }
}
