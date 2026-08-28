package com.basicsapp.timer.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.ui.components.AdaptiveTimerText
import com.basicsapp.timer.ui.components.CubeInputDialog
import com.basicsapp.timer.ui.components.CubeNet
import com.basicsapp.timer.ui.components.CubeNetFromColors
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.utils.TimeFormatter
import com.basicsapp.timer.viewmodel.TimerInputMode
import com.basicsapp.timer.viewmodel.TimerState
import com.basicsapp.timer.viewmodel.TimerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    onTimerPosition: (Offset) -> Unit = {}
) {
    val context = LocalContext.current
    val elapsed by viewModel.elapsed.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val scramble by viewModel.scramble.collectAsState()
    val puzzleType by viewModel.puzzleType.collectAsState()
    val hideMillisWhileSolving by viewModel.hideMillisWhileSolving.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val sessionPb by viewModel.sessionPb.collectAsState()
    val overallPb by viewModel.overallPb.collectAsState()
    val inspectionEnabled by viewModel.inspectionEnabled.collectAsState()
    val enabledAverages by viewModel.enabledAverages.collectAsState()
    val isInspecting by viewModel.isInspecting.collectAsState()
    val inspectionTime by viewModel.inspectionTime.collectAsState()
    val inspectionPenalty by viewModel.inspectionPenalty.collectAsState()
    val lastSolveTime by viewModel.lastSolveTime.collectAsState()
    val lastSolvePenalty by viewModel.lastSolvePenalty.collectAsState()
    val scrambleIndex by viewModel.scrambleIndex.collectAsState()
    val lastSolveUsedPrevScramble by viewModel.lastSolveUsedPrevScramble.collectAsState()
    val lastSolveId by viewModel.lastSolveId.collectAsState()
    val holdTimeMs by viewModel.holdTimeMs.collectAsState()
    val inspectionHoldStart by viewModel.inspectionHoldStart.collectAsState()
    val inputMode by viewModel.inputMode.collectAsState()
    val manualColors by viewModel.manualColors.collectAsState()
    val scope = rememberCoroutineScope()

    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showNetPopup by remember { mutableStateOf(false) }
    var showCubeInput by remember { mutableStateOf(false) }

    val armProgress = remember { Animatable(0f) }
    LaunchedEffect(timerState, holdTimeMs) {
        if (timerState == TimerState.ARMED) {
            armProgress.snapTo(0f)
            armProgress.animateTo(1f, animationSpec = tween(holdTimeMs))
        } else {
            armProgress.snapTo(0f)
        }
    }

    // Split on original moves (never breaks a notation+prime), then format each line
    val moves = scramble.split(" ").filter { it.isNotBlank() }
    val mid = (moves.size + 1) / 2
    val line1 = moves.take(mid).joinToString("  ") { it.replace("'", " '") }
    val line2 = moves.drop(mid).joinToString("  ") { it.replace("'", " '") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BasicsColors.Background)
            .pointerInput(inspectionHoldStart, holdTimeMs) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = true)
                    when {
                        isInspecting -> {
                            val downTime = System.nanoTime()
                            viewModel.armTimer()
                            val up = waitForUpOrCancellation()
                            val heldMs = (System.nanoTime() - downTime) / 1_000_000
                            if (up != null && heldMs >= holdTimeMs) {
                                viewModel.startFromInspection()
                            } else {
                                viewModel.disarmTimer()
                            }
                        }
                        timerState == TimerState.IDLE -> {
                            if (inspectionEnabled && inspectionHoldStart) {
                                // hold-to-start inspection: the countdown engages once the
                                // arm hold time is reached (while still holding); releasing
                                // starts the solve timer
                                // show the arm fill effect while holding; the countdown
                                // engages once the hold time is reached, then displays normally
                                viewModel.armTimer()
                                val downTime = System.nanoTime()
                                val countdownJob = scope.launch {
                                    delay(holdTimeMs.toLong())
                                    viewModel.startInspection()
                                    viewModel.disarmTimer()
                                }
                                val up = waitForUpOrCancellation()
                                countdownJob.cancel()
                                val heldMs = (System.nanoTime() - downTime) / 1_000_000
                                if (up != null && heldMs >= holdTimeMs) {
                                    viewModel.startFromInspection()
                                } else {
                                    viewModel.cancelInspection()
                                    viewModel.disarmTimer()
                                }
                            } else if (inspectionEnabled) {
                                val up = waitForUpOrCancellation()
                                if (up != null) viewModel.startInspection()
                            } else {
                                val downTime = System.nanoTime()
                                viewModel.armTimer()
                                val up = waitForUpOrCancellation()
                                val heldMs = (System.nanoTime() - downTime) / 1_000_000
                                if (up != null && heldMs >= holdTimeMs) {
                                    viewModel.startTimer()
                                } else {
                                    viewModel.disarmTimer()
                                }
                            }
                        }
                        timerState == TimerState.RUNNING -> {
                            val up = waitForUpOrCancellation()
                            if (up != null) viewModel.stopTimer()
                        }
                        else -> waitForUpOrCancellation()
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // scramble container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .border(1.dp, BasicsColors.Border)
                    .background(BasicsColors.SurfaceContainer)
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (inputMode == TimerInputMode.MANUAL) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("manual cube state", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary)
                        Text("time whatever's in your hands", fontSize = 11.sp, fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "\u25A6",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFont.Orbitron,
                            color = BasicsColors.Primary,
                            modifier = Modifier.clickable { showCubeInput = true }
                        )
                        Text(
                            text = "scramble",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFont.Orbitron,
                            color = BasicsColors.Secondary,
                            modifier = Modifier.clickable { viewModel.setInputMode(TimerInputMode.SCRAMBLE) }
                        )
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line1, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary, textAlign = TextAlign.Start, lineHeight = 20.sp)
                        if (line2.isNotBlank()) {
                            Text(line2, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary, textAlign = TextAlign.Start, lineHeight = 20.sp)
                        }
                    }

                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "<<",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFont.Orbitron,
                            color = if (scrambleIndex > 0) BasicsColors.Primary else BasicsColors.Border,
                            modifier = Modifier.clickable(enabled = scrambleIndex > 0) { viewModel.previousScramble() }
                        )
                        Canvas(
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.refreshScramble() }
                        ) {
                            val s = size.minDimension
                            val t = 5f
                            val color = BasicsColors.Primary
                            val p = s * 0.1f
                            val w = s - p * 2
                            val gap = w * 0.3f
                            // draw square clockwise from top-left corner, with a gap at top
                            // left edge: full
                            drawLine(color, Offset(p, p), Offset(p, p + w), t)
                            // bottom edge: full
                            drawLine(color, Offset(p, p + w), Offset(p + w, p + w), t)
                            // right edge: full
                            drawLine(color, Offset(p + w, p + w), Offset(p + w, p), t)
                            // top edge: from right to just past middle (gap ends here)
                            drawLine(color, Offset(p + w, p), Offset(p + gap, p), t)
                            // arrowhead at the end of top edge, pointing left
                            val ah = w * 0.6f
                            drawLine(color, Offset(p + gap, p), Offset(p + gap + ah * 0.5f, p - ah * 0.5f), t)
                            drawLine(color, Offset(p + gap, p), Offset(p + gap + ah * 0.5f, p + ah * 0.5f), t)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "\u2398",
                            fontSize = 18.sp,
                            color = BasicsColors.Primary,
                            modifier = Modifier.clickable {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("scramble", scramble)
                                clipboard.setPrimaryClip(clip)
                            }
                        )
                        Text(
                            text = "in",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFont.Orbitron,
                            color = BasicsColors.Primary,
                            modifier = Modifier.clickable { showCustomDialog = true }
                        )
                    }
                }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // center: timer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // last solve
                    if (lastSolveTime != null && timerState == TimerState.IDLE && !isInspecting && timerState != TimerState.ARMED) {
                        val isDnf = lastSolvePenalty == -1
                        val isPlus2 = lastSolvePenalty == 2
                        val isYellow = lastSolveUsedPrevScramble
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdaptiveTimerText(
                                text = if (isDnf) "dnf" else TimeFormatter.formatTime(lastSolveTime!!),
                                color = when {
                                    isDnf -> BasicsColors.Error
                                    isPlus2 -> BasicsColors.Error
                                    isYellow -> Color(0xFFCCCC00)
                                    else -> BasicsColors.Primary
                                }
                            )
                            if (isPlus2) Text(" +2", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Error, modifier = Modifier.padding(start = 4.dp))
                            if (isDnf) Text(" dnf", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Error, modifier = Modifier.padding(start = 4.dp))
                            if (isYellow) Text(" <<", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = Color(0xFFCCCC00), modifier = Modifier.padding(start = 4.dp))
                        }
                    }

                    if (isInspecting) {
                        val isArmed = timerState == TimerState.ARMED
                        when {
                            inspectionPenalty == "dnf" -> Text("dnf", fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Error)
                            inspectionPenalty == "plus2" -> Text("+2", fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Error)
                            isArmed -> ArmedText(
                                text = "${inspectionTime}",
                                progress = armProgress.value,
                                fontSize = 64.sp,
                                normalColor = if (inspectionTime <= 3) BasicsColors.Tertiary else BasicsColors.Primary
                            )
                            else -> Text(
                                "${inspectionTime}",
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont.Orbitron,
                                color = if (inspectionTime <= 3) BasicsColors.Tertiary else BasicsColors.Primary
                            )
                        }
                    }

                    if (timerState == TimerState.RUNNING) {
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                onTimerPosition(coords.localToRoot(Offset.Zero))
                            }
                        ) {
                            AdaptiveTimerText(
                                text = if (hideMillisWhileSolving) TimeFormatter.formatTimeNoMillis(elapsed) else TimeFormatter.formatTime(elapsed),
                                color = BasicsColors.Primary
                            )
                        }
                    }

                    if (!isInspecting && timerState == TimerState.IDLE && lastSolveTime == null) {
                        Box(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                onTimerPosition(coords.localToRoot(Offset.Zero))
                            }
                        ) {
                            AdaptiveTimerText(text = "0:00.000", color = BasicsColors.Primary)
                        }
                    }

                    if (timerState == TimerState.ARMED && !isInspecting) {
                        ArmedText(
                            text = if (lastSolveTime != null) TimeFormatter.formatTime(lastSolveTime!!) else "0:00.000",
                            progress = armProgress.value,
                            fontSize = 72.sp,
                            normalColor = BasicsColors.Primary,
                            letterSpacing = (-2).sp
                        )
                    }

                    if (timerState != TimerState.ARMED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val stateText = when {
                            isInspecting -> if (inspectionHoldStart) "release to start" else "hold to start"
                            timerState == TimerState.RUNNING -> "tap to stop"
                            timerState == TimerState.IDLE && inspectionEnabled -> if (inspectionHoldStart) "hold to inspect" else "tap to inspect"
                            timerState == TimerState.IDLE -> "hold to arm"
                            else -> ""
                        }
                        Text(stateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary, letterSpacing = 0.1.sp)
                    }

                    if (lastSolveId != null && timerState == TimerState.IDLE && !isInspecting && timerState != TimerState.ARMED) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                            Text(
                                text = "+2",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont.Orbitron,
                                color = BasicsColors.Secondary,
                                modifier = Modifier.clickable { viewModel.markLastSolvePenalty(2) }
                            )
                            Text(
                                text = "dnf",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont.Orbitron,
                                color = BasicsColors.Secondary,
                                modifier = Modifier.clickable { viewModel.markLastSolvePenalty(-1) }
                            )
                            if (lastSolvePenalty != 0) {
                                Text(
                                    text = "undo",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = AppFont.Orbitron,
                                    color = BasicsColors.Secondary,
                                    modifier = Modifier.clickable { viewModel.markLastSolvePenalty(0) }
                                )
                            }
                            Text(
                                text = "delete",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFont.Orbitron,
                                color = BasicsColors.Error,
                                modifier = Modifier.clickable { showDeleteConfirm = true }
                            )
                        }
                    }
                }
            }

            // bottom: pb left, net middle, averages right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("session pb", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary, letterSpacing = 0.1.sp)
                    Text(
                        text = if (sessionPb != null) TimeFormatter.formatTime(sessionPb!!) else "---",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("overall pb (${puzzleType.displayName.lowercase()})", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary, letterSpacing = 0.1.sp)
                    Text(
                        text = if (overallPb != null) TimeFormatter.formatTime(overallPb!!) else "---",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Primary
                    )
                }

                if (scramble.isNotBlank()) {
                    Box(
                        modifier = Modifier.clickable { showNetPopup = true },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (inputMode == TimerInputMode.MANUAL) {
                            CubeNetFromColors(
                                colors = manualColors,
                                modifier = Modifier
                                    .height(72.dp)
                                    .aspectRatio(12f / 9f)
                            )
                        } else {
                            CubeNet(
                                scramble = scramble,
                                puzzleType = puzzleType,
                                modifier = Modifier
                                    .height(72.dp)
                                    .aspectRatio(12f / 9f)
                            )
                        }
                    }
                }

                if (stats.totalSolves > 0) {
                    val extraAverages = listOfNotNull(
                        if (50 in enabledAverages) "ao50" to stats.ao50 else null,
                        if (100 in enabledAverages) "ao100" to stats.ao100 else null,
                        if (200 in enabledAverages) "ao200" to stats.ao200 else null,
                        if (500 in enabledAverages) "ao500" to stats.ao500 else null
                    )
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text("ao5", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary, letterSpacing = 0.1.sp)
                        Text(
                            text = TimeFormatter.formatStatTime(stats.ao5),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("ao12", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary, letterSpacing = 0.1.sp)
                        Text(
                            text = TimeFormatter.formatStatTime(stats.ao12),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary
                        )
                        extraAverages.forEach { (label, value) ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary, letterSpacing = 0.1.sp)
                            Text(
                                text = TimeFormatter.formatStatTime(value),
                                fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary
                            )
                        }
                    }
                }
            }
        }

        if (showCustomDialog) {
            AlertDialog(
                onDismissRequest = { showCustomDialog = false },
                containerColor = BasicsColors.SurfaceContainer,
                titleContentColor = BasicsColors.Primary,
                textContentColor = BasicsColors.Secondary,
                shape = RoundedCornerShape(0.dp),
                title = {
                    Text("input scramble", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                },
                text = {
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = AppFont.Orbitron,
                            fontSize = 13.sp,
                            color = BasicsColors.Primary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BasicsColors.Primary,
                            unfocusedBorderColor = BasicsColors.Border,
                            cursorColor = BasicsColors.Primary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            viewModel.setCustomScramble(customInput.trim())
                            customInput = ""
                            showCustomDialog = false
                        })
                    )
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            customInput = ""
                            showCustomDialog = false
                            showCubeInput = true
                        }) {
                            Text("paint cube", fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            viewModel.setCustomScramble(customInput.trim())
                            customInput = ""
                            showCustomDialog = false
                        }) {
                            Text("apply", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontSize = 12.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        customInput = ""
                        showCustomDialog = false
                    }) {
                        Text("cancel", fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary, fontSize = 12.sp)
                    }
                }
            )
        }

        if (showCubeInput) {
            CubeInputDialog(
                initialColors = manualColors,
                onDismiss = { showCubeInput = false },
                onConfirm = { colors ->
                    viewModel.setInputMode(TimerInputMode.MANUAL)
                    viewModel.setManualColors(colors)
                    showCubeInput = false
                }
            )
        }

        if (showNetPopup && scramble.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { showNetPopup = false },
                containerColor = BasicsColors.SurfaceContainer,
                titleContentColor = BasicsColors.Primary,
                textContentColor = BasicsColors.Secondary,
                shape = RoundedCornerShape(0.dp),
                title = {
                    Text("cube net", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                },
                text = {
                    Column {
                        if (inputMode == TimerInputMode.MANUAL) {
                            CubeNetFromColors(
                                colors = manualColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(12f / 9f)
                            )
                        } else {
                            CubeNet(
                                scramble = scramble,
                                puzzleType = puzzleType,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(12f / 9f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (inputMode == TimerInputMode.MANUAL) "[manual cube state]" else scramble,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFont.Orbitron,
                            color = BasicsColors.Secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showNetPopup = false }) {
                        Text("close", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontSize = 12.sp)
                    }
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = BasicsColors.SurfaceContainer,
                titleContentColor = BasicsColors.Primary,
                textContentColor = BasicsColors.Secondary,
                shape = RoundedCornerShape(0.dp),
                title = { Text("delete solve?", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold) },
                text = { Text("this cannot be undone.", fontFamily = AppFont.Orbitron) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteLastSolveFromButton()
                        showDeleteConfirm = false
                    }) { Text("delete", fontFamily = AppFont.Orbitron, color = BasicsColors.Error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("cancel", fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary)
                    }
                }
            )
        }
    }
}

@Composable
fun ArmedText(
    text: String,
    progress: Float,
    fontSize: androidx.compose.ui.unit.TextUnit,
    normalColor: Color,
    armedColor: Color = BasicsColors.Armed,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp
) {
    val baseDensity = LocalDensity.current
    val cappedDensity = Density(baseDensity.density, baseDensity.fontScale.coerceAtMost(1.2f))
    CompositionLocalProvider(LocalDensity provides cappedDensity) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = AppFont.Orbitron,
            color = normalColor,
            letterSpacing = letterSpacing,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fillHeight = size.height * progress
                    drawRect(
                        color = armedColor,
                        topLeft = Offset(0f, size.height - fillHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, fillHeight),
                        blendMode = BlendMode.SrcIn
                    )
                }
        )
    }
}
