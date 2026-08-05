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
            val restoredSettings = settingsRepository.settings.first().copy(
                welcomeMessage = root.optString("welcomeMessage", LibrarySettings.DEFAULT_WELCOME_MESSAGE),
                idleTimeoutSeconds = root.optInt("idleTimeoutSeconds", LibrarySettings.DEFAULT_IDLE_TIMEOUT_SECONDS),
                baseFloor = root.optInt("baseFloor", LibrarySettings.DEFAULT_BASE_FLOOR),
                volume = root.optInt("volume", LibrarySettings.DEFAULT_VOLUME),
                featuredFacilityCount = root.optInt(
                    "featuredFacilityCount",
                    LibrarySettings.DEFAULT_FEATURED_FACILITY_COUNT
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

    /** 마지막 백업 시각(epoch millis). 백업 파일이 없으면 null. */
    fun lastBackupAt(): Long? = backupFile().takeIf { it.exists() }?.lastModified()

    private fun backupFile(): File = File(context.filesDir, "library_backup.json")

    companion object {
        private const val BACKUP_VERSION = 1

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
