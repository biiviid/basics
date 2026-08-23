package com.basicsapp.timer.utils

import com.basicsapp.timer.ui.components.CubieCube

/**
 * Two-phase Kociemba solver for the 3x3, built directly on the app's CubieCube model
 * (csTimer's exact convention, so move semantics match applyScramble 1:1).
 *
 * Given a cube state it returns the WCA move sequence that reconstructs that state from
 * solved -- i.e. a "scramble" that reproduces a manually-painted cube state. This is what
 * makes a manual solve portable to other timers (they get a real scramble instead of our
 * "manual:..." facelet tag).
 *
 * Algorithm:
 *   Phase 1 (any of the 18 face moves): reach G1 = { twist == 0, flip == 0, the four
 *     UD-slice edges are in the middle layer positions 8..11 }.
 *   Phase 2 (G1 moves U, D, R2, L2, F2, B2 -- these preserve all three phase-1
 *     invariants): solve completely.
 * Both phases are IDA* searches over one shared move budget; the result is proven by the
 * existing CubieCube model itself (applyScramble(scrambleFor(cc)) == cc).
 *
 * Move tables are derived from CubieCube.moveCube and pruning tables are computed by BFS
 * at first use, then cached for the process lifetime.
 */
object KociembaSolver {

    // move indices match CubieCube.moveCube: R,R2,R';F,F2,F';L,L2,L';U,U2,U';B,B2,B';D,D2,D'
    private val MOVE_NAMES = arrayOf("R", "R2", "R'", "F", "F2", "F'", "L", "L2", "L'", "U", "U2", "U'", "B", "B2", "B'", "D", "D2", "D'")
    private val INV_MOVE = intArrayOf(2, 1, 0, 5, 4, 3, 8, 7, 6, 11, 10, 9, 14, 13, 12, 17, 16, 15)
    private val OPP_FACE = intArrayOf(2, 4, 0, 5, 1, 3) // R<->L, F<->B, U<->D
    private val PHASE2_MOVES = intArrayOf(1, 4, 7, 9, 10, 11, 13, 15, 16, 17) // R2,F2,L2,U,U2,U',B2,D,D2,D'
    private val FACT = intArrayOf(1, 1, 2, 6, 24, 120, 720, 5040, 40320)

    private const val N_TWIST = 2187
    private const val N_FLIP = 2048
    private const val N_SLICE = 495
    private const val N_CORN = 40320
    private const val N_EDGE = 40320
    private const val N_UD = 24
    private const val MAX_PHASE1 = 13
    private const val MAX_PHASE2 = 20
    private const val NODE_LIMIT = 100_000_000

    private const val SLICE_MASK = 0x0F00 // edge positions 8..11 = the UD slice

    private lateinit var sliceMaskToIndex: IntArray
    private lateinit var sliceIndexToMask: IntArray
    private var sliceGoal = 0

    private lateinit var twistMove: IntArray
    private lateinit var flipMove: IntArray
    private lateinit var sliceMove: IntArray
    private lateinit var cornerMove: IntArray
    private lateinit var edgeMove: IntArray
    private lateinit var udMove: IntArray

    private lateinit var prunTwistSlice: ByteArray
    private lateinit var prunFlipSlice: ByteArray
    private lateinit var prunCornerUd: ByteArray
    private lateinit var prunEdgeUd: ByteArray

    private var initialized = false
    private var nodes = 0
    private val path = IntArray(MAX_PHASE1 + MAX_PHASE2 + 1)
    private var pathLen = 0

    /** True once the pruning tables have been built for this process. */
    fun isInitialized(): Boolean = initialized

    /**
     * Returns a WCA scramble string that, applied to a solved cube, reproduces cc.
     * If the tables are not built yet, [onInitProgress] is called with 0f..1f as each
     * build phase completes (on the caller's thread).
     */
    fun scrambleFor(cc: CubieCube, onInitProgress: ((Float) -> Unit)? = null): String? {
        initTables(onInitProgress)
        val solution = synchronized(this) { solvePath(cc) } ?: return null
        if (solution.isEmpty()) return ""
        return solution.asReversed().joinToString(" ") { MOVE_NAMES[INV_MOVE[it]] }
    }


