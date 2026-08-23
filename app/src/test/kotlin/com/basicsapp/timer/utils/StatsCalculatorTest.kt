package com.basicsapp.timer.utils

import com.basicsapp.timer.data.models.Solve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsCalculatorTest {

    private fun solve(ms: Long, penalty: Int = 0) =
        Solve(sessionId = 1, timeMs = ms, penalty = penalty)

    @Test
    fun `ao5 removes best and worst`() {
        val stats = StatsCalculator.getSessionStats((1L..5L).map { solve(it * 1000) })
        assertEquals(3000L, stats.ao5)
    }

    @Test
    fun `ao5 with one DNF still counts - DNF is trimmed as worst`() {
        val solves = listOf(solve(1000), solve(2000), solve(3000), solve(4000), solve(99999, penalty = -1))
        assertEquals(3000L, StatsCalculator.getSessionStats(solves).ao5)
    }

    @Test
    fun `ao5 with two DNFs is DNF`() {
        val solves = listOf(solve(1000), solve(2000), solve(3000), solve(1, penalty = -1), solve(2, penalty = -1))
        assertEquals(StatsCalculator.DNF, StatsCalculator.getSessionStats(solves).ao5)
    }

    @Test
    fun `ao12 with one DNF counts`() {
        val solves = (1L..11L).map { solve(it * 1000) } + solve(99999, penalty = -1)
        assertEquals(6500L, StatsCalculator.getSessionStats(solves).ao12)
    }

    @Test
    fun `ao12 with two DNFs is DNF`() {
        val solves = (1L..10L).map { solve(it * 1000) } + solve(1, penalty = -1) + solve(2, penalty = -1)
        assertEquals(StatsCalculator.DNF, StatsCalculator.getSessionStats(solves).ao12)
    }

    @Test
    fun `ao50 trims 5 percent each side`() {
        val solves = (1L..50L).map { solve(it * 1000) }
        assertEquals(25500L, StatsCalculator.getSessionStats(solves).ao50)
    }

    @Test
    fun `plus two adds 2000ms into the average`() {
        val solves = listOf(solve(1000), solve(1000, penalty = 2), solve(3000), solve(4000), solve(5000))
        // effective values: 1000, 3000(+2), 3000, 4000, 5000 -> trim 1000 and 5000 -> avg(3000,3000,4000)
        assertEquals(3333L, StatsCalculator.getSessionStats(solves).ao5)
    }

    @Test
    fun `mean excludes DNF solves`() {
        val solves = listOf(solve(1000), solve(2000), solve(3000), solve(1, penalty = -1))
        assertEquals(2000L, StatsCalculator.getSessionStats(solves).mean)
    }

    @Test
    fun `standard deviation is sample not population`() {
        val solves = listOf(solve(1000), solve(2000), solve(3000))
        assertEquals(1000.0, StatsCalculator.getSessionStats(solves).stdDev, 1e-9)
    }

    @Test
    fun `best and worst exclude DNF`() {
        val solves = listOf(solve(1000), solve(5000), solve(3000), solve(1, penalty = -1))
        val stats = StatsCalculator.getSessionStats(solves)
        assertEquals(1000L, stats.best)
        assertEquals(5000L, stats.worst)
    }

    @Test
    fun `all DNF session reports DNF everywhere`() {
        val solves = List(5) { solve(1000, penalty = -1) }
        val stats = StatsCalculator.getSessionStats(solves)
        assertEquals(StatsCalculator.DNF, stats.best)
        assertEquals(StatsCalculator.DNF, stats.worst)
        assertEquals(StatsCalculator.DNF, stats.mean)
        assertEquals(StatsCalculator.DNF, stats.ao5)
        assertEquals(5, stats.dnfCount)
        assertEquals(5, stats.totalSolves)
    }

    @Test
    fun `empty session has no stats`() {
        val stats = StatsCalculator.getSessionStats(emptyList())
        assertNull(stats.best)
        assertNull(stats.ao5)
        assertEquals(0.0, stats.stdDev, 0.0)
        assertEquals(0, stats.totalSolves)
    }

    @Test
    fun `not enough solves gives null average`() {
        val solves = listOf(solve(1000), solve(2000), solve(3000))
        val stats = StatsCalculator.getSessionStats(solves)
        assertNull(stats.ao5)
        assertNull(stats.ao12)
    }
}
