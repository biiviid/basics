package com.basicsapp.timer.ui.components

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class CubieCubeTest {

    @Test
    fun `solved colors match the solved cube`() {
        assertArrayEquals(CubieCube.toColorIndices(CubieCube()), CubieCube.solvedColors())
    }

    @Test
    fun `face string round-trips to the same colors`() {
        val colors = IntArray(54) { (it * 3) % 6 }
        val str = CubieCube.faceStringFromColors(colors)
        assertEquals(54, str.length)
        assertArrayEquals(colors, CubieCube.colorsFromFaceString(str))
    }

    @Test
    fun `solved face string is uniform per face`() {
        assertEquals(
            "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB",
            CubieCube.faceStringFromColors(CubieCube.solvedColors())
        )
    }

    @Test
    fun `empty scramble leaves the cube solved`() {
        assertEquals(
            "UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB",
            CubieCube.toFaceCube(CubieCube.applyScramble(""))
        )
    }
}