    // ---- coordinates (all derived from the existing CubieCube convention) ----

    private fun twistOf(cc: CubieCube): Int {
        var t = 0
        for (i in 6 downTo 0) t = t * 3 + (cc.ca[i] shr 3)
        return t
    }

    private fun flipOf(cc: CubieCube): Int {
        var f = 0
        for (i in 10 downTo 0) f = f * 2 + (cc.ea[i] and 1)
        return f
    }

    private fun sliceMaskOf(cc: CubieCube): Int {
        var mask = 0
        for (p in 0 until 12) if ((cc.ea[p] shr 1) >= 8) mask = mask or (1 shl p)
        return mask
    }

    private fun sliceIndexOf(cc: CubieCube): Int = sliceMaskToIndex[sliceMaskOf(cc)]

    private fun lehmer(perm: IntArray): Int {
        var code = 0
        for (i in perm.indices) {
            var smaller = 0
            for (j in i + 1 until perm.size) if (perm[j] < perm[i]) smaller++
            code += smaller * FACT[perm.size - 1 - i]
        }
        return code
    }

    private fun cornerPermOf(cc: CubieCube): Int = lehmer(IntArray(8) { cc.ca[it] and 7 })
    private fun edgePermOf(cc: CubieCube): Int = lehmer(IntArray(8) { cc.ea[it] shr 1 }) // G1: positions 0..7 hold pieces 0..7
    private fun sliceOrderOf(cc: CubieCube): Int = lehmer(IntArray(4) { (cc.ea[8 + it] shr 1) - 8 }) // G1: positions 8..11 hold pieces 8..11

    // ---- representative state builders (identity except for one coordinate) ----

    private fun ccFromTwist(t: Int): CubieCube {
        var x = t
        val ori = IntArray(8)
        var sum = 0
        for (i in 0 until 7) { ori[i] = x % 3; x /= 3; sum += ori[i] }
        ori[7] = (3 - sum % 3) % 3
        return CubieCube(IntArray(8) { it or (ori[it] shl 3) }, IntArray(12) { it shl 1 })
    }

    private fun ccFromFlip(f: Int): CubieCube {
        var x = f
        val ori = IntArray(12)
        var sum = 0
        for (i in 0 until 11) { ori[i] = x % 2; x /= 2; sum += ori[i] }
        ori[11] = sum % 2
        return CubieCube(IntArray(8) { it }, IntArray(12) { (it shl 1) or ori[it] })
    }

    private fun ccFromSlice(s: Int): CubieCube {
        val mask = sliceIndexToMask[s]
        val ea = IntArray(12)
        var ns = 0
        var sl = 8
        for (p in 0 until 12) {
            ea[p] = if (mask and (1 shl p) != 0) sl++ shl 1 else ns++ shl 1
        }
        return CubieCube(IntArray(8) { it }, ea)
    }

    private fun ccFromCornerPerm(code: Int): CubieCube {
        val perm = permFromLehmer(code, 8)
        return CubieCube(IntArray(8) { perm[it] }, IntArray(12) { it shl 1 })
    }

    private fun ccFromEdgePerm(code: Int): CubieCube {
        val perm = permFromLehmer(code, 8)
        val ea = IntArray(12)
        for (i in 0 until 8) ea[i] = perm[i] shl 1
        for (i in 0 until 4) ea[8 + i] = (8 + i) shl 1
        return CubieCube(IntArray(8) { it }, ea)
    }

    private fun ccFromSliceOrder(code: Int): CubieCube {
        val perm = permFromLehmer(code, 4)
        val ea = IntArray(12)
        for (i in 0 until 8) ea[i] = i shl 1
        for (i in 0 until 4) ea[8 + i] = (8 + perm[i]) shl 1
        return CubieCube(IntArray(8) { it }, ea)
    }

    private fun permFromLehmer(code: Int, n: Int): IntArray {
        val perm = IntArray(n)
        val used = BooleanArray(n)
        var c = code
        for (i in 0 until n) {
            val f = FACT[n - 1 - i]
            val idx = c / f
            c %= f
            var count = -1
            for (v in 0 until n) {
                if (!used[v]) count++
                if (count == idx) { perm[i] = v; used[v] = true; break }
            }
        }
        return perm
    }
    // ---- table building ----

