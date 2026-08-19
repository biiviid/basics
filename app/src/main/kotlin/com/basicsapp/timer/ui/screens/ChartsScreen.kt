package com.basicsapp.timer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.data.models.Solve
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.viewmodel.TimerViewModel

@Composable
fun ChartsScreen(viewModel: TimerViewModel) {
    val solves by viewModel.solves.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BasicsColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "charts",
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
            Text(
                text = "time progression",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont.Orbitron,
                color = BasicsColors.Tertiary,
                letterSpacing = 0.1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BasicsColors.Border)
                    .background(BasicsColors.Surface)
            ) {
                TimeProgressionChart(solves = solves, modifier = Modifier.fillMaxWidth().height(250.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "time distribution",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont.Orbitron,
                color = BasicsColors.Tertiary,
                letterSpacing = 0.1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BasicsColors.Border)
                    .background(BasicsColors.Surface)
            ) {
                TimeDistributionChart(solves = solves, modifier = Modifier.fillMaxWidth().height(200.dp))
            }
        }
    }
}

@Composable
fun TimeProgressionChart(solves: List<Solve>, modifier: Modifier = Modifier) {
    val validSolves = solves.filter { it.penalty != -1 }
    Canvas(modifier = modifier.background(BasicsColors.Surface)) {
        if (validSolves.size < 2) return@Canvas

        val times = validSolves.map { solve ->
            val t = if (solve.penalty == 2) solve.timeMs + 2000 else solve.timeMs
            t / 1000f
        }

        val minTime = times.min()
        val maxTime = times.max()
        val range = (maxTime - minTime).coerceAtLeast(0.1f)
        val paddedMin = minTime - range * 0.05f
        val paddedMax = maxTime + range * 0.05f
        val paddedRange = paddedMax - paddedMin

        val leftPadding = 60f
        val rightPadding = 20f
        val topPadding = 20f
        val bottomPadding = 30f
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 142, 145, 146)
            textSize = 24f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }

        // Y-axis labels and grid lines
        val ySteps = 5
        for (i in 0..ySteps) {
            val value = paddedMax - (paddedRange * i / ySteps)
            val y = topPadding + (chartHeight * i / ySteps)

            drawLine(Color(0xFF353535), Offset(leftPadding, y), Offset(size.width - rightPadding, y), strokeWidth = 1f)

            val label = String.format("%.1fs", value)
            drawContext.canvas.nativeCanvas.drawText(label, 4f, y + 8f, paint)
        }

        // Axes
        drawLine(Color(0xFF444748), Offset(leftPadding, topPadding), Offset(leftPadding, size.height - bottomPadding), strokeWidth = 2f)
        drawLine(Color(0xFF444748), Offset(leftPadding, size.height - bottomPadding), Offset(size.width - rightPadding, size.height - bottomPadding), strokeWidth = 2f)

        // X-axis labels
        val xLabelCount = if (times.size <= 10) times.size else 10
        for (i in 0 until xLabelCount) {
            val index = (i * (times.size - 1) / (xLabelCount - 1).coerceAtLeast(1))
            val x = leftPadding + (index.toFloat() / (times.size - 1)) * chartWidth
            drawContext.canvas.nativeCanvas.drawText("${index + 1}", x - 8f, size.height - 6f, paint)
        }

        // Line path
        val path = Path()
        times.forEachIndexed { index, time ->
            val x = leftPadding + (index.toFloat() / (times.size - 1)) * chartWidth
            val y = topPadding + chartHeight - ((time - paddedMin) / paddedRange) * chartHeight
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = Color.White, style = Stroke(width = 2.5f))

        // Dots
        times.forEachIndexed { index, time ->
            val x = leftPadding + (index.toFloat() / (times.size - 1)) * chartWidth
            val y = topPadding + chartHeight - ((time - paddedMin) / paddedRange) * chartHeight
            drawCircle(color = Color.White, radius = 4f, center = Offset(x, y))
        }

        // DNF markers
        solves.forEachIndexed { index, solve ->
            if (solve.penalty == -1 && solves.size > 1) {
                val x = leftPadding + (index.toFloat() / (solves.size - 1)) * chartWidth
                val y = topPadding + 10f
                drawLine(Color(0xFFFF4444), Offset(x - 8, y - 8), Offset(x + 8, y + 8), strokeWidth = 3f)
                drawLine(Color(0xFFFF4444), Offset(x + 8, y - 8), Offset(x - 8, y + 8), strokeWidth = 3f)
            }
        }
    }
}

@Composable
fun TimeDistributionChart(solves: List<Solve>, modifier: Modifier = Modifier) {
    val validSolves = solves.filter { it.penalty != -1 }
    Canvas(modifier = modifier.background(BasicsColors.Surface)) {
        if (validSolves.isEmpty()) return@Canvas

        val times = validSolves.map { solve ->
            val t = if (solve.penalty == 2) solve.timeMs + 2000 else solve.timeMs
            t / 1000f
        }

        val maxTime = times.max()
        val bucketSize = if (maxTime <= 10f) 2f else if (maxTime <= 30f) 5f else if (maxTime <= 60f) 10f else 15f
        val bucketCount = ((maxTime / bucketSize).toInt() + 1).coerceAtMost(20)

        val buckets = IntArray(bucketCount)
        times.forEach { time ->
            val bucketIndex = (time / bucketSize).toInt().coerceIn(0, bucketCount - 1)
            buckets[bucketIndex]++
        }

        val maxCount = buckets.max().coerceAtLeast(1)
        val leftPadding = 60f
        val rightPadding = 20f
        val topPadding = 20f
        val bottomPadding = 40f
        val chartWidth = size.width - leftPadding - rightPadding
        val chartHeight = size.height - topPadding - bottomPadding
        val barWidth = chartWidth / bucketCount

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(255, 142, 145, 146)
            textSize = 22f
            isAntiAlias = true
            typeface = android.graphics.Typeface.MONOSPACE
        }

        // Y-axis labels and grid
        val ySteps = 4
        for (i in 0..ySteps) {
            val value = maxCount * (ySteps - i) / ySteps
            val y = topPadding + (chartHeight * i / ySteps)
            drawLine(Color(0xFF353535), Offset(leftPadding, y), Offset(size.width - rightPadding, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText("$value", 4f, y + 8f, paint)
        }

        // Axes
        drawLine(Color(0xFF444748), Offset(leftPadding, topPadding), Offset(leftPadding, size.height - bottomPadding), strokeWidth = 2f)
        drawLine(Color(0xFF444748), Offset(leftPadding, size.height - bottomPadding), Offset(size.width - rightPadding, size.height - bottomPadding), strokeWidth = 2f)

        // Bars
        buckets.forEachIndexed { index, count ->
            if (count > 0) {
                val barHeight = (count.toFloat() / maxCount) * chartHeight
                val x = leftPadding + index * barWidth
                val y = size.height - bottomPadding - barHeight
                drawRect(
                    color = Color(0xFF555555),
                    topLeft = Offset(x + 3, y),
                    size = Size(barWidth - 6, barHeight)
                )
            }
        }

        // X-axis labels
        paint.textSize = 18f
        for (i in 0 until bucketCount) {
            val label = "${(i * bucketSize).toInt()}-${((i + 1) * bucketSize).toInt()}"
            val x = leftPadding + i * barWidth + barWidth / 2 - 20f
            drawContext.canvas.nativeCanvas.drawText(label, x, size.height - 8f, paint)
        }
    }
}
