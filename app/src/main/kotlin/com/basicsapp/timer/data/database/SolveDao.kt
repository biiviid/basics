package com.basicsapp.timer.data.database

import androidx.room.*
import com.basicsapp.timer.data.models.Solve
import kotlinx.coroutines.flow.Flow

@Dao
interface SolveDao {
    @Insert suspend fun insert(solve: Solve): Long
    @Update suspend fun update(solve: Solve)
    @Delete suspend fun delete(solve: Solve)
    @Query("SELECT * FROM solves WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getSolvesBySession(sessionId: Int): Flow<List<Solve>>
    @Query("SELECT * FROM solves WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastSolveForSession(sessionId: Int): Solve?
    @Query("DELETE FROM solves WHERE sessionId = :sessionId") suspend fun deleteAllSolvesForSession(sessionId: Int)
    @Query("UPDATE solves SET penalty = :penalty WHERE id = :id") suspend fun updatePenalty(id: Int, penalty: Int)
    @Query("SELECT s.* FROM solves s INNER JOIN sessions sess ON s.sessionId = sess.id WHERE sess.puzzleType = :puzzleType AND s.penalty != -1 ORDER BY s.timeMs ASC LIMIT 1")
    suspend fun getBestSolveForPuzzleType(puzzleType: String): Solve?
}
