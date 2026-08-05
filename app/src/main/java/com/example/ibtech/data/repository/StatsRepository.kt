package com.example.ibtech.data.repository

import android.content.Context
import com.example.ibtech.data.stats.StatEventDao
import com.example.ibtech.data.stats.StatEventEntity
import com.example.ibtech.data.stats.StatsDatabase
import com.example.ibtech.domain.model.StatEvent
import com.example.ibtech.domain.model.StatEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 통계 이벤트의 단일 접근 지점 (요구사항 명세서 4.3절, 로드맵 11단계).
 *
 * 개인정보 없이 메뉴/시설/콘텐츠 이름과 시각만 기록한다. [BackupRepository]와 같은 이유로
 * CSV 내보내기도 기기 내부 저장소(`context.filesDir`)에만 저장하고 외부 공유는 하지 않는다.
 */
class StatsRepository private constructor(
    private val context: Context,
    private val dao: StatEventDao
) {

    fun observeEventsSince(since: Long): Flow<List<StatEvent>> =
        dao.observeSince(since).map { entities -> entities.map { it.toDomain() } }

    suspend fun logEvent(type: StatEventType, label: String? = null) {
        dao.insert(
            StatEventEntity(
                type = type.name,
                label = label,
                correctCount = null,
                totalCount = null,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun logQuizComplete(category: String, correctCount: Int, totalCount: Int) {
        dao.insert(
            StatEventEntity(
                type = StatEventType.QUIZ_COMPLETE.name,
                label = category,
                correctCount = correctCount,
                totalCount = totalCount,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /** 전체 이벤트 로그를 CSV로 내보낸다. 성공하면 true. */
    suspend fun exportCsv(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val rows = dao.getAll()
            val csv = buildString {
                appendLine("timestamp,type,label,correctCount,totalCount")
                rows.forEach { row ->
                    appendLine(
                        listOf(
                            row.timestamp.toString(),
                            row.type,
                            csvField(row.label),
                            row.correctCount?.toString().orEmpty(),
                            row.totalCount?.toString().orEmpty()
                        ).joinToString(",")
                    )
                }
            }
            exportFile().writeText(csv)
            true
        }.getOrDefault(false)
    }

    fun lastExportAt(): Long? = exportFile().takeIf { it.exists() }?.lastModified()

    private fun csvField(value: String?): String =
        "\"${value.orEmpty().replace("\"", "\"\"")}\""

    private fun exportFile(): File = File(context.filesDir, "stats_export.csv")

    private fun StatEventEntity.toDomain(): StatEvent = StatEvent(
        type = runCatching { StatEventType.valueOf(type) }.getOrDefault(StatEventType.MENU_SELECT),
        label = label,
        correctCount = correctCount,
        totalCount = totalCount,
        timestamp = timestamp
    )

    companion object {
        @Volatile
        private var instance: StatsRepository? = null

        fun getInstance(context: Context): StatsRepository =
            instance ?: synchronized(this) {
                instance ?: StatsRepository(
                    context = context.applicationContext,
                    dao = StatsDatabase.getInstance(context).statEventDao()
                ).also { instance = it }
            }
    }
}
