package com.example.ibtech.data.stats

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 통계 전용 Room 데이터베이스 (로드맵 11단계).
 *
 * 다른 저장소는 전부 DataStore+JSON을 쓰지만([FacilityRepository] 등의 주석 참고), 통계는
 * 시간대별 범위 쿼리(오늘/주간/월간)와 계속 늘어나는 append 로그라는 점에서 Room이 더 맞는
 * 유일한 데이터라 이 저장소만 예외로 둔다.
 */
@Database(entities = [StatEventEntity::class], version = 1, exportSchema = false)
abstract class StatsDatabase : RoomDatabase() {

    abstract fun statEventDao(): StatEventDao

    companion object {
        @Volatile
        private var instance: StatsDatabase? = null

        fun getInstance(context: Context): StatsDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StatsDatabase::class.java,
                    "stats.db"
                ).build().also { instance = it }
            }
    }
}
