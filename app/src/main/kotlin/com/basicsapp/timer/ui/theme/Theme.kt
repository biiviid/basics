package com.basicsapp.timer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.basicsapp.timer.R

object AppFont {
    val Orbitron = FontFamily(
        Font(R.font.orbitron_regular, FontWeight.Normal),
        Font(R.font.orbitron_bold, FontWeight.Bold)
    )
}

object BasicsColors {
    val Background = Color(0xFF131313)
    val Surface = Color(0xFF1C1B1B)
    val SurfaceContainer = Color(0xFF20201F)
    val SurfaceHigh = Color(0xFF2A2A2A)
    val SurfaceHighest = Color(0xFF353535)
    val Primary = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFC6C6C6)
    val Tertiary = Color(0xFF8E9192)
    val Border = Color(0xFF444748)
    val BorderHeavy = Color(0xFFFFFFFF)
    val Accent = Color(0xFF555555)
    val Error = Color(0xFFFF4444)
    val DnfRed = Color(0xFFFFB4AB)
    val Armed = Color(0xFF88CCFF)
}
