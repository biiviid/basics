package com.basicsapp.timer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.data.models.Solve
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.utils.TimeFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SolveCard(
    solve: Solve,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isRepeatedScramble: Boolean = false,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDnf = solve.penalty == -1
    val isPlus2 = solve.penalty == 2
    val isError = isDnf || isPlus2
    val isYellow = isRepeatedScramble && !isError
    val bgColor = if (index % 2 == 0) BasicsColors.Surface else BasicsColors.Background
    val dateFormat = SimpleDateFormat("h:mma, MMM d", Locale.getDefault())
    val dateStr = dateFormat.format(Date(solve.createdAt))

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() else onClick() },
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, if (isSelected) BasicsColors.Primary else BasicsColors.Border)
                    .background(if (isSelected) BasicsColors.Primary else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("x", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Background)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = "${index + 1}.",
            fontSize = 14.sp,
            fontFamily = AppFont.Orbitron,
            color = BasicsColors.Tertiary,
            modifier = Modifier.width(32.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isDnf) TimeFormatter.formatTime(solve.timeMs) else TimeFormatter.formatTimeForDisplay(solve.timeMs, solve.penalty),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppFont.Orbitron,
                    color = when {
                        isError -> BasicsColors.Error
                        isYellow -> Color(0xFFCCCC00)
                        else -> BasicsColors.Primary
                    }
                )

                if (isPlus2) {
                    Text(" +2", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Error)
                }
                if (isDnf) {
                    Text(" dnf", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Error)
                }
                if (isYellow) {
                    Text(" <<", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = Color(0xFFCCCC00))
                }
            }

            Text(
                text = dateStr,
                fontSize = 11.sp,
                fontFamily = AppFont.Orbitron,
                color = BasicsColors.Tertiary,
                letterSpacing = 0.05.sp
            )
        }
    }
}
