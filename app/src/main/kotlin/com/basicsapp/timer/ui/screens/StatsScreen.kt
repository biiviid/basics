package com.basicsapp.timer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.ui.components.StatItem
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.utils.TimeFormatter
import com.basicsapp.timer.viewmodel.TimerViewModel

@Composable
fun StatsScreen(viewModel: TimerViewModel) {
    val stats by viewModel.stats.collectAsState()
    val solves by viewModel.solves.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BasicsColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "session stats",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AppFont.Orbitron,
            color = BasicsColors.Tertiary,
            letterSpacing = 0.1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (solves.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "no data",
                    fontFamily = AppFont.Orbitron,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BasicsColors.Tertiary,
                    letterSpacing = 0.1.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BasicsColors.Border)
                    .background(BasicsColors.Surface)
                    .padding(16.dp)
            ) {
                Column {
                    StatItem(label = "best", value = TimeFormatter.formatStatTime(stats.best))
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "worst", value = TimeFormatter.formatStatTime(stats.worst))
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "mean", value = TimeFormatter.formatStatTime(stats.mean))
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "median", value = TimeFormatter.formatStatTime(stats.median))
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "std dev", value = if (stats.stdDev > 0) "${String.format("%.2f", stats.stdDev)}s" else "n/a")
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "ao5", value = TimeFormatter.formatStatTime(stats.ao5))
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "ao12", value = TimeFormatter.formatStatTime(stats.ao12))
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "total", value = stats.totalSolves.toString())
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "dnfs", value = stats.dnfCount.toString())
                    HorizontalDivider(color = BasicsColors.Border, thickness = 1.dp)
                    StatItem(label = "session time", value = TimeFormatter.formatTimeForDisplay(stats.sessionTime))
                }
            }
        }
    }
}
