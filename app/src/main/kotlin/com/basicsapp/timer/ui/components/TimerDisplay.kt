package com.basicsapp.timer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.utils.TimeFormatter
import com.basicsapp.timer.viewmodel.TimerState

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
        Text(
            text = if (isDnf) "dnf" else TimeFormatter.formatTime(elapsed),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AppFont.Orbitron,
            color = if (isDnf) BasicsColors.Error else BasicsColors.Primary,
            letterSpacing = (-2).sp
        )
    }
}
