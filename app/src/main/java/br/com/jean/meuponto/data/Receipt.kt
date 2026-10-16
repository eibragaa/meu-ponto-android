package br.com.jean.meuponto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val capturedAt: Long,
    val workDate: String,
    val workTime: String,
    val imagePath: String,
    val recognizedText: String
)