    private fun initTables(onProgress: ((Float) -> Unit)? = null) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            // 1 (slice maps) + 6 (move tables) + 4 (prune tables) build phases
            val phases = 11f
            var done = 0f
            fun tick() { done++; onProgress?.invoke((done / phases).coerceIn(0f, 1f)) }

            sliceMaskToIndex = IntArray(4096) { -1 }
            val masks = ArrayList<Int>(N_SLICE)
            for (a in 0 until 9) for (b in a + 1 until 10) for (c in b + 1 until 11) for (d in c + 1 until 12) {
                val m = (1 shl a) or (1 shl b) or (1 shl c) or (1 shl d)
                sliceMaskToIndex[m] = masks.size
                masks.add(m)
            }
            sliceIndexToMask = masks.toIntArray()
            sliceGoal = sliceMaskToIndex[SLICE_MASK]
            tick()

            twistMove = IntArray(N_TWIST * 18)
            for (m in 0 until 18) {
                val mc = CubieCube.moveCube[m]
                for (t in 0 until N_TWIST) twistMove[t * 18 + m] = twistOf(CubieCube.cubeMult(ccFromTwist(t), mc))
            }
            tick()

            flipMove = IntArray(N_FLIP * 18)
            for (m in 0 until 18) {
                val mc = CubieCube.moveCube[m]
                for (f in 0 until N_FLIP) flipMove[f * 18 + m] = flipOf(CubieCube.cubeMult(ccFromFlip(f), mc))
            }
            tick()

            sliceMove = IntArray(N_SLICE * 18)
            for (m in 0 until 18) {
                val mc = CubieCube.moveCube[m]
                for (s in 0 until N_SLICE) sliceMove[s * 18 + m] = sliceIndexOf(CubieCube.cubeMult(ccFromSlice(s), mc))
            }
            tick()

            cornerMove = IntArray(N_CORN * 18)
            for (m in 0 until 18) {
                val mc = CubieCube.moveCube[m]
                for (c in 0 until N_CORN) cornerMove[c * 18 + m] = cornerPermOf(CubieCube.cubeMult(ccFromCornerPerm(c), mc))
            }
            tick()

            edgeMove = IntArray(N_EDGE * 18)
            for (m in 0 until 18) {
                val mc = CubieCube.moveCube[m]
                for (e in 0 until N_EDGE) edgeMove[e * 18 + m] = edgePermOf(CubieCube.cubeMult(ccFromEdgePerm(e), mc))
            }
            tick()

            udMove = IntArray(N_UD * 18)
            for (m in 0 until 18) {
                val mc = CubieCube.moveCube[m]
                for (u in 0 until N_UD) udMove[u * 18 + m] = sliceOrderOf(CubieCube.cubeMult(ccFromSliceOrder(u), mc))
            }
            tick()

            val allMoves = IntArray(18) { it }
            prunTwistSlice = buildPrune(N_TWIST, N_SLICE, twistMove, sliceMove, allMoves, 0, sliceGoal)
            tick()
            prunFlipSlice = buildPrune(N_FLIP, N_SLICE, flipMove, sliceMove, allMoves, 0, sliceGoal)
            tick()
            prunCornerUd = buildPrune(N_CORN, N_UD, cornerMove, udMove, PHASE2_MOVES, 0, 0)
            tick()
            prunEdgeUd = buildPrune(N_EDGE, N_UD, edgeMove, udMove, PHASE2_MOVES, 0, 0)
            tick()

