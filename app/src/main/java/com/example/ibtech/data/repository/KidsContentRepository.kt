package com.example.ibtech.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.ibtech.data.datastore.KidsContentDataStoreKeys
import com.example.ibtech.data.datastore.kidsContentDataStore
import com.example.ibtech.domain.model.LibraryEtiquetteTip
import com.example.ibtech.domain.model.QuizQuestion
import com.example.ibtech.domain.model.RecommendedBook
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 어린이 콘텐츠(퀴즈/추천도서/도서관 예절)의 단일 접근 지점 (요구사항 명세서 2.10~2.15절).
 *
 * [UsageRepository]와 같은 이유로 싱글턴 + 인터페이스 없는 직접 클래스로 둔다.
 */
class KidsContentRepository private constructor(
    private val dataStore: DataStore<Preferences>
) {

    val quizQuestions: Flow<List<QuizQuestion>> = dataStore.data.map { prefs ->
        KidsJsonMapper.quizFromJson(prefs[KidsContentDataStoreKeys.QUIZ_QUESTIONS_JSON].orEmpty())
    }

    val books: Flow<List<RecommendedBook>> = dataStore.data.map { prefs ->
        KidsJsonMapper.booksFromJson(prefs[KidsContentDataStoreKeys.BOOKS_JSON].orEmpty())
    }

    val etiquetteTips: Flow<List<LibraryEtiquetteTip>> = dataStore.data.map { prefs ->
        KidsJsonMapper.etiquetteFromJson(prefs[KidsContentDataStoreKeys.ETIQUETTE_TIPS_JSON].orEmpty())
    }

    /** 저장된 콘텐츠가 하나도 없으면(최초 실행) 기본 시드로 1회 채운다. */
    suspend fun ensureSeeded(context: Context) {
        if (quizQuestions.first().isNotEmpty() ||
            books.first().isNotEmpty() ||
            etiquetteTips.first().isNotEmpty()
        ) {
            return
        }
        dataStore.edit { prefs ->
            prefs[KidsContentDataStoreKeys.QUIZ_QUESTIONS_JSON] =
                KidsJsonMapper.quizToJson(DefaultKidsContent.buildQuizQuestions(context))
            prefs[KidsContentDataStoreKeys.BOOKS_JSON] =
                KidsJsonMapper.booksToJson(DefaultKidsContent.buildBooks(context))
            prefs[KidsContentDataStoreKeys.ETIQUETTE_TIPS_JSON] =
                KidsJsonMapper.etiquetteToJson(DefaultKidsContent.buildEtiquetteTips(context))
        }
    }

    suspend fun getBook(id: String): RecommendedBook? =
        books.first().firstOrNull { it.id == id }

    companion object {
        @Volatile
        private var instance: KidsContentRepository? = null

        fun getInstance(context: Context): KidsContentRepository =
            instance ?: synchronized(this) {
                instance ?: KidsContentRepository(context.applicationContext.kidsContentDataStore)
                    .also { instance = it }
            }
    }
}
