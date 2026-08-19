package com.basicsapp.timer.data.database

import androidx.room.*
import com.basicsapp.timer.data.models.Session
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert suspend fun insert(session: Session): Long
    @Update suspend fun update(session: Session)
    @Delete suspend fun delete(session: Session)
    @Query("SELECT * FROM sessions ORDER BY updatedAt DESC") fun getAllSessions(): Flow<List<Session>>
    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun getSessionById(id: Int): Session?
    @Query("DELETE FROM sessions") suspend fun deleteAllSessions()
}
