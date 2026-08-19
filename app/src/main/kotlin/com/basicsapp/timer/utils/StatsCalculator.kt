package com.basicsapp.timer.utils

import com.basicsapp.timer.data.models.Solve
import kotlin.math.pow
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
    fun getSessionStats(solves: List<Solve>): SessionStats {
        val valid = solves.filter { it.penalty != -1 }
        if (valid.isEmpty()) {
            return SessionStats(null, null, null, null, 0.0, null, null, null, null, null, null, solves.size, solves.count { it.penalty == -1 }, 0)
        }

        val times = valid.map { if (it.penalty == 2) it.timeMs + 2000 else it.timeMs }
        val sorted = times.sorted()

        val best = sorted.first()
        val worst = sorted.last()
        val mean = times.average().toLong()
        val median = if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        } else {
            sorted[sorted.size / 2]
        }

        val variance = times.map { (it - mean).toDouble().pow(2) }.average()
        val stdDev = sqrt(variance)

        val sessionTime = times.sum()

        val ao5 = if (valid.size >= 5) {
            val last5 = valid.takeLast(5).map { if (it.penalty == 2) it.timeMs + 2000 else it.timeMs }.sorted()
            (last5[1] + last5[2] + last5[3]) / 3
        } else null

        val ao12 = if (valid.size >= 12) {
            val last12 = valid.takeLast(12).map { if (it.penalty == 2) it.timeMs + 2000 else it.timeMs }.sorted()
            last12.subList(1, 11).average().toLong()
        } else null

        val ao50 = if (valid.size >= 50) {
            val last50 = valid.takeLast(50).map { if (it.penalty == 2) it.timeMs + 2000 else it.timeMs }.sorted()
            last50.subList(1, 49).average().toLong()
        } else null

        val ao100 = if (valid.size >= 100) {
            val last100 = valid.takeLast(100).map { if (it.penalty == 2) it.timeMs + 2000 else it.timeMs }.sorted()
            last100.subList(1, 99).average().toLong()
        } else null

        val ao200 = if (valid.size >= 200) {
            val last200 = valid.takeLast(200).map { if (it.penalty == 2) it.timeMs + 2000 else it.timeMs }.sorted()
            last200.subList(1, 199).average().toLong()
        } else null

        val ao500 = if (valid.size >= 500) {
            val last500 = valid.takeLast(500).map { if (it.penalty == 2) it.timeMs + 2000 else it.timeMs }.sorted()
            last500.subList(1, 499).average().toLong()
        } else null

        return SessionStats(best, worst, mean, median, stdDev, ao5, ao12, ao50, ao100, ao200, ao500, solves.size, solves.count { it.penalty == -1 }, sessionTime)
    }
}
