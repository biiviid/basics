package com.basicsapp.timer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.basicsapp.timer.ui.screens.*
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.utils.FileExporter
import com.basicsapp.timer.utils.PuzzleType
import com.basicsapp.timer.viewmodel.TimerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BasicsMainScreen()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicsMainScreen() {
    val viewModel: TimerViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var showSessionMenu by remember { mutableStateOf(false) }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var longPressedSession by remember { mutableStateOf<com.basicsapp.timer.data.models.Session?>(null) }
    var exportProgress by remember { mutableStateOf<Float?>(null) }
    var showDeleteSessionDialog by remember { mutableStateOf(false) }

    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val puzzleType by viewModel.puzzleType.collectAsState()
    val inspectionEnabled by viewModel.inspectionEnabled.collectAsState()
    val enabledAverages by viewModel.enabledAverages.collectAsState()
    val holdTimeMs by viewModel.holdTimeMs.collectAsState()
    val inspectionHoldStart by viewModel.inspectionHoldStart.collectAsState()
    val currentSession = sessions.find { it.id == currentSessionId }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val json = inputStream?.bufferedReader()?.use { r -> r.readText() }
                if (json != null) {
                    val imported = com.basicsapp.timer.utils.FileExporter.importSession(json)
                    if (imported != null) {
                        viewModel.importSession(imported)
                        Toast.makeText(context, "imported successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "invalid file format", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BasicsColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BasicsColors.Surface)
                .border(1.dp, BasicsColors.Border)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "basics.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppFont.Orbitron,
                color = BasicsColors.Primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Text(
                        text = "${currentSession?.name ?: "no session"} · ${puzzleType.displayName.lowercase()}",
                        fontSize = 14.sp,
                        fontFamily = AppFont.Orbitron,
                        color = BasicsColors.Secondary,
                        modifier = Modifier.clickable { showSessionMenu = true }
                    )

                    DropdownMenu(
                        expanded = showSessionMenu,
                        onDismissRequest = { showSessionMenu = false },
                        modifier = Modifier.background(BasicsColors.SurfaceContainer)
                    ) {
                        sessions.forEach { session ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            viewModel.selectSession(session.id)
                                            showSessionMenu = false
                                        },
                                        onLongClick = {
                                            longPressedSession = session
                                            showSessionMenu = false
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "${session.name} (${PuzzleType.fromString(session.puzzleType).displayName.lowercase()})",
                                    fontFamily = AppFont.Orbitron,
                                    fontSize = 14.sp,
                                    color = if (session.id == currentSessionId) BasicsColors.Primary else BasicsColors.Secondary,
                                    fontWeight = if (session.id == currentSessionId) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        HorizontalDivider(color = BasicsColors.Border)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSessionMenu = false
                                    showNewSessionDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "+ new session",
                                fontFamily = AppFont.Orbitron,
                                fontSize = 14.sp,
                                color = BasicsColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSessionMenu = false
                                    // open file picker for import
                                    importLauncher.launch(arrayOf("application/json"))
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "import from file",
                                fontFamily = AppFont.Orbitron,
                                fontSize = 14.sp,
                                color = BasicsColors.Secondary
                            )
                        }
                    }
                }

                Text(
                    text = "\u2699",
                    fontSize = 18.sp,
                    color = BasicsColors.Secondary,
                    modifier = Modifier.clickable { showSettings = true }
                )
            }
        }

        // tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BasicsColors.Surface)
                .border(1.dp, BasicsColors.Border)
        ) {
            listOf("timer", "history", "stats", "charts").forEachIndexed { index, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = index }
                        .background(if (selectedTab == index) BasicsColors.Background else BasicsColors.Surface)
                        .border(1.dp, BasicsColors.Border)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFont.Orbitron,
                        color = if (selectedTab == index) BasicsColors.Primary else BasicsColors.Tertiary,
                        letterSpacing = 0.1.sp
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> TimerScreen(viewModel = viewModel)
            1 -> HistoryScreen(viewModel = viewModel)
            2 -> StatsScreen(viewModel = viewModel)
            3 -> ChartsScreen(viewModel = viewModel)
        }
    }

    // progress overlay while exporting a session to cstimer (solver table build + solving)
    if (exportProgress != null) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = BasicsColors.SurfaceContainer,
            shape = RoundedCornerShape(0.dp),
            title = {
                Text("exporting to cstimer", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "building cube solver / solving states",
                        fontSize = 11.sp,
                        fontFamily = AppFont.Orbitron,
                        color = BasicsColors.Tertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(44.dp)
                            .border(2.dp, BasicsColors.Primary)
                            .background(BasicsColors.Surface),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(exportProgress!!)
                                .background(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${(exportProgress!! * 100).toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFont.Orbitron,
                        color = BasicsColors.Primary
                    )
                }
            },
            confirmButton = {}
        )
    }

    // long-press session action sheet
    longPressedSession?.let { session ->
        AlertDialog(
            onDismissRequest = { longPressedSession = null },
            containerColor = BasicsColors.SurfaceContainer,
            titleContentColor = BasicsColors.Primary,
            textContentColor = BasicsColors.Secondary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            title = {
                Text(session.name, fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    PuzzleType.fromString(session.puzzleType).displayName.lowercase(),
                    fontFamily = AppFont.Orbitron,
                    fontSize = 12.sp,
                    color = BasicsColors.Tertiary
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        scope.launch {
                            val sessionData = viewModel.getSessionForExport(session.id)
                            val solves = viewModel.getSolvesForExport(session.id)
                            if (sessionData != null) {
                                val path = FileExporter.exportSession(context, sessionData, solves)
                                Toast.makeText(context, if (path != null) "exported to $path" else "export failed", Toast.LENGTH_LONG).show()
                            }
                        }
                        longPressedSession = null
                    }) {
                        Text("export", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = {
                        scope.launch {
                            val sessionData = viewModel.getSessionForExport(session.id)
                            val solves = viewModel.getSolvesForExport(session.id)
                            if (sessionData != null) {
                                val hasManual = solves.any { it.scramble.startsWith("manual:") }
                                if (hasManual) exportProgress = 0f
                                val path = withContext(Dispatchers.IO) {
                                    FileExporter.exportSessionCsTimer(context, sessionData, solves) { p ->
                                        if (hasManual) exportProgress = p
                                    }
                                }
                                exportProgress = null
                                Toast.makeText(context, if (path != null) "exported to $path" else "export failed", Toast.LENGTH_LONG).show()
                            }
                        }
                        longPressedSession = null
                    }) {
                        Text("cstimer", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontWeight = FontWeight.Bold)
                    }

                    TextButton(onClick = {
                        showDeleteSessionDialog = true
                    }) {
                        Text("delete", fontFamily = AppFont.Orbitron, color = BasicsColors.Error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { longPressedSession = null }) {
                    Text("cancel", fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary)
                }
            }
        )
    }

    // delete confirmation
    if (showDeleteSessionDialog && longPressedSession != null) {
        AlertDialog(
            onDismissRequest = { showDeleteSessionDialog = false },
            containerColor = BasicsColors.SurfaceContainer,
            titleContentColor = BasicsColors.Primary,
            textContentColor = BasicsColors.Secondary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            title = { Text("delete \"${longPressedSession!!.name}\"?", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold) },
            text = { Text("all solves will be permanently deleted.", fontFamily = AppFont.Orbitron) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(longPressedSession!!.id)
                    showDeleteSessionDialog = false
                    longPressedSession = null
                }) { Text("delete", fontFamily = AppFont.Orbitron, color = BasicsColors.Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSessionDialog = false }) {
                    Text("cancel", fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary)
                }
            }
        )
    }

    // settings popup - rectangular style
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            containerColor = BasicsColors.SurfaceContainer,
            titleContentColor = BasicsColors.Primary,
            textContentColor = BasicsColors.Secondary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            title = {
                Text("settings", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.1.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // extra averages toggles
                    listOf(50, 100, 200, 500).forEach { size ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ao$size", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary, letterSpacing = 0.1.sp)
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 24.dp)
                                    .background(if (size in enabledAverages) BasicsColors.Primary else BasicsColors.SurfaceHighest)
                                    .border(1.dp, BasicsColors.Border)
                                    .clickable { viewModel.toggleAverage(size) },
                                contentAlignment = if (size in enabledAverages) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 18.dp, height = 18.dp)
                                        .background(if (size in enabledAverages) BasicsColors.Background else BasicsColors.Tertiary)
                                        .padding(2.dp)
                                )
                            }
                        }
                        if (size != 500) HorizontalDivider(color = BasicsColors.Border)
                    }

                    HorizontalDivider(color = BasicsColors.Border)

                    // inspection toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("wca inspection", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary, letterSpacing = 0.1.sp)
                            Text("15s countdown, +2 if over", fontFamily = AppFont.Orbitron, fontSize = 11.sp, color = BasicsColors.Tertiary)
                        }
                        // rectangular toggle
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 24.dp)
                                .background(if (inspectionEnabled) BasicsColors.Primary else BasicsColors.SurfaceHighest)
                                .border(1.dp, BasicsColors.Border)
                                .clickable { viewModel.toggleInspection() },
                            contentAlignment = if (inspectionEnabled) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 18.dp, height = 18.dp)
                                    .background(if (inspectionEnabled) BasicsColors.Background else BasicsColors.Tertiary)
                                    .padding(2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BasicsColors.Border)

                    // hold to start inspection toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("hold to start inspection", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary, letterSpacing = 0.1.sp)
                            Text("press starts countdown, release starts solve", fontFamily = AppFont.Orbitron, fontSize = 11.sp, color = BasicsColors.Tertiary)
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 44.dp, height = 24.dp)
                                .background(if (inspectionHoldStart) BasicsColors.Primary else BasicsColors.SurfaceHighest)
                                .border(1.dp, BasicsColors.Border)
                                .clickable { viewModel.toggleInspectionHoldStart() },
                            contentAlignment = if (inspectionHoldStart) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 18.dp, height = 18.dp)
                                    .background(if (inspectionHoldStart) BasicsColors.Background else BasicsColors.Tertiary)
                                    .padding(2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BasicsColors.Border)

                    // arm hold time slider (0 - 1s)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("arm hold time", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary, letterSpacing = 0.1.sp)
                            Text(String.format("%.2f", holdTimeMs / 1000.0) + "s", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Secondary)
                        }
                        Slider(
                            value = holdTimeMs.toFloat(),
                            onValueChange = { viewModel.setHoldTimeMs(it.toInt()) },
                            valueRange = 0f..1000f,
                            steps = 19,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = BasicsColors.Primary,
                                activeTrackColor = BasicsColors.Primary,
                                inactiveTrackColor = BasicsColors.SurfaceHighest
                            )
                        )
                    }

                    HorizontalDivider(color = BasicsColors.Border)

                    // privacy policy - opens the embedded (offline) policy dialog
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showSettings = false
                                showPrivacyPolicy = true
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("privacy policy", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary, letterSpacing = 0.1.sp)
                        Text(">", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Tertiary)
                    }

                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("close", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // privacy policy popup - embedded text (fully offline), matches the hosted policy
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            containerColor = BasicsColors.SurfaceContainer,
            titleContentColor = BasicsColors.Primary,
            textContentColor = BasicsColors.Secondary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            title = {
                Text("privacy policy", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.1.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("basics. is a speedcubing timer application. We do not collect, store, or transmit any personal information or user data.", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Secondary)
                    Text("Data Storage", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary)
                    Text("All data (solve times, scrambles, session information) is stored locally on your device. No data is sent to any server or third party.", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Secondary)
                    Text("Internet Connection", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary)
                    Text("basics. does not require an internet connection to function. The app does not communicate with any remote servers.", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Secondary)
                    Text("Third-Party Services", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary)
                    Text("basics. does not integrate any third-party analytics, advertising, or tracking services.", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Secondary)
                    Text("Children's Privacy", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary)
                    Text("Since we do not collect any personal information, the app is safe for users of all ages.", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Secondary)
                    Text("Changes", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary)
                    Text("This privacy policy may be updated from time to time. Any changes will be reflected in the \"Last updated\" date above.", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Secondary)
                    Text("Contact", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BasicsColors.Primary)
                    Text("If you have questions about this privacy policy, you can reach us at: basics.timer.app@gmail.com", fontFamily = AppFont.Orbitron, fontSize = 12.sp, color = BasicsColors.Secondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) {
                    Text("close", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // new session dialog
    if (showNewSessionDialog) {
        var sessionName by remember { mutableStateOf("") }
        var selectedPuzzle by remember { mutableStateOf(puzzleType) }
        AlertDialog(
            onDismissRequest = { showNewSessionDialog = false },
            containerColor = BasicsColors.SurfaceContainer,
            titleContentColor = BasicsColors.Primary,
            textContentColor = BasicsColors.Secondary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            title = { Text("new session", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it.take(50) },
                        label = { Text("session name", fontFamily = AppFont.Orbitron) },
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BasicsColors.Primary,
                            unfocusedTextColor = BasicsColors.Secondary,
                            focusedBorderColor = BasicsColors.Primary,
                            unfocusedBorderColor = BasicsColors.Border,
                            focusedLabelColor = BasicsColors.Primary,
                            unfocusedLabelColor = BasicsColors.Tertiary,
                            cursorColor = BasicsColors.Primary
                        )
                    )

                    Text("puzzle type", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BasicsColors.Tertiary, letterSpacing = 0.1.sp)

                    val rows = PuzzleType.entries.chunked(3)
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { puzzle ->
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, if (selectedPuzzle == puzzle) BasicsColors.Primary else BasicsColors.Border)
                                        .background(if (selectedPuzzle == puzzle) BasicsColors.SurfaceHighest else BasicsColors.Surface)
                                        .clickable { selectedPuzzle = puzzle }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = puzzle.displayName.lowercase(),
                                        fontFamily = AppFont.Orbitron,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedPuzzle == puzzle) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedPuzzle == puzzle) BasicsColors.Primary else BasicsColors.Tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (sessionName.isNotBlank()) {
                        viewModel.createSession(sessionName.trim().lowercase(), selectedPuzzle)
                        showNewSessionDialog = false
                    }
                }) { Text("create", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showNewSessionDialog = false }) {
                    Text("cancel", fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary)
                }
            }
        )
    }
}
