// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import dev.itsvic.parceltracker.api.Status
import java.time.Instant

@Entity
data class ParcelStatus(
    @PrimaryKey val parcelId: Int,
    val status: Status,
    val lastChange: Instant,
)

@Dao
interface ParcelStatusDao {
  @Query("SELECT * FROM ParcelStatus WHERE parcelId=:parcelId")
  suspend fun get(parcelId: Int): ParcelStatus

  @Insert suspend fun insert(status: ParcelStatus)

  @Update suspend fun update(status: ParcelStatus)

  @Delete(entity = ParcelStatus::class) suspend fun deleteByParcelId(parcelId: ParcelId)
}
