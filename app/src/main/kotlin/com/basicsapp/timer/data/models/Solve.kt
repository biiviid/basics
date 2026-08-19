package com.basicsapp.timer.data.models

import androidx.room.*

@Entity(
    tableName = "solves",
    foreignKeys = [ForeignKey(
        entity = Session::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sessionId"])]
)
data class Solve(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val timeMs: Long,
    val penalty: Int = 0,
    val scramble: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
