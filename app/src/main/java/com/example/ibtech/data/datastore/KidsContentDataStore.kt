package com.example.ibtech.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * 어린이 콘텐츠(퀴즈/추천도서/도서관 예절) 저장소 (요구사항 명세서 0.1절 `data/datastore`,
 * 8단계). 세 콘텐츠가 항상 같이 다뤄지고 서로 참조([QuizQuestion.recommendedBookIds])하므로
 * 파일을 나누지 않고 하나의 DataStore에 키 3개로 둔다. [FacilityDataStore]와 동일하게 Room
 * 없이 JSON 직렬화로 관리한다.
 */
val Context.kidsContentDataStore by preferencesDataStore(name = "kids_content")

object KidsContentDataStoreKeys {
    val QUIZ_QUESTIONS_JSON: Preferences.Key<String> = stringPreferencesKey("quiz_questions_json")
    val BOOKS_JSON: Preferences.Key<String> = stringPreferencesKey("books_json")
    val ETIQUETTE_TIPS_JSON: Preferences.Key<String> = stringPreferencesKey("etiquette_tips_json")

    /** 기본 시드를 이미 채워봤는지. 목록이 현재 비어있는지와 분리해서, 관리자가 의도적으로
     * 전부 삭제한 뒤에도 기본값이 다시 채워지지 않게 한다. */
    val DEFAULT_CONTENT_SEEDED: Preferences.Key<Boolean> = booleanPreferencesKey("default_content_seeded")

    /** 200문제 문제은행을 이미 병합해봤는지(1회성 마이그레이션). [DEFAULT_CONTENT_SEEDED]와
     * 별개다 — 이미 시드가 끝난(구버전 12문제) 기존 설치에도 한 번만 200문제를 추가하기
     * 위해서다. 그 뒤로는 관리자가 지우거나 고쳐도 다시 채우지 않는다. */
    val QUIZ_BANK_200_MERGED: Preferences.Key<Boolean> = booleanPreferencesKey("quiz_bank_200_merged")

    /** 60권 이상 추천도서 풀을 이미 병합해봤는지(1회성 마이그레이션). [QUIZ_BANK_200_MERGED]와
     * 같은 이유로 별개 플래그를 쓴다. */
    val BOOK_POOL_60_MERGED: Preferences.Key<Boolean> = booleanPreferencesKey("book_pool_60_merged")
}
