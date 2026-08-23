package com.basicsapp.timer.utils

import com.basicsapp.timer.data.models.Solve
import kotlin.math.ceil
import kotlin.math.roundToLong
import kotlin.math.sqrt

data class SessionStats(
    val best: Long?,
    val worst: Long?,
    val mean: Long?,
    val median: Long?,
    val stdDev: Double,
    val ao5: Long?,
    val ao12: Long?,
    val ao50: Long?,
    val ao100: Long?,
    val ao200: Long?,
    val ao500: Long?,
    val totalSolves: Int,
    val dnfCount: Int,
    val sessionTime: Long
)

object StatsCalculator {

    /**
     * Sentinel value representing a DNF result inside stats.
     * Mirrors csTimer, where a DNF sorts after every real time ("worse than everything").
     */
    const val DNF = Long.MAX_VALUE

    /** Effective time in ms: +2 adds 2000ms; DNF becomes the DNF sentinel. */
    private fun effective(solve: Solve): Long = when (solve.penalty) {
        -1 -> DNF
        2 -> solve.timeMs + 2000
        else -> solve.timeMs
    }

    private fun isDnf(t: Long): Boolean = t == DNF

    /**
     * csTimer's default trim setting (`trim = "p5"`, `trimr = "a"`):
     * drop ceil(5%) of the fastest and ceil(5%) of the slowest solves.
     * ao5 -> 1/1, ao12 -> 1/1, ao50 -> 3/3, ao100 -> 5/5, ao200 -> 10/10, ao500 -> 25/25.
     */
    private fun trimCount(n: Int): Int = ceil(n * 0.05).toInt()

    /**
     * csTimer-compatible average over the last `size` solves:
     *  - the window is the last `size` solves INCLUDING DNFs
     *  - `trimCount(size)` fastest and slowest solves are removed
     *  - the average is DNF iff there are more DNFs than can be trimmed off the slow end
     *    (ao5/ao12: 2+ DNFs -> DNF; one DNF is fine and gets trimmed as the worst)
     * Returns null when there are fewer than `size` solves.
     */
    private fun computeAverage(solves: List<Solve>, size: Int): Long? {
        if (solves.size < size) return null
        val values = solves.takeLast(size).map { effective(it) }
        val dnfCount = values.count { isDnf(it) }
        val trim = trimCount(size)
        if (size - 2 * trim <= 0) return null
        if (dnfCount > trim) return DNF
        val kept = values.sorted().subList(trim, size - trim)
        return (kept.sum().toDouble() / kept.size).roundToLong()
    }

    fun getSessionStats(solves: List<Solve>): SessionStats {
        val dnfCount = solves.count { it.penalty == -1 }

        if (solves.isEmpty()) {
            return SessionStats(null, null, null, null, 0.0, null, null, null, null, null, null, 0, 0, 0)
        }

        val times = solves.filter { it.penalty != -1 }.map { effective(it) }
        val sorted = times.sorted()

        val best = sorted.firstOrNull() ?: DNF
        val worst = sorted.lastOrNull() ?: DNF
        val mean = if (sorted.isNotEmpty()) times.average().roundToLong() else DNF
        val median = when {
            sorted.isEmpty() -> null
            sorted.size % 2 == 0 -> (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
            else -> sorted[sorted.size / 2]
        }

        // Sample standard deviation (n-1 denominator), matching csTimer.
        val stdDev = if (times.size >= 2) {
            val meanD = times.average()
            sqrt(times.sumOf { (it - meanD) * (it - meanD) } / (times.size - 1))
        } else {
            0.0
        }

        val sessionTime = times.sum()

        return SessionStats(
            best = best,
            worst = worst,
            mean = mean,
            median = median,
            stdDev = stdDev,
            ao5 = computeAverage(solves, 5),
            ao12 = computeAverage(solves, 12),
            ao50 = computeAverage(solves, 50),
            ao100 = computeAverage(solves, 100),
            ao200 = computeAverage(solves, 200),
            ao500 = computeAverage(solves, 500),
            totalSolves = solves.size,
            dnfCount = dnfCount,
            sessionTime = sessionTime
        )
    }
}
