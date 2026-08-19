package com.basicsapp.timer.utils

import kotlin.random.Random

enum class PuzzleType(val displayName: String, val scrambleLength: Int) {
    TWO_BY_TWO("2x2", 11),
    THREE_BY_THREE("3x3", 20),
    FOUR_BY_FOUR("4x4", 44),
    FIVE_BY_FIVE("5x5", 60),
    SIX_BY_SIX("6x6", 80),
    SEVEN_BY_SEVEN("7x7", 100),
    SKEWB("Skewb", 11),
    PYRAMINX("Pyraminx", 11),
    MEGAMINX("Megaminx", 11),
    SQUARE_ONE("Square-1", 13),
    CLOCK("Clock", 14);

    companion object {
        fun fromString(value: String): PuzzleType {
            return entries.find { it.name == value || it.displayName == value } ?: THREE_BY_THREE
        }
    }
}

object ScrambleGenerator {

    private val NxN_MOVES = listOf("R", "L", "U", "D", "F", "B")
    private val NxN_MODIFIERS = listOf("", "'", "2")
    private val BIG_MODIFIERS = listOf("", "'", "2", "w")

    fun generateScramble(puzzle: PuzzleType): String {
        val result = when (puzzle) {
            PuzzleType.TWO_BY_TWO -> generateNxNScramble(2, puzzle.scrambleLength)
            PuzzleType.THREE_BY_THREE -> generate3x3Scramble()
            PuzzleType.FOUR_BY_FOUR -> generateNxNScramble(4, puzzle.scrambleLength)
            PuzzleType.FIVE_BY_FIVE -> generateNxNScramble(5, puzzle.scrambleLength)
            PuzzleType.SIX_BY_SIX -> generateBigCubeScramble(6, puzzle.scrambleLength)
            PuzzleType.SEVEN_BY_SEVEN -> generateBigCubeScramble(7, puzzle.scrambleLength)
            PuzzleType.SKEWB -> generateSkewbScramble()
            PuzzleType.PYRAMINX -> generatePyraminxScramble()
            PuzzleType.MEGAMINX -> generateMegaminxScramble()
            PuzzleType.SQUARE_ONE -> generateSquareOneScramble()
            PuzzleType.CLOCK -> generateClockScramble()
        }
        return result
    }

    private fun generate3x3Scramble(): String {
        val moves = mutableListOf<String>()
        var lastFace = ""
        var secondLastFace = ""

        while (moves.size < 20) {
            val face = NxN_MOVES[Random.nextInt(6)]
            if (face == lastFace) continue
            if (face == getOppositeFace(lastFace) && face == secondLastFace) continue

            val modifier = NxN_MODIFIERS[Random.nextInt(3)]
            moves.add("$face$modifier")
            secondLastFace = lastFace
            lastFace = face
        }
        return moves.joinToString(" ")
    }

    private fun generateNxNScramble(n: Int, length: Int): String {
        val moves = if (n <= 3) NxN_MOVES else NxN_MOVES + NxN_MOVES.map { "${it}w" }
        val modifiers = NxN_MODIFIERS
        val result = mutableListOf<String>()
        var lastFace = ""
        var secondLastFace = ""

        while (result.size < length) {
            val face = moves[Random.nextInt(moves.size)]
            val baseFace = face.replace("w", "")
            if (baseFace == lastFace) continue
            if (baseFace == getOppositeFace(lastFace) && baseFace == secondLastFace) continue

            val modifier = modifiers[Random.nextInt(modifiers.size)]
            result.add("$face$modifier")
            secondLastFace = lastFace
            lastFace = baseFace
        }
        return result.joinToString(" ")
    }

    private fun generateBigCubeScramble(n: Int, length: Int): String {
        val innerLayers = (2 until n).map { "${it}w" }
        val allMoves = NxN_MOVES + innerLayers
        val modifiers = BIG_MODIFIERS
        val result = mutableListOf<String>()
        var lastFace = ""

        while (result.size < length) {
            val move = allMoves[Random.nextInt(allMoves.size)]
            val baseFace = move.replace("w", "").replace(Regex("\\d+w"), "")
            if (baseFace == lastFace) continue

            val modifier = modifiers[Random.nextInt(modifiers.size)]
            result.add("$move$modifier")
            lastFace = baseFace
        }
        return result.joinToString(" ")
    }

    private fun generateSkewbScramble(): String {
        val moves = listOf("R", "L", "U", "B")
        val modifiers = listOf("", "'")
        val result = mutableListOf<String>()
        var lastFace = ""

        while (result.size < 11) {
            val face = moves[Random.nextInt(4)]
            if (face == lastFace) continue
            val modifier = modifiers[Random.nextInt(2)]
            result.add("$face$modifier")
            lastFace = face
        }
        return result.joinToString(" ")
    }

    private fun generatePyraminxScramble(): String {
        val moves = listOf("R", "L", "U", "B")
        val modifiers = listOf("", "'")
        val result = mutableListOf<String>()
        var lastFace = ""

        while (result.size < 8) {
            val face = moves[Random.nextInt(4)]
            if (face == lastFace) continue
            val modifier = modifiers[Random.nextInt(2)]
            result.add("$face$modifier")
            lastFace = face
        }

        // Add tips
        val tips = listOf("r", "l", "u", "b")
        tips.forEach { tip ->
            when (Random.nextInt(3)) {
                0 -> result.add("$tip")
                1 -> result.add("$tip'")
            }
        }
        return result.joinToString(" ")
    }

    private fun generateMegaminxScramble(): String {
        val result = StringBuilder()
        for (i in 0 until 7) {
            val line = mutableListOf<String>()
            for (j in 0 until 5) {
                val move = if (Random.nextBoolean()) "R" else "D"
                val modifier = if (Random.nextBoolean()) "++" else "--"
                line.add("$move$modifier")
            }
            val extra = if (Random.nextBoolean()) "U" else "U'"
            line.add(extra)
            result.appendLine(line.joinToString(" "))
        }
        return result.toString().trim()
    }

    private fun generateSquareOneScramble(): String {
        val result = mutableListOf<String>()
        for (i in 0 until 13) {
            val top = Random.nextInt(-5, 6)
            val bottom = Random.nextInt(-5, 6)
            result.add("($top,$bottom)")
            result.add("/")
        }
        return result.joinToString(" ")
    }

    private fun generateClockScramble(): String {
        val pins = listOf("UR", "DR", "DL", "UL", "U", "R", "D", "L", "ALL")
        val result = StringBuilder()

        // Scramble pins (top)
        pins.forEach { pin ->
            val value = Random.nextInt(-5, 6)
            val sign = if (value >= 0) "+" else ""
            result.append("$pin${sign}$value ")
        }
        result.append("y2 ")

        // Scramble pins (bottom)
        pins.forEach { pin ->
            val value = Random.nextInt(-5, 6)
            val sign = if (value >= 0) "+" else ""
            result.append("$pin${sign}$value ")
        }

        // Pin states
        val pinStates = (1..4).map { if (Random.nextBoolean()) "UR" else "DR" }
        result.append(pinStates.joinToString(" "))

        return result.toString().trim()
    }

    private fun getOppositeFace(face: String): String {
        return when (face) {
            "R" -> "L"
            "L" -> "R"
            "U" -> "D"
            "D" -> "U"
            "F" -> "B"
            "B" -> "F"
            else -> ""
        }
    }
}
