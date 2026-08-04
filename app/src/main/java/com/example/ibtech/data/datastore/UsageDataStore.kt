package com.example.ibtech.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * 이용방법 안내 콘텐츠 저장소 (요구사항 명세서 0.1절 `data/datastore`, 7단계).
 *
 * [FacilityDataStore][com.example.ibtech.data.datastore.facilityDataStore]와 동일하게
 * Room 없이 DataStore(Preferences) + JSON 직렬화로 둔다 — 관리자 화면(10단계)이 이 저장소를
 * 그대로 쓸 수 있다.
 */
val Context.usageDataStore by preferencesDataStore(name = "usage_topics")

object UsageDataStoreKeys {
    val TOPICS_JSON: Preferences.Key<String> = stringPreferencesKey("usage_topics_json")
}
