package com.example.ibtech.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.ibtech.data.datastore.AppSettingsKeys
import com.example.ibtech.data.datastore.appSettingsDataStore
import com.example.ibtech.domain.model.LibrarySettings
import kotlinx.coroutines.flow.Flow
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
                ?: LibrarySettings.DEFAULT_BASE_FLOOR
        )
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