            initialized = true
        }
    }

    private fun buildPrune(
        nA: Int, nB: Int, tableA: IntArray, tableB: IntArray, moves: IntArray, goalA: Int, goalB: Int
    ): ByteArray {
        val size = nA * nB
        val prun = ByteArray(size) { -1 }
        val qA = IntArray(size)
        val qB = IntArray(size)
        var head = 0
        var tail = 0
        prun[goalA * nB + goalB] = 0
        qA[tail] = goalA; qB[tail] = goalB; tail++
        while (head < tail) {
            val a = qA[head]
            val b = qB[head]
            head++
            val d = prun[a * nB + b].toInt()
            for (m in moves) {
                val im = INV_MOVE[m]
                val na = tableA[a * 18 + im]
                val nb = tableB[b * 18 + im]
                val idx = na * nB + nb
                if (prun[idx] < 0) {
                    prun[idx] = (d + 1).toByte()
                    qA[tail] = na; qB[tail] = nb; tail++
                }
            }
        }
        return prun
    }
    // ---- IDA* search ----
    // Phase 1 (any face move) reaches G1 = { twist == 0, flip == 0, UD-slice in place }:
    // every state reaches G1 in at most 12 moves. Phase 2 (G1 moves U, D, R2, L2, F2, B2)
    // solves completely: every G1 state solves in at most 18 moves. Decoupling the two
    // budgets keeps each phase's search tree bounded and small.
    // The returned move list, applied to cc, yields the solved cube.

    private fun solvePath(cc: CubieCube): List<Int>? {
        var phase1Len = 0
        var inG1 = false
        for (d1 in 0..MAX_PHASE1) {
            nodes = 0
            pathLen = 0
            if (phase1ToG1(cc, d1, -1, -1)) { phase1Len = pathLen; inG1 = true; break }
            if (nodes >= NODE_LIMIT) return null
        }
        if (!inG1) return null

        var g1 = cc
        for (i in 0 until phase1Len) g1 = CubieCube.cubeMult(g1, CubieCube.moveCube[path[i]])

        for (d2 in 0..MAX_PHASE2) {
            nodes = 0
            pathLen = phase1Len
            if (phase2ToSolved(g1, d2, -1, -1)) return (0 until pathLen).map { path[it] }
            if (nodes >= NODE_LIMIT) return null
        }
        return null
    }

    private fun phase1ToG1(cc: CubieCube, depth: Int, lastFace: Int, secondLastFace: Int): Boolean {
        if (++nodes > NODE_LIMIT) return false
        val twist = twistOf(cc)
        val flip = flipOf(cc)
        val slice = sliceIndexOf(cc)
        if (twist == 0 && flip == 0 && slice == sliceGoal) return true
        if (depth == 0) return false
        val h = maxOf(
            prunTwistSlice[twist * N_SLICE + slice].takeIf { it >= 0 }?.toInt() ?: 0,
            prunFlipSlice[flip * N_SLICE + slice].takeIf { it >= 0 }?.toInt() ?: 0
        )
        if (depth < h) return false
        for (m in 0 until 18) {
            val face = m / 3
            if (lastFace >= 0) {
                if (face == lastFace) continue
                if (face == OPP_FACE[lastFace] && face == secondLastFace) continue
            }
            path[pathLen++] = m
            if (phase1ToG1(CubieCube.cubeMult(cc, CubieCube.moveCube[m]), depth - 1, face, lastFace)) return true
            pathLen--
        }
        return false
    }

    private fun phase2ToSolved(cc: CubieCube, depth: Int, lastFace: Int, secondLastFace: Int): Boolean {
        if (++nodes > NODE_LIMIT) return false
        val corner = cornerPermOf(cc)
        val edge = edgePermOf(cc)
        val ud = sliceOrderOf(cc)
        if (corner == 0 && edge == 0 && ud == 0) return true
        if (depth == 0) return false
        val h = maxOf(
            prunCornerUd[corner * N_UD + ud].takeIf { it >= 0 }?.toInt() ?: 0,
            prunEdgeUd[edge * N_UD + ud].takeIf { it >= 0 }?.toInt() ?: 0
        )
        if (depth < h) return false
        for (m in PHASE2_MOVES) {
            val face = m / 3
            if (lastFace >= 0) {
                if (face == lastFace) continue
                if (face == OPP_FACE[lastFace] && face == secondLastFace) continue
            }
            path[pathLen++] = m
            if (phase2ToSolved(CubieCube.cubeMult(cc, CubieCube.moveCube[m]), depth - 1, face, lastFace)) return true
            pathLen--
        }
        return false
    }
}