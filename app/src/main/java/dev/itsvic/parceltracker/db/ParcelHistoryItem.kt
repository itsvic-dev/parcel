package dev.itsvic.parceltracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class ParcelHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parcelId: Int,
    val description: String,
    val time: LocalDateTime,
    val location: String,
)
