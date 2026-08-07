package com.example.ibtech.data.repository

import android.content.Context
import com.example.ibtech.domain.model.LibrarySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * 운영 데이터 백업/복구 (요구사항 4.1절 "데이터 내보내기/가져오기(JSON)", 로드맵 10단계).
 *
 * 기기 내부 저장소(`context.filesDir`)에 파일 하나로만 저장한다 — 외부 공유는 하지 않는다(이번
 * 단계 범위). 관리자 비밀번호([LibrarySettings.adminPasswordHash])는 백업에 포함하지 않는다 —
 * 오래된 백업을 복구했다가 최근에 바꾼 비밀번호가 되돌아가 관리자가 잠기는 일을 막기 위해서다.
 */
class BackupRepository private constructor(
    private val context: Context,
    private val facilityRepository: FacilityRepository,
    private val usageRepository: UsageRepository,
    private val kidsContentRepository: KidsContentRepository,
    private val eventRepository: EventRepository,
    private val settingsRepository: SettingsRepository
) {

    suspend fun export(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val settings = settingsRepository.settings.first()
            val root = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("welcomeMessage", settings.welcomeMessage)
                put("idleTimeoutSeconds", settings.idleTimeoutSeconds)
                put("baseFloor", settings.baseFloor)
                put("volume", settings.volume)
                put("featuredFacilityCount", settings.featuredFacilityCount)
                put("facilitiesJson", FacilityJsonMapper.toJson(facilityRepository.allFacilities.first()))
                put("usageTopicsJson", UsageJsonMapper.toJson(usageRepository.topics.first()))
                put("quizQuestionsJson", KidsJsonMapper.quizToJson(kidsContentRepository.quizQuestions.first()))
                put("booksJson", KidsJsonMapper.booksToJson(kidsContentRepository.books.first()))
                put("etiquetteTipsJson", KidsJsonMapper.etiquetteToJson(kidsContentRepository.etiquetteTips.first()))
                put("eventsJson", EventJsonMapper.eventsToJson(eventRepository.events.first()))
                put("noticesJson", EventJsonMapper.noticesToJson(eventRepository.notices.first()))
            }
            backupFile().writeText(root.toString())
            true
        }.getOrDefault(false)
    }

    suspend fun import(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val file = backupFile()
            if (!file.exists()) return@runCatching false

            val root = JSONObject(file.readText())

            // 이 앱이 이해하는 버전 범위를 벗어나면(미래 포맷·손상된 값) 복구하지 않는다.
            // 그대로 진행하면 알 수 없는 필드 구성을 잘못 해석해 설정이 깨질 수 있다.
            val version = root.optInt("version", UNKNOWN_BACKUP_VERSION)
            if (version !in MIN_SUPPORTED_BACKUP_VERSION..BACKUP_VERSION) {
                return@runCatching false
            }

            // 정상 export()는 이 7개 키를 항상 함께 쓴다. 하나라도 없으면 잘리거나 손상된
            // 파일이라는 뜻이므로, 있는 항목만 부분 반영해 나머지를 빈 목록으로 지우는 대신
            // 아무것도 건드리지 않고 실패로 처리한다(부분 백업 복구 위험 최소화).
            val requiredKeys = listOf(
                "facilitiesJson", "usageTopicsJson", "quizQuestionsJson",
                "booksJson", "etiquetteTipsJson", "eventsJson", "noticesJson"
            )
            if (requiredKeys.any { !root.has(it) }) {
                return@runCatching false
            }

            val restoredSettings = settingsRepository.settings.first().copy(
                welcomeMessage = root.optString("welcomeMessage", LibrarySettings.DEFAULT_WELCOME_MESSAGE),
                idleTimeoutSeconds = sanitizedIdleTimeoutSeconds(
                    root.optInt("idleTimeoutSeconds", LibrarySettings.DEFAULT_IDLE_TIMEOUT_SECONDS)
                ),
                baseFloor = root.optInt("baseFloor", LibrarySettings.DEFAULT_BASE_FLOOR),
                volume = sanitizedVolume(root.optInt("volume", LibrarySettings.DEFAULT_VOLUME)),
                featuredFacilityCount = sanitizedFeaturedFacilityCount(
                    root.optInt("featuredFacilityCount", LibrarySettings.DEFAULT_FEATURED_FACILITY_COUNT)
                )
            )
            settingsRepository.updateSettings(restoredSettings)
            facilityRepository.replaceAll(FacilityJsonMapper.fromJson(root.optString("facilitiesJson", "")))
            usageRepository.replaceAll(UsageJsonMapper.fromJson(root.optString("usageTopicsJson", "")))
            kidsContentRepository.replaceAll(
                quiz = KidsJsonMapper.quizFromJson(root.optString("quizQuestionsJson", "")),
                books = KidsJsonMapper.booksFromJson(root.optString("booksJson", "")),
                etiquette = KidsJsonMapper.etiquetteFromJson(root.optString("etiquetteTipsJson", ""))
            )
            eventRepository.replaceAll(
                events = EventJsonMapper.eventsFromJson(root.optString("eventsJson", "")),
                notices = EventJsonMapper.noticesFromJson(root.optString("noticesJson", ""))
            )
            true
        }.getOrDefault(false)
    }

    /** 무입력 timeout이 0/음수면 화면 진입 즉시 홈으로 복귀하는 상태가 된다. 관리자 설정 화면의
     * 수동 저장(`SettingsAdminViewModel.onSaveSettings`)과 같은 기준(`> 0`)으로 막는다. */
    private fun sanitizedIdleTimeoutSeconds(value: Int): Int =
        if (value > 0) value else LibrarySettings.DEFAULT_IDLE_TIMEOUT_SECONDS

    private fun sanitizedVolume(value: Int): Int =
        value.coerceIn(LibrarySettings.MIN_VOLUME, LibrarySettings.MAX_VOLUME)

    private fun sanitizedFeaturedFacilityCount(value: Int): Int =
        if (value in LibrarySettings.FEATURED_FACILITY_COUNT_OPTIONS) {
            value
        } else {
            LibrarySettings.DEFAULT_FEATURED_FACILITY_COUNT
        }

    /** 마지막 백업 시각(epoch millis). 백업 파일이 없으면 null. */
    fun lastBackupAt(): Long? = backupFile().takeIf { it.exists() }?.lastModified()

    private fun backupFile(): File = File(context.filesDir, "library_backup.json")

    companion object {
        private const val BACKUP_VERSION = 1
        private const val MIN_SUPPORTED_BACKUP_VERSION = 1
        private const val UNKNOWN_BACKUP_VERSION = -1

        @Volatile
        private var instance: BackupRepository? = null

        fun getInstance(context: Context): BackupRepository =
            instance ?: synchronized(this) {
                instance ?: BackupRepository(
                    context = context.applicationContext,
                    facilityRepository = FacilityRepository.getInstance(context),
                    usageRepository = UsageRepository.getInstance(context),
                    kidsContentRepository = KidsContentRepository.getInstance(context),
                    eventRepository = EventRepository.getInstance(context),
                    settingsRepository = SettingsRepository.getInstance(context)
                ).also { instance = it }
            }
    }
}
