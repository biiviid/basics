package com.basicsapp.timer.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.basicsapp.timer.data.models.Session
import com.basicsapp.timer.data.models.Solve
import com.basicsapp.timer.ui.components.CubieCube
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BasicsExport(
    val version: Int = 1,
    val app: String = "basics.",
    val exportedAt: Long = System.currentTimeMillis(),
    val sessions: List<ExportSession>
)

data class ExportSession(
    val name: String,
    val puzzleType: String,
    val createdAt: Long,
    val solves: List<ExportSolve>
)

data class ExportSolve(
    val timeMs: Long,
    val penalty: Int,
    val scramble: String = "",
    val createdAt: Long
)

object FileExporter {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun exportSession(context: Context, session: Session, solves: List<Solve>): String? {
        val exportData = BasicsExport(
            sessions = listOf(
                ExportSession(
                    name = session.name,
                    puzzleType = session.puzzleType,
                    createdAt = session.createdAt,
                    solves = solves.map { ExportSolve(it.timeMs, it.penalty, createdAt = it.createdAt) }
                )
            )
        )
        val json = gson.toJson(exportData)
        return writeJsonToDownloads(
            context, json,
            "basics_${session.name.replace(" ", "_")}_${System.currentTimeMillis()}.json"
        )
    }

    /**
     * csTimer-compatible JSON export, the de-facto import format used by other cubing
     * timers. Manual cube-state solves are exported as a real equivalent WCA scramble
     * produced by KociembaSolver, so the manual state survives the move to another timer.
     */
    fun exportSessionCsTimer(
        context: Context,
        session: Session,
        solves: List<Solve>,
        onProgress: ((Float) -> Unit)? = null
    ): String? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        // progress units: the solver table build (first use only) plus one per manual solve
        val manualCount = solves.count { it.scramble.startsWith("manual:") }
        val initNeeded = manualCount > 0 && !KociembaSolver.isInitialized()
        val totalUnits = (manualCount + if (initNeeded) 1 else 0).toFloat().coerceAtLeast(1f)
        var unitsDone = 0f
        val unit = 1f / totalUnits

        fun scrambleFor(raw: String): String {
            if (!raw.startsWith("manual:")) return raw
            val colors = CubieCube.colorsFromFaceString(raw.removePrefix("manual:"))
            val cc = CubieCube.buildCubieFromColors(colors) ?: return ""
            val scramble = KociembaSolver.scrambleFor(cc) { initFrac ->
                onProgress?.invoke(((unitsDone + initFrac * unit) / totalUnits).coerceIn(0f, 1f))
            }
            unitsDone += 1f
            onProgress?.invoke((unitsDone / totalUnits).coerceIn(0f, 1f))
            return scramble ?: ""
        }

        val exportData = listOf(
            mapOf(
                "session" to session.name,
                "puzzle" to csTimerPuzzleCode(PuzzleType.fromString(session.puzzleType)),
                "solves" to solves.map { solve ->
                    mapOf(
                        "time" to String.format(Locale.US, "%.2f", solve.timeMs / 1000.0),
                        "penalty" to when (solve.penalty) { 2 -> "+2"; -1 -> "DNF"; else -> "0" },
                        "scramble" to scrambleFor(solve.scramble),
                        "comment" to "",
                        "date" to dateFormat.format(Date(solve.createdAt))
                    )
                }
            )
        )
        val json = gson.toJson(exportData)
        onProgress?.invoke(1f)
        return writeJsonToDownloads(
            context, json,
            "cstimer_${session.name.replace(" ", "_")}_${System.currentTimeMillis()}.json"
        )
    }

    private fun csTimerPuzzleCode(puzzle: PuzzleType): String = when (puzzle) {
        PuzzleType.TWO_BY_TWO -> "222"
        PuzzleType.THREE_BY_THREE -> "333"
        PuzzleType.FOUR_BY_FOUR -> "444"
        PuzzleType.FIVE_BY_FIVE -> "555"
        PuzzleType.SIX_BY_SIX -> "666"
        PuzzleType.SEVEN_BY_SEVEN -> "777"
        PuzzleType.SKEWB -> "skewb"
        PuzzleType.PYRAMINX -> "pyraminx"
        PuzzleType.MEGAMINX -> "megaminx"
        PuzzleType.SQUARE_ONE -> "sq1"
        PuzzleType.CLOCK -> "clock"
    }

    private fun writeJsonToDownloads(context: Context, json: String, fileName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        os.write(json.toByteArray())
                    }
                }
                "Downloads/$fileName"
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.writeText(json)
                file.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }

    fun importSession(json: String): BasicsExport? {
        return try {
            gson.fromJson(json, BasicsExport::class.java)
        } catch (e: Exception) {
            null
        }
    }
}