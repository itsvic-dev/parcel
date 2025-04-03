package dev.itsvic.parceltracker.db

import android.content.Context
import dev.itsvic.parceltracker.ParcelApplication

suspend fun Context.deleteParcel(parcel: Parcel) {
    val db = ParcelApplication.db
    val parcelId = ParcelId(parcel.id)
    db.parcelDao().delete(parcel)
    db.parcelStatusDao().deleteByParcelId(parcelId)
    if (parcel.isArchived)
        db.parcelHistoryDao().deleteByParcelId(parcelId)
}
