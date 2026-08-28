package com.basicsapp.timer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basicsapp.timer.data.models.Session
import com.basicsapp.timer.data.models.Solve
import com.basicsapp.timer.data.repository.TimerRepository
import com.basicsapp.timer.utils.BasicsExport
import com.basicsapp.timer.utils.PuzzleType
import com.basicsapp.timer.utils.ScrambleGenerator
import com.basicsapp.timer.utils.SessionStats
import com.basicsapp.timer.utils.StatsCalculator
import com.basicsapp.timer.ui.components.CubieCube
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TimerRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("basics_settings", Context.MODE_PRIVATE)

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId: StateFlow<Int?> = _currentSessionId.asStateFlow()

    private val _solves = MutableStateFlow<List<Solve>>(emptyList())
    val solves: StateFlow<List<Solve>> = _solves.asStateFlow()

    private val _stats = MutableStateFlow(SessionStats(null, null, null, null, 0.0, null, null, null, null, null, null, 0, 0, 0))
    val stats: StateFlow<SessionStats> = _stats.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _elapsed = MutableStateFlow(0L)
    val elapsed: StateFlow<Long> = _elapsed.asStateFlow()

    private val _scramble = MutableStateFlow("")
    val scramble: StateFlow<String> = _scramble.asStateFlow()

    private val _puzzleType = MutableStateFlow(PuzzleType.THREE_BY_THREE)
    val puzzleType: StateFlow<PuzzleType> = _puzzleType.asStateFlow()

    private val _sessionPb = MutableStateFlow<Long?>(null)
    val sessionPb: StateFlow<Long?> = _sessionPb.asStateFlow()

    private val _overallPb = MutableStateFlow<Long?>(null)
    val overallPb: StateFlow<Long?> = _overallPb.asStateFlow()

    private val _inspectionEnabled = MutableStateFlow(false)
    val inspectionEnabled: StateFlow<Boolean> = _inspectionEnabled.asStateFlow()

    private val _showAveragesOnTimer = MutableStateFlow(false)
    val showAveragesOnTimer: StateFlow<Boolean> = _showAveragesOnTimer.asStateFlow()

    private val _enabledAverages = MutableStateFlow<Set<Int>>(emptySet())
    val enabledAverages: StateFlow<Set<Int>> = _enabledAverages.asStateFlow()

    private val _holdTimeMs = MutableStateFlow(200)
    val holdTimeMs: StateFlow<Int> = _holdTimeMs.asStateFlow()

    private val _inspectionHoldStart = MutableStateFlow(false)
    val inspectionHoldStart: StateFlow<Boolean> = _inspectionHoldStart.asStateFlow()

    // input mode: generated scrambles, or a manually-entered cube state
    private val _inputMode = MutableStateFlow(TimerInputMode.SCRAMBLE)
    val inputMode: StateFlow<TimerInputMode> = _inputMode.asStateFlow()

    private val _manualColors = MutableStateFlow(CubieCube.solvedColors())
    val manualColors: StateFlow<IntArray> = _manualColors.asStateFlow()

    private val _inspectionTime = MutableStateFlow(15)
    val inspectionTime: StateFlow<Int> = _inspectionTime.asStateFlow()

    private val _isInspecting = MutableStateFlow(false)
    val isInspecting: StateFlow<Boolean> = _isInspecting.asStateFlow()

    private val _inspectionPenalty = MutableStateFlow("none")
    val inspectionPenalty: StateFlow<String> = _inspectionPenalty.asStateFlow()

    private val _lastSolveTime = MutableStateFlow<Long?>(null)
    val lastSolveTime: StateFlow<Long?> = _lastSolveTime.asStateFlow()

    private val _lastSolvePenalty = MutableStateFlow(0)
    val lastSolvePenalty: StateFlow<Int> = _lastSolvePenalty.asStateFlow()

    private val _lastSolveId = MutableStateFlow<Int?>(null)
    val lastSolveId: StateFlow<Int?> = _lastSolveId.asStateFlow()

    // Scramble history for prev/next
    private val _scrambleHistory = MutableStateFlow<List<String>>(emptyList())
    val scrambleHistory: StateFlow<List<String>> = _scrambleHistory.asStateFlow()

    private val _scrambleIndex = MutableStateFlow(0)
    val scrambleIndex: StateFlow<Int> = _scrambleIndex.asStateFlow()

    // Whether last solve used a "previous" scramble
    private val _lastSolveUsedPrevScramble = MutableStateFlow(false)
    val lastSolveUsedPrevScramble: StateFlow<Boolean> = _lastSolveUsedPrevScramble.asStateFlow()

    private var inspectionJob: Job? = null
    private var startTime = 0L
    private var tickerJob: Job? = null

    init {
        // restore persisted settings
        _inspectionEnabled.value = prefs.getBoolean("inspection_enabled", false)
        _showAveragesOnTimer.value = prefs.getBoolean("show_averages_on_timer", false)
        _holdTimeMs.value = prefs.getInt("hold_time_ms", 200).coerceIn(0, 1000)
        _inspectionHoldStart.value = prefs.getBoolean("inspection_hold_start", false)
        _enabledAverages.value = prefs.getString("enabled_averages", "")
            ?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        _inputMode.value = runCatching {
            TimerInputMode.valueOf(prefs.getString("input_mode", TimerInputMode.SCRAMBLE.name) ?: TimerInputMode.SCRAMBLE.name)
        }.getOrDefault(TimerInputMode.SCRAMBLE)

        viewModelScope.launch {
            repository.getAllSessions().collect { sessionList ->
                _sessions.value = sessionList
                if (_currentSessionId.value == null) {
                    if (sessionList.isNotEmpty()) {
                        _currentSessionId.value = sessionList.first().id
                        val session = sessionList.first()
                        _puzzleType.value = PuzzleType.fromString(session.puzzleType)
                    } else {
                        val id = repository.insertSession(Session(name = "default", puzzleType = "3x3"))
                        _currentSessionId.value = id.toInt()
                    }
                }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            _currentSessionId.filterNotNull().flatMapLatest { sessionId ->
                repository.getSolvesBySession(sessionId)
            }.collect { solveList ->
                _solves.value = solveList
                val sessionStats = StatsCalculator.getSessionStats(solveList)
                _stats.value = sessionStats
                _sessionPb.value = sessionStats.best?.takeIf { it != StatsCalculator.DNF }
                updateOverallPb()
                val lastId = _lastSolveId.value
                if (lastId != null && solveList.none { it.id == lastId }) {
                    _lastSolveTime.value = null
                    _lastSolveId.value = null
                    _lastSolvePenalty.value = 0
                    _lastSolveUsedPrevScramble.value = false
                }
            }
        }

        generateNewScramble()
        if (_inputMode.value == TimerInputMode.MANUAL) {
            _scramble.value = manualScrambleString()
        }
    }

    private suspend fun updateOverallPb() {
        val puzzle = _puzzleType.value.name
        val best = repository.getBestSolveForPuzzleType(puzzle)
        _overallPb.value = best?.let {
            if (it.penalty == 2) it.timeMs + 2000 else it.timeMs
        }
    }

    fun toggleInspection() {
        _inspectionEnabled.value = !_inspectionEnabled.value
        prefs.edit().putBoolean("inspection_enabled", _inspectionEnabled.value).apply()
    }

    fun toggleShowAverages() {
        _showAveragesOnTimer.value = !_showAveragesOnTimer.value
        prefs.edit().putBoolean("show_averages_on_timer", _showAveragesOnTimer.value).apply()
    }

    fun toggleInspectionHoldStart() {
        _inspectionHoldStart.value = !_inspectionHoldStart.value
        prefs.edit().putBoolean("inspection_hold_start", _inspectionHoldStart.value).apply()
    }

    fun setHoldTimeMs(ms: Int) {
        _holdTimeMs.value = ms.coerceIn(0, 1000)
        prefs.edit().putInt("hold_time_ms", _holdTimeMs.value).apply()
    }

    // ---- manual cube-state input ----

    fun setInputMode(mode: TimerInputMode) {
        _inputMode.value = mode
        prefs.edit().putString("input_mode", mode.name).apply()
        if (mode == TimerInputMode.SCRAMBLE) {
            generateNewScramble()
        } else {
            _scramble.value = manualScrambleString()
            _lastSolveUsedPrevScramble.value = false
        }
    }

    /** Bulk-apply a full manual state (from the picker). */
    fun setManualColors(colors: IntArray) {
        _manualColors.value = colors.copyOf()
        _scramble.value = manualScrambleString()
    }

    fun setManualColor(faceletIdx: Int, colorIdx: Int) {
        if (faceletIdx !in 0 until 54) return
        val next = _manualColors.value.copyOf()
        next[faceletIdx] = colorIdx.coerceIn(0, 5)
        _manualColors.value = next
        _scramble.value = manualScrambleString()
    }

    fun resetManualColors() {
        _manualColors.value = CubieCube.solvedColors()
        _scramble.value = manualScrambleString()
    }

    private fun manualScrambleString(): String =
        "manual:" + CubieCube.faceStringFromColors(_manualColors.value)

    /**
     * Advances after a recorded solve. In scramble mode this rolls a fresh scramble;
     * in manual mode the custom-state session is one-shot — timing a solve with a
     * manual cube state returns the timer to generated scrambles for the next solve
     * (the manual state stays in _manualColors, so it can be re-picked anytime).
     */
    private fun prepareNextSolve() {
        if (_inputMode.value == TimerInputMode.SCRAMBLE) {
            generateNewScramble()
        } else {
            setInputMode(TimerInputMode.SCRAMBLE)
        }
    }

    fun toggleAverage(size: Int) {
        _enabledAverages.value = if (size in _enabledAverages.value) {
            _enabledAverages.value - size
        } else {
            _enabledAverages.value + size
        }
        prefs.edit().putString("enabled_averages", _enabledAverages.value.sorted().joinToString(",")).apply()
    }

    fun startInspection() {
        if (!_inspectionEnabled.value) {
            startTimer()
            return
        }
        _isInspecting.value = true
        _inspectionTime.value = 15
        _inspectionPenalty.value = "none"
        _lastSolveTime.value = null
        inspectionJob?.cancel()
        inspectionJob = viewModelScope.launch {
            while (_inspectionTime.value > -3) {
                delay(1000)
                _inspectionTime.value--
                when {
                    _inspectionTime.value == 0 -> _inspectionPenalty.value = "none"
                    _inspectionTime.value == -1 -> _inspectionPenalty.value = "plus2"
                    _inspectionTime.value == -2 -> _inspectionPenalty.value = "plus2"
                    _inspectionTime.value <= -3 -> _inspectionPenalty.value = "dnf"
                }
            }
        }
    }

    fun startFromInspection() {
        inspectionJob?.cancel()
        val penalty = _inspectionPenalty.value
        _isInspecting.value = false

        if (penalty == "dnf") {
            recordSolve(0, -1, _scramble.value)
            _lastSolveTime.value = 0
            _lastSolvePenalty.value = -1
            _inspectionPenalty.value = "none"
            _inspectionTime.value = 15
            prepareNextSolve()
        } else {
            startTimer()
        }
    }

    fun cancelInspection() {
        inspectionJob?.cancel()
        _isInspecting.value = false
        _inspectionTime.value = 15
        _inspectionPenalty.value = "none"
    }

    fun generateNewScramble() {
        val newScramble = ScrambleGenerator.generateScramble(_puzzleType.value)
        val history = _scrambleHistory.value.toMutableList()
        // Add new scramble at current index+1, truncate future
        val insertAt = (_scrambleIndex.value + 1).coerceAtMost(history.size)
        while (history.size > insertAt) history.removeAt(history.size - 1)
        history.add(newScramble)
        _scrambleHistory.value = history
        _scrambleIndex.value = history.size - 1
        _scramble.value = newScramble
        _lastSolveUsedPrevScramble.value = false
    }

    fun previousScramble() {
        val history = _scrambleHistory.value
        val idx = _scrambleIndex.value
        if (idx > 0) {
            _scrambleIndex.value = idx - 1
            _scramble.value = history[idx - 1]
            _lastSolveUsedPrevScramble.value = true
        }
    }

    fun refreshScramble() {
        if (_inputMode.value == TimerInputMode.SCRAMBLE) {
            generateNewScramble()
        }
    }

    fun setCustomScramble(scramble: String) {
        if (scramble.isBlank()) return
        val history = _scrambleHistory.value.toMutableList()
        history.add(scramble)
        _scrambleHistory.value = history
        _scrambleIndex.value = history.size - 1
        _scramble.value = scramble
        _lastSolveUsedPrevScramble.value = true
    }

    fun createSession(name: String, puzzle: PuzzleType = _puzzleType.value) {
        viewModelScope.launch {
            val session = Session(name = name, puzzleType = puzzle.name)
            val id = repository.insertSession(session)
            _currentSessionId.value = id.toInt()
            _puzzleType.value = puzzle
            _sessionPb.value = null
            _scrambleHistory.value = emptyList()
            _scrambleIndex.value = 0
            updateOverallPb()
            generateNewScramble()
        }
    }

    fun selectSession(id: Int) {
        _currentSessionId.value = id
        _lastSolveTime.value = null
        _scrambleHistory.value = emptyList()
        _scrambleIndex.value = 0
        viewModelScope.launch {
            val session = repository.getSessionById(id)
            session?.let {
                _puzzleType.value = PuzzleType.fromString(it.puzzleType)
                updateOverallPb()
                generateNewScramble()
            }
        }
    }

    fun renameSession(id: Int, newName: String) {
        viewModelScope.launch {
            val session = repository.getSessionById(id)
            session?.let {
                repository.updateSession(it.copy(name = newName, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun deleteSession(id: Int) {
        viewModelScope.launch {
            val session = repository.getSessionById(id)
            session?.let {
                repository.deleteSession(it)
                if (_currentSessionId.value == id) {
                    val remaining = _sessions.value.filter { s -> s.id != id }
                    if (remaining.isNotEmpty()) {
                        _currentSessionId.value = remaining.first().id
                        _puzzleType.value = PuzzleType.fromString(remaining.first().puzzleType)
                    } else {
                        val newId = repository.insertSession(Session(name = "default", puzzleType = "3x3"))
                        _currentSessionId.value = newId.toInt()
                        _puzzleType.value = PuzzleType.THREE_BY_THREE
                    }
                    _sessionPb.value = null
                    _lastSolveTime.value = null
                    _scrambleHistory.value = emptyList()
                    _scrambleIndex.value = 0
                    updateOverallPb()
                    generateNewScramble()
                }
            }
        }
    }

    fun armTimer() {
        _timerState.value = TimerState.ARMED
    }

    fun disarmTimer() {
        _timerState.value = TimerState.IDLE
    }

    fun startTimer() {
        _lastSolveTime.value = null
        startTime = System.nanoTime()
        _timerState.value = TimerState.RUNNING
        _elapsed.value = 0
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                _elapsed.value = (System.nanoTime() - startTime) / 1_000_000
                delay(16)
            }
        }
    }

    fun stopTimer() {
        tickerJob?.cancel()
        val wasRunning = _timerState.value == TimerState.RUNNING
        val endTime = System.nanoTime()
        var elapsedMs = (endTime - startTime) / 1_000_000
        _timerState.value = TimerState.IDLE

        val penalty = if (_inspectionEnabled.value && _inspectionPenalty.value == "plus2") 2 else 0
        if (penalty == 2) elapsedMs += 2000

        _elapsed.value = elapsedMs

        // guard: overlapping tap handlers may both call stopTimer; only the first records
        if (wasRunning && elapsedMs in 1..3599999) {
            val currentScramble = _scramble.value
            recordSolve(elapsedMs, penalty, currentScramble)
            _lastSolveTime.value = elapsedMs
            _lastSolvePenalty.value = penalty
            _inspectionPenalty.value = "none"
            _inspectionTime.value = 15
            prepareNextSolve()
        }
    }

    fun resetTimer() {
        _timerState.value = TimerState.IDLE
        _elapsed.value = 0
        _lastSolveTime.value = null
    }

    private fun recordSolve(timeMs: Long, penalty: Int = 0, scramble: String = "") {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            val solve = Solve(sessionId = sessionId, timeMs = timeMs, penalty = penalty, scramble = scramble)
            val id = repository.insertSolve(solve).toInt()
            _lastSolveId.value = id
        }
    }

    fun markLastSolvePenalty(penalty: Int) {
        val id = _lastSolveId.value ?: return
        viewModelScope.launch {
            repository.updatePenalty(id, penalty)
            _lastSolvePenalty.value = penalty
        }
    }

    fun deleteLastSolveFromButton() {
        val id = _lastSolveId.value ?: return
        viewModelScope.launch {
            val solve = repository.getLastSolveForSession(_currentSessionId.value ?: return@launch)
            solve?.let { repository.deleteSolve(it) }
            _lastSolveTime.value = null
            _lastSolveId.value = null
            _lastSolvePenalty.value = 0
            _lastSolveUsedPrevScramble.value = false
        }
    }

    fun deleteSolve(id: Int) {
        viewModelScope.launch {
            val solve = _solves.value.find { it.id == id }
            solve?.let { repository.deleteSolve(it) }
        }
    }

    fun deleteSolves(ids: Set<Int>) {
        viewModelScope.launch {
            val solvesToDelete = _solves.value.filter { it.id in ids }
            solvesToDelete.forEach { repository.deleteSolve(it) }
        }
    }

    fun deleteLastSolve() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            val lastSolve = repository.getLastSolveForSession(sessionId)
            lastSolve?.let { repository.deleteSolve(it) }
        }
    }

    fun markPenalty(id: Int, penalty: Int) {
        viewModelScope.launch {
            repository.updatePenalty(id, penalty)
        }
    }

    suspend fun getSessionForExport(sessionId: Int): Session? {
        return repository.getSessionById(sessionId)
    }

    suspend fun getSolvesForExport(sessionId: Int): List<Solve> {
        return repository.getSolvesBySession(sessionId).first()
    }

    fun importSession(export: BasicsExport) {
        viewModelScope.launch {
            for (exportSession in export.sessions) {
                val puzzle = PuzzleType.fromString(exportSession.puzzleType)
                val session = Session(name = exportSession.name, puzzleType = puzzle.name, createdAt = exportSession.createdAt)
                val sessionId = repository.insertSession(session)
                for (exportSolve in exportSession.solves) {
                    val solve = Solve(sessionId = sessionId.toInt(), timeMs = exportSolve.timeMs, penalty = exportSolve.penalty, createdAt = exportSolve.createdAt)
                    repository.insertSolve(solve)
                }
            }
        }
    }
}

enum class TimerState {
    IDLE, ARMED, RUNNING, STOPPED
}

enum class TimerInputMode {
    SCRAMBLE, MANUAL
}
