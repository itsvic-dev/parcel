package dev.itsvic.parceltracker.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Entity
data class ParcelHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parcelId: Int,
    val description: String,
    val time: LocalDateTime,
    val location: String,
)

@Dao
interface ParcelHistoryDao {
    @Query("SELECT * FROM parcelhistoryitem WHERE parcelId=:id")
    fun getAllById(id: Int): Flow<List<ParcelHistoryItem>>

    @Insert
    suspend fun insert(item: ParcelHistoryItem)
}
