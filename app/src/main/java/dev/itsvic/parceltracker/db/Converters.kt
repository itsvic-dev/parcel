// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime

class Converters {
  @TypeConverter
  fun fromTimestamp(value: Long?): Instant? {
    return value?.let { Instant.ofEpochMilli(it) }
  }

  @TypeConverter
  fun instantToTimestamp(instant: Instant?): Long? {
    return instant?.toEpochMilli()
  }

  @TypeConverter
  fun dateTimeFromString(value: String?): LocalDateTime? {
    return value?.let { LocalDateTime.parse(it) }
  }

  @TypeConverter
  fun dateTimeToString(value: LocalDateTime?): String? {
    return value?.toString()
  }
}
