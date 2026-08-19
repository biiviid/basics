package com.basicsapp.timer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.ui.components.CubeNet
import com.basicsapp.timer.ui.components.SolveCard
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.ui.theme.BasicsColors
import com.basicsapp.timer.utils.TimeFormatter
import com.basicsapp.timer.viewmodel.TimerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: TimerViewModel) {
    val solves by viewModel.solves.collectAsState()
    val puzzleType by viewModel.puzzleType.collectAsState()
    var selectedSolveId by remember { mutableStateOf<Int?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    var selectionMode by remember { mutableStateOf(false) }
    var sortDescending by remember { mutableStateOf(true) }
    var sortByTime by remember { mutableStateOf(false) }
    var timeSortAscending by remember { mutableStateOf(true) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }

    val monthFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    val availableMonths = remember(solves) {
        solves.map { monthFormat.format(Date(it.createdAt)) }.distinct()
    }
    val filteredSolves = remember(solves, sortDescending, sortByTime, timeSortAscending, selectedMonth) {
        val filtered = if (selectedMonth != null) {
            solves.filter { monthFormat.format(Date(it.createdAt)) == selectedMonth }
        } else solves
        when {
            sortByTime && timeSortAscending -> filtered.sortedBy { it.timeMs }
            sortByTime -> filtered.sortedByDescending { it.timeMs }
            sortDescending -> filtered.reversed()
            else -> filtered
        }
    }

    // detect repeated scrambles (scrambles that appear more than once)
    val scrambleCounts = remember(solves) {
        solves.filter { it.scramble.isNotBlank() }
            .groupBy { it.scramble }
            .mapValues { it.value.size }
    }
    val repeatedScrambles = remember(scrambleCounts) {
        scrambleCounts.filter { it.value > 1 }.keys
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BasicsColors.Background)
    ) {
        if (solves.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "no solves recorded",
                    fontFamily = AppFont.Orbitron,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BasicsColors.Tertiary,
                    letterSpacing = 0.1.sp
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // filter bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BasicsColors.Surface)
                        .border(1.dp, BasicsColors.Border)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // sort by time toggle
                    Box(
                        modifier = Modifier
                            .border(1.dp, if (sortByTime) BasicsColors.Primary else BasicsColors.Border)
                            .background(if (sortByTime) BasicsColors.SurfaceHighest else BasicsColors.Surface)
                            .clickable {
                                if (sortByTime) {
                                    timeSortAscending = !timeSortAscending
                                } else {
                                    sortByTime = true
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (!sortByTime) "time" else if (timeSortAscending) "time \u2191" else "time \u2193",
                            fontFamily = AppFont.Orbitron,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sortByTime) BasicsColors.Primary else BasicsColors.Tertiary
                        )
                    }

                    // sort by date toggle
                    Box(
                        modifier = Modifier
                            .border(1.dp, if (!sortByTime) BasicsColors.Primary else BasicsColors.Border)
                            .background(if (!sortByTime) BasicsColors.SurfaceHighest else BasicsColors.Surface)
                            .clickable {
                                if (!sortByTime) {
                                    sortDescending = !sortDescending
                                } else {
                                    sortByTime = false
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (sortByTime) "date" else if (sortDescending) "newest" else "oldest",
                            fontFamily = AppFont.Orbitron,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!sortByTime) BasicsColors.Primary else BasicsColors.Tertiary
                        )
                    }

                    // month chips
                    Box(
                        modifier = Modifier
                            .border(1.dp, if (selectedMonth == null) BasicsColors.Primary else BasicsColors.Border)
                            .background(if (selectedMonth == null) BasicsColors.SurfaceHighest else BasicsColors.Surface)
                            .clickable { selectedMonth = null }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "all",
                            fontFamily = AppFont.Orbitron,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMonth == null) BasicsColors.Primary else BasicsColors.Tertiary
                        )
                    }

                    availableMonths.forEach { month ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (selectedMonth == month) BasicsColors.Primary else BasicsColors.Border)
                                .background(if (selectedMonth == month) BasicsColors.SurfaceHighest else BasicsColors.Surface)
                                .clickable { selectedMonth = if (selectedMonth == month) null else month }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = month.lowercase(),
                                fontFamily = AppFont.Orbitron,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMonth == month) BasicsColors.Primary else BasicsColors.Tertiary
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(filteredSolves) { index, solve ->
                        val isRepeated = solve.scramble.isNotBlank() && solve.scramble in repeatedScrambles
                        SolveCard(
                            solve = solve,
                            index = index,
                            isRepeatedScramble = isRepeated,
                            selectionMode = selectionMode,
                            isSelected = solve.id in selectedIds,
                            onClick = { selectedSolveId = solve.id },
                            onLongClick = {
                                selectionMode = true
                                selectedIds = setOf(solve.id)
                            },
                            onToggleSelect = {
                                selectedIds = if (solve.id in selectedIds) selectedIds - solve.id else selectedIds + solve.id
                                if (selectedIds.isEmpty()) selectionMode = false
                            }
                        )
                    }
                }

                if (selectionMode) {
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
                            text = "${selectedIds.size} selected",
                            fontFamily = AppFont.Orbitron,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BasicsColors.Tertiary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .clickable {
                                        selectionMode = false
                                        selectedIds = emptySet()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("cancel", fontFamily = AppFont.Orbitron, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BasicsColors.Tertiary)
                            }
                            Box(
                                modifier = Modifier
                                    .border(1.dp, BasicsColors.Error)
                                    .clickable { showDeleteDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("delete", fontFamily = AppFont.Orbitron, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BasicsColors.Error)
                            }
                        }
                    }
                }
            }
        }
    }

    // solve detail dialog
    selectedSolveId?.let { solveId ->
        val solve = solves.find { it.id == solveId }
        solve?.let {
            AlertDialog(
                onDismissRequest = { selectedSolveId = null },
                containerColor = BasicsColors.SurfaceContainer,
                titleContentColor = BasicsColors.Primary,
                textContentColor = BasicsColors.Secondary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
                title = {
                    Column {
                        Text(
                            text = TimeFormatter.formatTime(solve.timeMs),
                            fontFamily = AppFont.Orbitron,
                            fontWeight = FontWeight.Bold,
                            color = if (solve.penalty == -1 || solve.penalty == 2) BasicsColors.Error else BasicsColors.Primary
                        )
                        if (solve.penalty == -1) {
                            Text("dnf", fontSize = 12.sp, fontFamily = AppFont.Orbitron, color = BasicsColors.Error)
                        }
                        if (solve.penalty == 2) {
                            Text("+2", fontSize = 12.sp, fontFamily = AppFont.Orbitron, color = BasicsColors.Error)
                        }
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.Start) {
                        if (solve.scramble.isNotBlank()) {
                            val words = solve.scramble.split(" ").filter { it.isNotBlank() }
                            val mid = (words.size + 1) / 2
                            val l1 = words.take(mid).joinToString("  ") { it.replace("'", " '") }
                            val l2 = words.drop(mid).joinToString("  ") { it.replace("'", " '") }
                            Text(
                                text = l1,
                                fontFamily = AppFont.Orbitron,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BasicsColors.Secondary,
                                lineHeight = 16.sp
                            )
                            if (l2.isNotBlank()) {
                                Text(
                                    text = l2,
                                    fontFamily = AppFont.Orbitron,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BasicsColors.Secondary,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // cube net for this solve
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CubeNet(
                                    scramble = solve.scramble,
                                    puzzleType = puzzleType,
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .aspectRatio(12f / 9f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Text(
                            text = when (solve.penalty) {
                                0 -> "no penalty"
                                2 -> "+2 penalty applied"
                                -1 -> "did not finish"
                                else -> ""
                            },
                            fontFamily = AppFont.Orbitron,
                            fontSize = 12.sp,
                            color = if (solve.penalty != 0) BasicsColors.Error else BasicsColors.Tertiary
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            viewModel.markPenalty(solveId, 2)
                            selectedSolveId = null
                        }) { Text("+2", fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary, fontSize = 12.sp) }

                        TextButton(onClick = {
                            viewModel.markPenalty(solveId, -1)
                            selectedSolveId = null
                        }) { Text("dnf", fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary, fontSize = 12.sp) }

                        TextButton(onClick = {
                            viewModel.markPenalty(solveId, 0)
                            selectedSolveId = null
                        }) { Text("clear", fontFamily = AppFont.Orbitron, color = BasicsColors.Secondary, fontSize = 12.sp) }

                        TextButton(onClick = {
                            showDeleteDialog = true
                        }) { Text("delete", fontFamily = AppFont.Orbitron, color = BasicsColors.Error, fontSize = 12.sp) }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedSolveId = null }) {
                        Text("close", fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary, fontSize = 12.sp)
                    }
                }
            )
        }
    }

    if (showDeleteDialog && (selectedSolveId != null || selectionMode)) {
        val isBatch = selectionMode && selectedIds.isNotEmpty()
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = BasicsColors.SurfaceContainer,
            titleContentColor = BasicsColors.Primary,
            textContentColor = BasicsColors.Secondary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            title = { Text(if (isBatch) "delete ${selectedIds.size} solves?" else "delete solve?", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold) },
            text = { Text("this cannot be undone.", fontFamily = AppFont.Orbitron) },
            confirmButton = {
                TextButton(onClick = {
                    if (isBatch) {
                        viewModel.deleteSolves(selectedIds)
                        selectedIds = emptySet()
                        selectionMode = false
                    } else {
                        viewModel.deleteSolve(selectedSolveId!!)
                        selectedSolveId = null
                    }
                    showDeleteDialog = false
                }) { Text("delete", fontFamily = AppFont.Orbitron, color = BasicsColors.Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("cancel", fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary)
                }
            }
        )
    }
}
