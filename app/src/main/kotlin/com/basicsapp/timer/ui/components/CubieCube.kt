package com.basicsapp.timer.ui.components

// 1:1 translation of csTimer's CubieCube from mathlib.js
// All moveCube arrays are pre-computed from csTimer's exact derivation code

data class CubieCube(
    var ca: IntArray = intArrayOf(0, 1, 2, 3, 4, 5, 6, 7),
    var ea: IntArray = intArrayOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
) {
    companion object {
        val cFacelet = arrayOf(
            intArrayOf(8, 9, 20),
            intArrayOf(6, 18, 38),
            intArrayOf(0, 36, 47),
            intArrayOf(2, 45, 11),
            intArrayOf(29, 26, 15),
            intArrayOf(27, 44, 24),
            intArrayOf(33, 53, 42),
            intArrayOf(35, 17, 51)
        )
        val eFacelet = arrayOf(
            intArrayOf(5, 10), intArrayOf(7, 19), intArrayOf(3, 37), intArrayOf(1, 46),
            intArrayOf(32, 16), intArrayOf(28, 25), intArrayOf(30, 43), intArrayOf(34, 52),
            intArrayOf(23, 12), intArrayOf(21, 41), intArrayOf(50, 39), intArrayOf(48, 14)
        )

        // Pre-computed moveCube from csTimer's exact derivation
        // Index: R=0, R2=1, R'=2, F=3, F2=4, F'=5, L=6, L2=7, L'=8, U=9, U2=10, U'=11, B=12, B2=13, B'=14, D=15, D2=16, D'=17
        val moveCube: Array<CubieCube> = arrayOf(
            CubieCube(intArrayOf(20,1,2,8,15,5,6,19), intArrayOf(16,2,4,6,22,10,12,14,8,18,20,0)),        // 0: R
            CubieCube(intArrayOf(7,1,2,4,3,5,6,0), intArrayOf(8,2,4,6,0,10,12,14,22,18,20,16)),           // 1: R2
            CubieCube(intArrayOf(19,1,2,15,8,5,6,20), intArrayOf(22,2,4,6,16,10,12,14,0,18,20,8)),        // 2: R'
            CubieCube(intArrayOf(9,21,2,3,16,12,6,7), intArrayOf(0,19,4,6,8,17,12,14,3,11,20,22)),        // 3: F
            CubieCube(intArrayOf(5,4,2,3,1,0,6,7), intArrayOf(0,10,4,6,8,2,12,14,18,16,20,22)),           // 4: F2
            CubieCube(intArrayOf(12,16,2,3,21,9,6,7), intArrayOf(0,17,4,6,8,19,12,14,11,3,20,22)),        // 5: F'
            CubieCube(intArrayOf(0,10,22,3,4,17,13,7), intArrayOf(0,2,20,6,8,10,18,14,16,4,12,22)),       // 6: L
            CubieCube(intArrayOf(0,6,5,3,4,2,1,7), intArrayOf(0,2,12,6,8,10,4,14,16,20,18,22)),           // 7: L2
            CubieCube(intArrayOf(0,13,17,3,4,22,10,7), intArrayOf(0,2,18,6,8,10,20,14,16,12,4,22)),       // 8: L'
            CubieCube(intArrayOf(3,0,1,2,4,5,6,7), intArrayOf(6,0,2,4,8,10,12,14,16,18,20,22)),           // 9: U
            CubieCube(intArrayOf(2,3,0,1,4,5,6,7), intArrayOf(4,6,0,2,8,10,12,14,16,18,20,22)),           // 10: U2
            CubieCube(intArrayOf(1,2,3,0,4,5,6,7), intArrayOf(2,4,6,0,8,10,12,14,16,18,20,22)),           // 11: U'
            CubieCube(intArrayOf(0,1,11,23,4,5,18,14), intArrayOf(0,2,4,23,8,10,12,21,16,18,7,15)),       // 12: B
            CubieCube(intArrayOf(0,1,7,6,4,5,3,2), intArrayOf(0,2,4,14,8,10,12,6,16,18,22,20)),           // 13: B2
            CubieCube(intArrayOf(0,1,14,18,4,5,23,11), intArrayOf(0,2,4,21,8,10,12,23,16,18,15,7)),       // 14: B'
            CubieCube(intArrayOf(0,1,2,3,5,6,7,4), intArrayOf(0,2,4,6,10,12,14,8,16,18,20,22)),           // 15: D
            CubieCube(intArrayOf(0,1,2,3,6,7,4,5), intArrayOf(0,2,4,6,12,14,8,10,16,18,20,22)),           // 16: D2
            CubieCube(intArrayOf(0,1,2,3,7,4,5,6), intArrayOf(0,2,4,6,14,8,10,12,16,18,20,22))            // 17: D'
        )

        val MOVE_NAME_TO_IDX = mapOf(
            "R" to 0, "R2" to 1, "R'" to 2,
            "F" to 3, "F2" to 4, "F'" to 5,
            "L" to 6, "L2" to 7, "L'" to 8,
            "U" to 9, "U2" to 10, "U'" to 11,
            "B" to 12, "B2" to 13, "B'" to 14,
            "D" to 15, "D2" to 16, "D'" to 17
        )

        fun cornMult(a: CubieCube, b: CubieCube): IntArray {
            val prod = IntArray(8)
            for (corn in 0 until 8) {
                val ori = ((a.ca[b.ca[corn] and 7] shr 3) + (b.ca[corn] shr 3)) % 3
                prod[corn] = (a.ca[b.ca[corn] and 7] and 7) or (ori shl 3)
            }
            return prod
        }

        fun edgeMult(a: CubieCube, b: CubieCube): IntArray {
            val prod = IntArray(12)
            for (ed in 0 until 12) {
                prod[ed] = a.ea[b.ea[ed] shr 1] xor (b.ea[ed] and 1)
            }
            return prod
        }

        fun cubeMult(a: CubieCube, b: CubieCube): CubieCube {
            return CubieCube(cornMult(a, b), edgeMult(a, b))
        }

        fun applyMove(cc: CubieCube, moveStr: String): CubieCube {
            val idx = MOVE_NAME_TO_IDX[moveStr] ?: return cc
            return cubeMult(cc, moveCube[idx])
        }

        fun applyScramble(scramble: String): CubieCube {
            var cc = CubieCube()
            val moves = scramble.split(" ").filter { it.isNotBlank() }
            for (move in moves) {
                cc = applyMove(cc, move)
            }
            return cc
        }

        fun toFaceCube(cc: CubieCube): String {
            val f = IntArray(54) { it }
            for (c in 0 until 8) {
                val j = cc.ca[c] and 0x7
                val ori = cc.ca[c] shr 3
                for (n in 0 until 3) {
                    f[cFacelet[c][(n + ori) % 3]] = cFacelet[j][n]
                }
            }
            for (e in 0 until 12) {
                val j = cc.ea[e] shr 1
                val ori = cc.ea[e] and 1
                for (n in 0 until 2) {
                    f[eFacelet[e][(n + ori) % 2]] = eFacelet[j][n]
                }
            }
            val ts = "URFDLB"
            return f.map { ts[it / 9] }.joinToString("")
        }

        fun toColorIndices(cc: CubieCube): IntArray {
            val faceletStr = toFaceCube(cc)
            return IntArray(54) { "URFDLB".indexOf(faceletStr[it]) }
        }
    }
}
