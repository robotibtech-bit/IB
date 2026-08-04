package com.example.ibtech.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * 시설(Facility) 목록 저장소 (요구사항 명세서 0.1절 `data/datastore`, 4단계).
 *
 * Room 없이 [AppSettingsDataStore]와 동일한 DataStore(Preferences) 패턴을 쓴다. 목록 전체를
 * JSON 문자열 하나로 직렬화해 단일 키에 저장한다 — 항목 수가 도서관 POI 규모(수십 개)라
 * 쿼리 성능이 문제되지 않는다. 관리자 CRUD(10단계)나 통계(11단계)에서 Room이 실제로
 * 필요해지면 [com.example.ibtech.data.repository.FacilityRepository] 내부만 교체하면 된다.
 */
val Context.facilityDataStore by preferencesDataStore(name = "facilities")

object FacilityDataStoreKeys {
    val FACILITIES_JSON: Preferences.Key<String> = stringPreferencesKey("facilities_json")
}
