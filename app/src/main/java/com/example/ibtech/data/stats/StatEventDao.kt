package com.example.ibtech.data.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StatEventDao {

    @Insert
    suspend fun insert(event: StatEventEntity)

    @Query("SELECT * FROM stat_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun observeSince(since: Long): Flow<List<StatEventEntity>>

    @Query("SELECT * FROM stat_events ORDER BY timestamp DESC")
    suspend fun getAll(): List<StatEventEntity>
}
