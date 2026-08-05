package com.example.ibtech.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * 행사·공지(9단계) 저장소. [KidsContentDataStore]와 동일하게 Room 없이 JSON 직렬화로 관리한다.
 */
val Context.eventDataStore by preferencesDataStore(name = "events")

object EventDataStoreKeys {
    val EVENTS_JSON: Preferences.Key<String> = stringPreferencesKey("events_json")
    val NOTICES_JSON: Preferences.Key<String> = stringPreferencesKey("notices_json")
}
