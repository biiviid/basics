package com.basicsapp.timer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basicsapp.timer.ui.theme.AppFont
import com.basicsapp.timer.ui.theme.BasicsColors

/**
 * Thin input layer on top of the shared cube-state model (color indices 0-5, U/R/F/D/L/B).
 * Renders with the exact same net layout + palette as CubeNetFromColors, and writes taps
 * into the same IntArray(54) structure — no parallel state or renderer.
 */
@Composable
fun CubeInputDialog(
    initialColors: IntArray,
    onDismiss: () -> Unit,
    onConfirm: (IntArray) -> Unit
) {
    var colors by remember { mutableStateOf(initialColors.copyOf()) }
    var selectedColor by remember { mutableStateOf(0) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BasicsColors.SurfaceContainer,
        titleContentColor = BasicsColors.Primary,
        textContentColor = BasicsColors.Secondary,
        shape = RoundedCornerShape(0.dp),
        title = {
            Text("cube state", fontFamily = AppFont.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.1.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // the net renders through the shared CubeNetFromColors pipeline;
                // an invisible tap layer maps taps to facelets via netPosToFacelet
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(12f / 9f)) {
                    CubeNetFromColors(colors, Modifier.fillMaxSize())
                    Canvas(
                        onDraw = {},
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val cols = 12
                                    val rows = 9
                                    val cellW = size.width / cols
                                    val cellH = size.height / rows
                                    val col = (offset.x / cellW).toInt().coerceIn(0, cols - 1)
                                    val row = (offset.y / cellH).toInt().coerceIn(0, rows - 1)
                                    val facelet = netPosToFacelet(col, row)
                                    // centers are locked — face identity can't be broken by mispainting
                                    if (facelet != null && facelet % 9 != 4) {
                                        colors = colors.copyOf().also { it[facelet] = selectedColor }
                                        validationError = null
                                    }
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // palette — same colors the net uses
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0 until 6) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(
                                    width = if (selectedColor == i) 2.dp else 1.dp,
                                    color = if (selectedColor == i) BasicsColors.Primary else BasicsColors.Border
                                )
                                .background(Color(PALETTE[i]))
                                .clickable { selectedColor = i }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "select a color, then tap stickers to paint · centers are locked",
                    fontSize = 11.sp,
                    fontFamily = AppFont.Orbitron,
                    color = BasicsColors.Tertiary,
                    letterSpacing = 0.1.sp
                )
                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        fontSize = 11.sp,
                        fontFamily = AppFont.Orbitron,
                        color = BasicsColors.Error,
                        letterSpacing = 0.1.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val result = CubieCube.validateColors(colors)
                if (result == CubieCube.ValidationResult.VALID) {
                    onConfirm(colors)
                } else {
                    validationError = result.message
                }
            }) {
                Text("use this state", fontFamily = AppFont.Orbitron, color = BasicsColors.Primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("cancel", fontFamily = AppFont.Orbitron, color = BasicsColors.Tertiary)
            }
        }
    )
}