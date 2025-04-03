package dev.itsvic.parceltracker.db

import androidx.room.Dao
import androidx.room.Delete
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

data class ParcelId (
    val parcelId: Int,
)

@Dao
interface ParcelHistoryDao {
    @Query("SELECT * FROM parcelhistoryitem WHERE parcelId=:id")
    fun getAllById(id: Int): Flow<List<ParcelHistoryItem>>

    @Insert
    suspend fun insert(items: List<ParcelHistoryItem>)

    @Delete(entity = ParcelHistoryItem::class)
    suspend fun deleteByParcelId(parcelId: ParcelId)
}
