// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    version = 3,
    entities = [Parcel::class, ParcelStatus::class, ParcelHistoryItem::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parcelDao(): ParcelDao
    abstract fun parcelStatusDao(): ParcelStatusDao
    abstract fun parcelHistoryDao(): ParcelHistoryDao
}
