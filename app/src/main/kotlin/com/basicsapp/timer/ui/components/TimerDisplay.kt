package com.basicsapp.timer.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.utils.TimeFormatter
import com.basicsapp.timer.viewmodel.TimerState

/**
 * Timer-style text that:
 *  - caps the system font scale (so huge accessibility font settings can't push the
 *    timer onto a second line or off-screen),
 *  - never wraps (single line, no soft wrap),
 *  - and auto-shrinks its font size to fit the available width on narrow screens.
 */
@Composable
fun AdaptiveTimerText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 72.sp
) {
    val baseDensity = LocalDensity.current
    val cappedFontScale = baseDensity.fontScale.coerceAtMost(1.2f)
    val cappedDensity = Density(baseDensity.density, cappedFontScale)
    val textMeasurer = rememberTextMeasurer()

    val measured = textMeasurer.measure(
        AnnotatedString(text),
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = AppFont.Orbitron,
            letterSpacing = (-2).sp
        ),
        density = cappedDensity
    )

    BoxWithConstraints(modifier = modifier) {
        val availWidthPx = with(cappedDensity) { maxWidth.toPx() }
        val textWidthPx = measured.size.width.coerceAtLeast(1).toFloat()
        val fit = if (textWidthPx > availWidthPx) availWidthPx / textWidthPx else 1f

        CompositionLocalProvider(LocalDensity provides cappedDensity) {
            Text(
                text = text,
                fontSize = fontSize * fit,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont.Orbitron,
                color = color,
                letterSpacing = (-2).sp,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun TimerDisplay(
    elapsed: Long,
    timerState: TimerState,
    isDnf: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdaptiveTimerText(
            text = if (isDnf) "dnf" else TimeFormatter.formatTime(elapsed),
            color = if (isDnf) BasicsColors.Error else BasicsColors.Primary
        )
    }
}
