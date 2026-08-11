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

    /**
     * 기본 시드를 아직 한 번도 채우지 않았으면(최초 실행) 채운다.
     *
     * "최초 실행 여부"는 [KidsContentDataStoreKeys.DEFAULT_CONTENT_SEEDED] 플래그로만 판단한다
     * — 목록이 현재 비어있는지로 판단하면, 관리자가 세 목록을 모두 의도적으로 삭제했을 때
     * `KidsMenu` 재진입마다(이 함수가 다시 호출될 때마다) 기본 데이터가 부활한다.
     */
    suspend fun ensureSeeded(context: Context) {
        val alreadySeeded = dataStore.data.first()[KidsContentDataStoreKeys.DEFAULT_CONTENT_SEEDED] ?: false
        if (alreadySeeded) return

        // 이 플래그가 없는 기존 설치(이 수정 이전 버전에서 이미 정상 시드된 상태)를 위한
        // 마이그레이션: 이미 콘텐츠가 있으면 덮어쓰지 않고 플래그만 세운다.
        val hasExistingContent = quizQuestions.first().isNotEmpty() ||
            books.first().isNotEmpty() ||
            etiquetteTips.first().isNotEmpty()

        dataStore.edit { prefs ->
            if (!hasExistingContent) {
                prefs[KidsContentDataStoreKeys.QUIZ_QUESTIONS_JSON] =
                    KidsJsonMapper.quizToJson(DefaultKidsContent.buildQuizQuestions(context))
                prefs[KidsContentDataStoreKeys.BOOKS_JSON] =
                    KidsJsonMapper.booksToJson(DefaultKidsContent.buildBooks(context))
                prefs[KidsContentDataStoreKeys.ETIQUETTE_TIPS_JSON] =
                    KidsJsonMapper.etiquetteToJson(DefaultKidsContent.buildEtiquetteTips(context))
            }
            prefs[KidsContentDataStoreKeys.DEFAULT_CONTENT_SEEDED] = true
        }
    }

    /**
     * 200문제 문제은행(공룡/과학/동물/동화 각 50문제)을 아직 병합해보지 않았으면 1회 병합한다.
     *
     * [ensureSeeded]와 별개의 플래그([KidsContentDataStoreKeys.QUIZ_BANK_200_MERGED])를 쓴다 —
     * 이 기기가 이미 예전 12문제로 시드가 끝난 상태([ensureSeeded]는 그냥 반환)여도 200문제는
     * 한 번은 반영되어야 한다. 이미 있는 id는 건드리지 않고(관리자가 고쳤을 수 있으므로) 새
     * id만 추가한다 — 예전 자리표시자 12문제(`quiz_animal_1` 등)는 200문제 시드로 완전히
     * 대체된 내용이라 이 1회 마이그레이션에서만 함께 제거한다.
     */
    suspend fun ensureQuizBank200(context: Context) {
        val alreadyMerged = dataStore.data.first()[KidsContentDataStoreKeys.QUIZ_BANK_200_MERGED] ?: false
        if (alreadyMerged) return

        val target = DefaultKidsContent.buildQuizQuestions(context)
        val current = quizQuestions.first().filterNot { it.id in LEGACY_PLACEHOLDER_QUIZ_IDS }
        val existingIds = current.map { it.id }.toSet()
        val merged = current + target.filterNot { it.id in existingIds }

        dataStore.edit { prefs ->
            prefs[KidsContentDataStoreKeys.QUIZ_QUESTIONS_JSON] = KidsJsonMapper.quizToJson(merged)
            prefs[KidsContentDataStoreKeys.QUIZ_BANK_200_MERGED] = true
        }
    }

    /**
     * 200문제·200이미지 최종본(`QUIZ_IMAGE_MAPPING_200_FINAL.md` 기준, 2보기 59문제/4보기
     * 141문제)을 아직 적용해보지 않았으면 1회 적용한다.
     *
     * [ensureQuizBank200]과 달리 이미 있는 id라도 최종본에 포함된 id면 최종본 내용으로
     * 덮어쓴다 — 같은 id(`quiz_dinosaur_01` 등)라도 이전 버전은 보기 수·이미지가 확정되기
     * 전 임시본이었기 때문이다. 최종본에 없는 id(관리자가 직접 추가한 문제 등)는 그대로 둔다.
     */
    suspend fun ensureQuizBank200SmartChoices(context: Context) {
        val alreadyMerged =
            dataStore.data.first()[KidsContentDataStoreKeys.QUIZ_BANK_200_SMART_CHOICES_MERGED] ?: false
        if (alreadyMerged) return

        val target = DefaultKidsContent.buildQuizQuestions(context)
        val targetIds = target.map { it.id }.toSet()
        val current = quizQuestions.first().filterNot { it.id in targetIds }
        val merged = current + target

        dataStore.edit { prefs ->
            prefs[KidsContentDataStoreKeys.QUIZ_QUESTIONS_JSON] = KidsJsonMapper.quizToJson(merged)
            prefs[KidsContentDataStoreKeys.QUIZ_BANK_200_SMART_CHOICES_MERGED] = true
        }
    }

    /**
     * 60권 이상 추천도서 풀(기획 문서 "3. 초기 추천 도서 풀")을 아직 병합해보지 않았으면 1회
     * 병합한다. [ensureQuizBank200]과 같은 패턴 — 이미 있는 id는 건드리지 않고 새 id만
     * 추가하며, 새 풀과 완전히 같은 책인 예전 "강아지똥"(`book_dog_poop`, 취향 태그 없음)만
     * 이번 마이그레이션에서 함께 제거한다(새 풀의 `book_puppy_poop`이 같은 책을 태그 포함해
     * 대체한다).
     */
    suspend fun ensureBookPool60(context: Context) {
        val alreadyMerged = dataStore.data.first()[KidsContentDataStoreKeys.BOOK_POOL_60_MERGED] ?: false
        if (alreadyMerged) return

        val target = DefaultKidsContent.buildBooks(context)
        val current = books.first().filterNot { it.id == LEGACY_DUPLICATE_BOOK_ID }
        val existingIds = current.map { it.id }.toSet()
        val merged = current + target.filterNot { it.id in existingIds }

        dataStore.edit { prefs ->
            prefs[KidsContentDataStoreKeys.BOOKS_JSON] = KidsJsonMapper.booksToJson(merged)
            prefs[KidsContentDataStoreKeys.BOOK_POOL_60_MERGED] = true
        }
    }

    suspend fun getBook(id: String): RecommendedBook? =
        books.first().firstOrNull { it.id == id }

    /** 관리자 화면(10단계) 퀴즈/추천도서/예절 CRUD. id가 이미 있으면 갱신, 없으면 추가한다. */
    suspend fun upsertQuiz(question: QuizQuestion) {
        val current = quizQuestions.first()
        val updated = if (current.any { it.id == question.id }) {
            current.map { if (it.id == question.id) question else it }
        } else {
            current + question
        }
        saveQuiz(updated)
    }

    suspend fun deleteQuiz(id: String) {
        saveQuiz(quizQuestions.first().filterNot { it.id == id })
    }

    suspend fun upsertBook(book: RecommendedBook) {
        val current = books.first()
        val updated = if (current.any { it.id == book.id }) {
            current.map { if (it.id == book.id) book else it }
        } else {
            current + book
        }
        saveBooks(updated)
    }

    suspend fun deleteBook(id: String) {
        saveBooks(books.first().filterNot { it.id == id })
    }

    suspend fun upsertEtiquette(tip: LibraryEtiquetteTip) {
        val current = etiquetteTips.first()
        val updated = if (current.any { it.id == tip.id }) {
            current.map { if (it.id == tip.id) tip else it }
        } else {
            current + tip
        }
        saveEtiquette(updated)
    }

    suspend fun deleteEtiquette(id: String) {
        saveEtiquette(etiquetteTips.first().filterNot { it.id == id })
    }

    /** [BackupRepository] 복구 전용: 백업 목록들로 세 콘텐츠 전체를 대체한다. */
    suspend fun replaceAll(
        quiz: List<QuizQuestion>,
        books: List<RecommendedBook>,
        etiquette: List<LibraryEtiquetteTip>
    ) {
        saveQuiz(quiz)
        saveBooks(books)
        saveEtiquette(etiquette)
    }

    private suspend fun saveQuiz(questions: List<QuizQuestion>) {
        dataStore.edit { prefs -> prefs[KidsContentDataStoreKeys.QUIZ_QUESTIONS_JSON] = KidsJsonMapper.quizToJson(questions) }
    }

    private suspend fun saveBooks(books: List<RecommendedBook>) {
        dataStore.edit { prefs -> prefs[KidsContentDataStoreKeys.BOOKS_JSON] = KidsJsonMapper.booksToJson(books) }
    }

    private suspend fun saveEtiquette(tips: List<LibraryEtiquetteTip>) {
        dataStore.edit { prefs -> prefs[KidsContentDataStoreKeys.ETIQUETTE_TIPS_JSON] = KidsJsonMapper.etiquetteToJson(tips) }
    }

    companion object {
        /** [ensureQuizBank200]이 딱 한 번 정리하는, 200문제 이전 임시 예시 문제 12개의 id. */
        private val LEGACY_PLACEHOLDER_QUIZ_IDS = setOf(
            "quiz_animal_1", "quiz_animal_2", "quiz_animal_3",
            "quiz_dinosaur_1", "quiz_dinosaur_2", "quiz_dinosaur_3",
            "quiz_science_1", "quiz_science_2", "quiz_science_3",
            "quiz_fairytale_1", "quiz_fairytale_2", "quiz_fairytale_3"
        )

        /** [ensureBookPool60]이 딱 한 번 정리하는, 새 풀의 `book_puppy_poop`과 같은 책인 예전 id. */
        private const val LEGACY_DUPLICATE_BOOK_ID = "book_dog_poop"

        @Volatile
        private var instance: KidsContentRepository? = null

        fun getInstance(context: Context): KidsContentRepository =
            instance ?: synchronized(this) {
                instance ?: KidsContentRepository(context.applicationContext.kidsContentDataStore)
                    .also { instance = it }
            }
    }
}
