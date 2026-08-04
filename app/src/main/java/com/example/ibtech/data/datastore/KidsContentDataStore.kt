package com.example.ibtech.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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
}
