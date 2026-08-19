package com.basicsapp.timer.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.basicsapp.timer.data.models.Session
import com.basicsapp.timer.data.models.Solve
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

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
        val fileName = "basics_${session.name.replace(" ", "_")}_${System.currentTimeMillis()}.json"

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
