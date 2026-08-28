package com.basicsapp.timer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.data.models.Solve
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.utils.TimeFormatter
import com.basicsapp.timer.viewmodel.TimerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Info about a tapped histogram bucket. */
data class BucketInfo(val range: String, val count: Int, val solves: List<Solve>)

@Composable
fun ChartsScreen(viewModel: TimerViewModel) {
    val solves by viewModel.solves.collectAsState()

    var hoveredSolve by remember { mutableStateOf<Pair<Solve, Int>?>(null) }
    var hoveredBucket by remember { mutableStateOf<BucketInfo?>(null) }
    var selectedSolve by remember { mutableStateOf<Pair<Solve, Int>?>(null) }
    var selectedBucket by remember { mutableStateOf<BucketInfo?>(null) }

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
                Column {
                    TimeProgressionChart(
                        solves = solves,
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        onSolveTap = { solve, index ->
                            if (hoveredSolve?.second == index) {
                                selectedSolve = hoveredSolve
                                hoveredSolve = null
                            } else {
                                hoveredSolve = solve to index
                            }
                        }
                    )

                    // always-visible info strip: node details when one is tapped, else a hint
                    val dateFormat = remember { SimpleDateFormat("MMM d, h:mma", Locale.getDefault()) }
                    val hovered = hoveredSolve
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .then(
                                if (hovered != null) {
                                    Modifier.clickable {
                                        selectedSolve = hoveredSolve
                                        hoveredSolve = null
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .background(BasicsColors.SurfaceHigh)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (hovered != null) {
                            val (solve, _) = hovered
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = TimeFormatter.formatTimeForDisplay(solve.timeMs, solve.penalty),
                                    fontFamily = AppFont.Orbitron,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (solve.penalty != 0) BasicsColors.Error else BasicsColors.Primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = dateFormat.format(Date(solve.createdAt)),
                                    fontFamily = AppFont.Orbitron,
                                    fontSize = 10.sp,
                                    color = BasicsColors.Tertiary
                                )
                            }
                            Text(
                                text = "tap for more info",
                                fontFamily = AppFont.Orbitron,
                                fontSize = 9.sp,
                                color = BasicsColors.Tertiary
                            )
                        } else {
                            Text(
                                text = "tap a node to see info",
                                fontFamily = AppFont.Orbitron,
                                fontSize = 9.sp,
                                color = BasicsColors.Tertiary
                            )
                        }
                    }
                }
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
                Column {
                    TimeDistributionChart(
                        solves = solves,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        onBucketTap = { hoveredBucket = it }
                    )

                    // always-visible info strip: bucket details (with times) when one is tapped
                    val hovered = hoveredBucket
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .then(
                                if (hovered != null) {
                                    Modifier.clickable {
                                        selectedBucket = hoveredBucket
                                        hoveredBucket = null
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .background(BasicsColors.SurfaceHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (hovered != null) {
                            Text(
                                text = "${hovered.count} solves · ${hovered.range}",
                                fontFamily = AppFont.Orbitron,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BasicsColors.Primary
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                hovered.solves.forEach { solve ->
                                    Text(
                                        text = "• ${TimeFormatter.formatTimeForDisplay(solve.timeMs, solve.penalty)}",
                                        fontFamily = AppFont.Orbitron,
                                        fontSize = 11.sp,
                                        color = if (solve.penalty != 0) BasicsColors.Error else BasicsColors.Tertiary
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "tap a bar to see info",
                                    fontFamily = AppFont.Orbitron,
                                    fontSize = 9.sp,
                                    color = BasicsColors.Tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // solve info popup for the time progression chart
    selectedSolve?.let { (solve, index) ->
        AlertDialog(
            onDismissRequest = { selectedSolve = null },
            containerColor = BasicsColors.SurfaceContainer,
            titleContentColor = BasicsColors.Primary,
            textContentColor = BasicsColors.Secondary,
            shape = RoundedCornerShape(0.dp),
            title = {
                Text("solve #${index + 1}", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                val dateFormat = SimpleDateFormat("MMM d, h:mma", Locale.getDefault())
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = TimeFormatter.formatTimeForDisplay(solve.timeMs, solve.penalty),
                        fontFamily = AppFont.Orbitron,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (solve.penalty != 0) BasicsColors.Error else BasicsColors.Primary
                    )
                    Text(
                        text = dateFormat.format(Date(solve.createdAt)),
                        fontFamily = AppFont.Orbitron,
                        fontSize = 11.sp,
                        color = BasicsColors.Tertiary
                    )
                    Text(
                        text = when (solve.penalty) { 2 -> "+2 penalty"; -1 -> "did not finish"; else -> "no penalty" },
                        fontFamily = AppFont.Orbitron,
                        fontSize = 11.sp,
                        color = if (solve.penalty != 0) BasicsColors.Error else BasicsColors.Tertiary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSolve = null }) {
                    Text("close", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontSize = 12.sp)
                }
            }
        )
    }

    // bucket info popup for the time distribution chart
    selectedBucket?.let { info ->
        AlertDialog(
            onDismissRequest = { selectedBucket = null },
            containerColor = BasicsColors.SurfaceContainer,
            titleContentColor = BasicsColors.Primary,
            textContentColor = BasicsColors.Secondary,
            shape = RoundedCornerShape(0.dp),
            title = {
                Text("${info.range} solves", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${info.count} in this range",
                        fontFamily = AppFont.Orbitron,
                        fontSize = 11.sp,
                        color = BasicsColors.Tertiary
                    )
                    info.solves.take(12).forEach { solve ->
                        Text(
                            text = "• ${TimeFormatter.formatTimeForDisplay(solve.timeMs, solve.penalty)}",
                            fontFamily = AppFont.Orbitron,
                            fontSize = 12.sp,
                            color = if (solve.penalty != 0) BasicsColors.Error else BasicsColors.Secondary
                        )
                    }
                    if (info.count > 12) {
                        Text(
                            text = "+${info.count - 12} more",
                            fontFamily = AppFont.Orbitron,
                            fontSize = 11.sp,
                            color = BasicsColors.Tertiary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBucket = null }) {
                    Text("close", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontSize = 12.sp)
                }
            }
        )
    }
}
@Composable
fun TimeProgressionChart(
    solves: List<Solve>,
    modifier: Modifier = Modifier,
    onSolveTap: (Solve, Int) -> Unit = { _, _ -> }
) {
    val validSolves = solves.filter { it.penalty != -1 }
    Canvas(
        modifier = modifier
            .background(BasicsColors.Surface)
            .pointerInput(solves.size) {
                detectTapGestures { offset ->
                    if (solves.size < 2) return@detectTapGestures
                    val leftPadding = 60f
                    val rightPadding = 20f
                    val chartWidth = size.width - leftPadding - rightPadding
                    if (chartWidth <= 0f || offset.x < leftPadding || offset.x > size.width - rightPadding) return@detectTapGestures
                    val index = (((offset.x - leftPadding) / chartWidth) * (solves.size - 1)).roundToInt().coerceIn(0, solves.size - 1)
                    onSolveTap(solves[index], index)
                }
            }
    ) {
        if (validSolves.size < 2) return@Canvas

        val times = validSolves.map { solve -> (if (solve.penalty == 2) solve.timeMs + 2000 else solve.timeMs) / 1000f }

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

        // X-axis labels: solve numbers
        val xLabelCount = if (solves.size <= 10) solves.size else 10
        for (i in 0 until xLabelCount) {
            val index = (i * (solves.size - 1) / (xLabelCount - 1).coerceAtLeast(1))
            val x = leftPadding + (index.toFloat() / (solves.size - 1)) * chartWidth
            drawContext.canvas.nativeCanvas.drawText("${index + 1}", x - 8f, size.height - 6f, paint)
        }

        // Line path (breaks across DNFs)
        val path = Path()
        var started = false
        solves.forEachIndexed { index, solve ->
            if (solve.penalty == -1) return@forEachIndexed
            val x = leftPadding + (index.toFloat() / (solves.size - 1)) * chartWidth
            val t = (if (solve.penalty == 2) solve.timeMs + 2000 else solve.timeMs) / 1000f
            val y = topPadding + chartHeight - ((t - paddedMin) / paddedRange) * chartHeight
            if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
        }
        drawPath(path, color = Color.White, style = Stroke(width = 2.5f))

        // Dots
        solves.forEachIndexed { index, solve ->
            if (solve.penalty == -1) return@forEachIndexed
            val x = leftPadding + (index.toFloat() / (solves.size - 1)) * chartWidth
            val t = (if (solve.penalty == 2) solve.timeMs + 2000 else solve.timeMs) / 1000f
            val y = topPadding + chartHeight - ((t - paddedMin) / paddedRange) * chartHeight
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
fun TimeDistributionChart(
    solves: List<Solve>,
    modifier: Modifier = Modifier,
    onBucketTap: (BucketInfo) -> Unit = {}
) {
    val validSolves = solves.filter { it.penalty != -1 }
    Canvas(
        modifier = modifier
            .background(BasicsColors.Surface)
            .pointerInput(solves) {
                detectTapGestures { offset ->
                    val valid = solves.filter { it.penalty != -1 }
                    if (valid.isEmpty()) return@detectTapGestures
                    val times = valid.map { (if (it.penalty == 2) it.timeMs + 2000 else it.timeMs) / 1000f }
                    val maxTime = times.max()
                    val bucketSize = if (maxTime <= 10f) 2f else if (maxTime <= 30f) 5f else if (maxTime <= 60f) 10f else 15f
                    val bucketCount = ((maxTime / bucketSize).toInt() + 1).coerceAtMost(20)
                    val leftPadding = 60f
                    val rightPadding = 20f
                    val chartWidth = size.width - leftPadding - rightPadding
                    if (chartWidth <= 0f || offset.x < leftPadding || offset.x >= size.width - rightPadding) return@detectTapGestures
                    val bucketIndex = ((offset.x - leftPadding) / (chartWidth / bucketCount)).toInt().coerceIn(0, bucketCount - 1)
                    val bucketSolves = valid.filter {
                        val t = (if (it.penalty == 2) it.timeMs + 2000 else it.timeMs) / 1000f
                        (t / bucketSize).toInt().coerceIn(0, bucketCount - 1) == bucketIndex
                    }
                    val range = "${(bucketIndex * bucketSize).toInt()}-${((bucketIndex + 1) * bucketSize).toInt()}s"
                    onBucketTap(BucketInfo(range, bucketSolves.size, bucketSolves))
                }
            }
    ) {
        if (validSolves.isEmpty()) return@Canvas

        val times = validSolves.map { solve -> (if (solve.penalty == 2) solve.timeMs + 2000 else solve.timeMs) / 1000f }
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