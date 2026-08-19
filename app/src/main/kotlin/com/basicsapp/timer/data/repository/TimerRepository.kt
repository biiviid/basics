package com.basicsapp.timer.data.repository

import com.basicsapp.timer.data.database.SessionDao
import com.basicsapp.timer.data.database.SolveDao
import com.basicsapp.timer.data.models.Session
import com.basicsapp.timer.data.models.Solve
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val solveDao: SolveDao
) {
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAllSessions()
    fun getSolvesBySession(sessionId: Int): Flow<List<Solve>> = solveDao.getSolvesBySession(sessionId)
    suspend fun insertSession(session: Session): Long = sessionDao.insert(session)
    suspend fun updateSession(session: Session) = sessionDao.update(session)
    suspend fun deleteSession(session: Session) = sessionDao.delete(session)
    suspend fun getSessionById(id: Int): Session? = sessionDao.getSessionById(id)
    suspend fun insertSolve(solve: Solve): Long = solveDao.insert(solve)
    suspend fun deleteSolve(solve: Solve) = solveDao.delete(solve)
    suspend fun getLastSolveForSession(sessionId: Int): Solve? = solveDao.getLastSolveForSession(sessionId)
    suspend fun deleteAllSolvesForSession(sessionId: Int) = solveDao.deleteAllSolvesForSession(sessionId)
    suspend fun updatePenalty(id: Int, penalty: Int) = solveDao.updatePenalty(id, penalty)
    suspend fun getBestSolveForPuzzleType(puzzleType: String): Solve? = solveDao.getBestSolveForPuzzleType(puzzleType)
}
