package com.basicsapp.timer.utils

import com.basicsapp.timer.ui.components.CubieCube
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class KociembaSolverTest {

    private fun assertReproduces(cc: CubieCube) {
        val scramble = KociembaSolver.scrambleFor(cc)
        assertNotNull(scramble)
        val rebuilt = CubieCube.applyScramble(scramble!!)
        assertArrayEquals(cc.ca, rebuilt.ca)
        assertArrayEquals(cc.ea, rebuilt.ea)
    }

    @Test
    fun `solved cube yields empty scramble`() {
        assertEquals("", KociembaSolver.scrambleFor(CubieCube()))
    }

    @Test
    fun `every single move round-trips`() {
        for (move in listOf("R", "R2", "R'", "F", "F2", "F'", "L", "L2", "L'", "U", "U2", "U'", "B", "B2", "B'", "D", "D2", "D'")) {
            assertReproduces(CubieCube.applyScramble(move))
        }
    }

    @Test
    fun `generated scrambles round-trip`() {
        repeat(20) {
            val s = ScrambleGenerator.generateScramble(PuzzleType.THREE_BY_THREE)
            assertReproduces(CubieCube.applyScramble(s))
        }
    }

    @Test
    fun `manual state round-trips through the solver`() {
        val colors = CubieCube.toColorIndices(CubieCube.applyScramble("R U R' U' F2 L2 B2 D R2 U' F2 L"))
        val cc = CubieCube.buildCubieFromColors(colors)!!
        val scramble = KociembaSolver.scrambleFor(cc)
        assertNotNull(scramble)
        val rebuilt = CubieCube.applyScramble(scramble!!)
        assertArrayEquals(colors, CubieCube.toColorIndices(rebuilt))
    }
}