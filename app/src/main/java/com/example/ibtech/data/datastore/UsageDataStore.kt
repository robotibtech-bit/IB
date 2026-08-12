package com.example.ibtech.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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

    /** 실시간 좌석 현황 항목을 이미 추가하고 순서(첫 번째)까지 맞춰봤는지(1회성 마이그레이션).
     * [UsageRepository.ensureSeeded] 이후 추가된 항목이라 기존 설치에는
     * [ensureReadingSeatStatusTopic][com.example.ibtech.data.repository.UsageRepository.ensureReadingSeatStatusTopic]로
     * 따로 채운다. v2는 "첫 번째로 옮겨달라"는 요청 이후 순서 정정을 포함한다. */
    val READING_SEAT_STATUS_MERGED: Preferences.Key<Boolean> = booleanPreferencesKey("reading_seat_status_merged_v2")
}
