package com.basicsapp.timer.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.utils.TimeFormatter
import com.basicsapp.timer.viewmodel.TimerState

/**
 * Tracking (letter spacing) for the big timer digits.
 *
 * The timer was originally tracked at -2sp against the 72sp nominal font. The
 * ratio is expressed in em rather than sp so that the spacing scales with the
 * font size: once [ScaledTimerText] shrinks the font, an sp value would keep a
 * fixed pixel gap (which breaks the measure/render symmetry and makes the
 * spacing proportionally larger than designed), while an em value keeps the
 * spacing a constant ratio of the glyph size - a true uniform scale.
 *
 * -2 / 72 = -0.0277... em
 */
internal val TimerTextTrackingEm = TextUnit(-2f / 72f, TextUnitType.Em)

/** Cap on the system font scale the timer honours (accessibility sizes are capped). */
private const val MaxTimerFontScale = 1.2f

/**
 * Shrink factor for a measured text width into the available width.
 * The text is scaled down only when it does not fit; it never grows.
 */
internal fun timerTextFit(measuredWidthPx: Float, availWidthPx: Float): Float =
    if (availWidthPx > 0f && measuredWidthPx > availWidthPx) {
        (availWidthPx / measuredWidthPx).coerceAtMost(1f)
    } else {
        1f
    }

/**
 * Single-line Orbitron timer text that:
 *  - caps the system font scale (so huge accessibility font settings can't push
 *    the timer onto a second line or off-screen),
 *  - never wraps (single line, no soft wrap),
 *  - auto-shrinks its font size to fit the available width on narrow screens.
 *
 * [letterSpacing] is interpreted in **em** (see [TimerTextTrackingEm]) so the
 * shrink stays a true uniform scale; the same value drives both the width
 * measurement and the final render.
 *
 * [textModifier] is applied to the rendered [Text] itself (ArmedText uses this
 * to attach its progress fill), whereas [modifier] applies to the outer layout.
 */
@Composable
fun ScaledTimerText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    letterSpacing: TextUnit,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier
) {
    val baseDensity = LocalDensity.current
    val cappedFontScale = baseDensity.fontScale.coerceAtMost(MaxTimerFontScale)
    val cappedDensity = Density(baseDensity.density, cappedFontScale)
    val textMeasurer = rememberTextMeasurer()

    val measured = remember(text, fontSize, letterSpacing, cappedFontScale) {
        textMeasurer.measure(
            AnnotatedString(text),
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont.Orbitron,
                letterSpacing = letterSpacing
            ),
            density = cappedDensity
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val availWidthPx = with(cappedDensity) { maxWidth.toPx() }
        val fit = timerTextFit(measured.size.width.coerceAtLeast(1).toFloat(), availWidthPx)

        CompositionLocalProvider(LocalDensity provides cappedDensity) {
            Text(
                text = text,
                modifier = textModifier,
                fontSize = fontSize * fit,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont.Orbitron,
                color = color,
                letterSpacing = letterSpacing,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun AdaptiveTimerText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 72.sp
) {
    ScaledTimerText(
        text = text,
        color = color,
        fontSize = fontSize,
        letterSpacing = TimerTextTrackingEm,
        modifier = modifier
    )
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
