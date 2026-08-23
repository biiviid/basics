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

    // ---- manual state validation (buildCubieFromColors / validateColors) ----

    @Test
    fun `solved colors validate as solvable`() {
        assertEquals(CubieCube.ValidationResult.VALID, CubieCube.validateColors(CubieCube.solvedColors()))
    }

    @Test
    fun `real scramble colors validate as solvable`() {
        val cc = CubieCube.applyScramble("U R F D L B U2 R' F2 L D' B U R' F2 L2 D B2 R' U'")
        assertEquals(CubieCube.ValidationResult.VALID, CubieCube.validateColors(CubieCube.toColorIndices(cc)))
    }

    @Test
    fun `colors round-trip to the same cubie`() {
        val cc = CubieCube.applyScramble("R U R' U' F' L' B' D B2 L2 U2 R2 F D2 B R' U F2 L")
        val rebuilt = CubieCube.buildCubieFromColors(CubieCube.toColorIndices(cc))!!
        assertArrayEquals(cc.ca, rebuilt.ca)
        assertArrayEquals(cc.ea, rebuilt.ea)
    }

    @Test
    fun `wrong color count is rejected`() {
        val colors = CubieCube.solvedColors().copyOf()
        colors[0] = 1 // U face: one sticker turned red → U=8, R=10
        assertEquals(CubieCube.ValidationResult.COLOR_COUNT, CubieCube.validateColors(colors))
    }

    @Test
    fun `wrong center color is rejected`() {
        val colors = CubieCube.solvedColors().copyOf()
        colors[4] = 1 // U center painted red
        assertEquals(CubieCube.ValidationResult.CENTER_COLOR, CubieCube.validateColors(colors))
    }

    @Test
    fun `impossible corner colors are rejected`() {
        val colors = CubieCube.solvedColors().copyOf()
        // corner 0 (URF, facelets [8,9,20]): R sticker → D, and corner 6's D sticker → R (keeps counts balanced)
        colors[9] = 3
        colors[33] = 1
        assertEquals(CubieCube.ValidationResult.IMPOSSIBLE_PIECE, CubieCube.validateColors(colors))
    }

    @Test
    fun `duplicate corner piece is rejected`() {
        val colors = CubieCube.solvedColors().copyOf()
        // corner 0 becomes UFL (piece 1, same as corner 1); rebalance by turning corner 2's L sticker into R
        colors[9] = 4
        colors[36] = 1
        assertEquals(CubieCube.ValidationResult.DUPLICATE_PIECE, CubieCube.validateColors(colors))
    }

    @Test
    fun `twisted corner is rejected`() {
        val colors = CubieCube.solvedColors().copyOf()
        // rotate corner 0's (URF) stickers: slot0=U shows R, slot1=R shows F, slot2=F shows U
        colors[8] = 1
        colors[9] = 2
        colors[20] = 0
        assertEquals(CubieCube.ValidationResult.CORNER_ORIENTATION, CubieCube.validateColors(colors))
    }

    @Test
    fun `flipped edge is rejected`() {
        val colors = CubieCube.solvedColors().copyOf()
        // flip edge 0 (UR): slot0=U shows R, slot1=R shows U
        colors[5] = 1
        colors[10] = 0
        assertEquals(CubieCube.ValidationResult.EDGE_ORIENTATION, CubieCube.validateColors(colors))
    }

    @Test
    fun `edge swap with mismatched parity is rejected`() {
        val colors = CubieCube.solvedColors().copyOf()
        // swap UR (piece 0) and UF (piece 1) → edge permutation parity 1, corner parity 0
        colors[10] = 2
        colors[19] = 1
        assertEquals(CubieCube.ValidationResult.PERMUTATION_PARITY, CubieCube.validateColors(colors))
    }
}