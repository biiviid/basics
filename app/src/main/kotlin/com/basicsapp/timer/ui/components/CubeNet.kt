package com.basicsapp.timer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.basicsapp.timer.utils.PuzzleType

private val PALETTE = intArrayOf(
    0xFFFFFFFF.toInt(), // 0: U white
    0xFFFF3333.toInt(), // 1: R red
    0xFF00CC00.toInt(), // 2: F green
    0xFFFFFF00.toInt(), // 3: D yellow
    0xFFFF8800.toInt(), // 4: L orange
    0xFF0066FF.toInt()  // 5: B blue
)

// Net layout matches csTimer's prettyString:
//       [U]
//    [L][F][R][B]
//       [D]
// Grid: 12 cols x 9 rows
private fun faceletToNetPos(faceletIdx: Int): Pair<Int, Int>? {
    if (faceletIdx < 0 || faceletIdx >= 54) return null
    val faceIdx = faceletIdx / 9
    val sticker = faceletIdx % 9
    val col = sticker % 3
    val row = sticker / 3
    return when (faceIdx) {
        0 -> Pair(3 + col, row)         // U: top center
        1 -> Pair(6 + col, 3 + row)     // R: middle right
        2 -> Pair(3 + col, 3 + row)     // F: middle center
        3 -> Pair(3 + col, 6 + row)     // D: bottom center
        4 -> Pair(col, 3 + row)         // L: middle left
        5 -> Pair(9 + col, 3 + row)     // B: middle far right
        else -> null
    }
}

@Composable
fun CubeNet(
    scramble: String,
    puzzleType: PuzzleType,
    modifier: Modifier = Modifier
) {
    when (puzzleType) {
        PuzzleType.THREE_BY_THREE -> CubeNet333(scramble, modifier)
        else -> {
            Canvas(modifier = modifier) {
                drawRect(color = Color(0xFF353535), topLeft = Offset.Zero, size = Size(size.width, size.height), style = Stroke(width = 2f))
            }
        }
    }
}

@Composable
fun CubeNet333(scramble: String, modifier: Modifier = Modifier) {
    val colors = remember(scramble) {
        val cc = CubieCube.applyScramble(scramble)
        CubieCube.toColorIndices(cc)
    }

    Canvas(modifier = modifier) {
        val gridCols = 12
        val gridRows = 9
        val stickerW = size.width / gridCols
        val stickerH = size.height / gridRows
        val gap = 1.5f

        for (i in 0 until 54) {
            val pos = faceletToNetPos(i) ?: continue
            val color = Color(PALETTE[colors[i].coerceIn(0, 5)])
            val x = pos.first * stickerW + gap
            val y = pos.second * stickerH + gap
            val w = stickerW - gap * 2
            val h = stickerH - gap * 2
            drawRect(color = color, topLeft = Offset(x, y), size = Size(w, h))
            drawRect(color = Color(0xFF222222), topLeft = Offset(x, y), size = Size(w, h), style = Stroke(width = 1f))
        }
    }
}
