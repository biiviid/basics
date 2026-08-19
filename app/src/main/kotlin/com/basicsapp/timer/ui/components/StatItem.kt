package com.basicsapp.timer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.ui.theme.AppFont

@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.lowercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AppFont.Orbitron,
            color = BasicsColors.Tertiary,
            letterSpacing = 0.1.sp
        )

        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AppFont.Orbitron,
            color = BasicsColors.Primary
        )
    }
}
