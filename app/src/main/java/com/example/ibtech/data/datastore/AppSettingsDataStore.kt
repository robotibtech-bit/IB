package com.example.ibtech.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * 앱 설정 저장소 (요구사항 명세서 0.1절 `data/datastore`).
 *
 * 환영문구·무입력시간 등 "관리자가 바꾸지만 코드에 고정하지 않는" 값을 담는다. 도서관명은
 * "신트리도서관"으로 고정이라 여기 없다(2026-08-04 정정). 시설/이용정보/퀴즈처럼 목록형
 * 데이터는 이후 단계에서 Room으로 옮긴다.
 */
val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

object AppSettingsKeys {
    val WELCOME_MESSAGE: Preferences.Key<String> = stringPreferencesKey("welcome_message")
    val IDLE_TIMEOUT_SECONDS: Preferences.Key<Int> = intPreferencesKey("idle_timeout_seconds")
    val BASE_FLOOR: Preferences.Key<Int> = intPreferencesKey("base_floor")
    val VOLUME: Preferences.Key<Int> = intPreferencesKey("volume")
    val ADMIN_PASSWORD_HASH: Preferences.Key<String> = stringPreferencesKey("admin_password_hash")
}
