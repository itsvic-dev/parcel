// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.api

import com.squareup.moshi.JsonClass
import dev.itsvic.parceltracker.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

object IMileDeliveryService : DeliveryService {
  override val nameResource: Int = R.string.service_imile
  override val acceptsPostCode: Boolean = false
  override val requiresPostCode: Boolean = false

  private const val BASE_URL = "https://www.imile.com/"

  private val retrofit =
    Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(api_client)
      .addConverterFactory(api_factory)
      .build()

  private val service = retrofit.create(API::class.java)

  override suspend fun getParcel(trackingId: String, postCode: String?): Parcel {
    val response = service.getParcel(trackingId)

    if (response.status != "success" || response.resultObject == null) {
      throw ParcelNonExistentException()
    }

    val resultObject = response.resultObject
    val trackInfos = resultObject.trackInfos

    if (trackInfos.isEmpty()) {
      throw ParcelNonExistentException()
    }

    val history = trackInfos.map { trackInfo ->
      ParcelHistoryItem(
        description = trackInfo.content,
        time = LocalDateTime.parse(trackInfo.time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        location = trackInfo.operateStationName ?: ""
      )
    }

    val currentStatus = when (trackInfos.firstOrNull()?.trackStage) {
      1001 -> Status.Preadvice
      2004 -> Status.InWarehouse
      1002 -> Status.InTransit
      1003 -> Status.OutForDelivery
      else -> Status.Unknown
    }

    return Parcel(
      id = trackingId,
      history = history,
      currentStatus = currentStatus
    )
  }

  private interface API {
    @GET("saastms/mobileWeb/track/query")
    suspend fun getParcel(@Query("waybillNo") waybillNo: String): IMileResponse
  }

  @JsonClass(generateAdapter = true)
  data class IMileResponse(
    val status: String,
    val resultCode: String,
    val resultObject: IMileResultObject?,
    val message: String
  )

  @JsonClass(generateAdapter = true)
  data class IMileResultObject(
    val waybillNo: String,
    val sendSite: String?,
    val dispatchStation: String?,
    val country: String?,
    val trackInfos: List<IMileTrackInfo>
  )

  @JsonClass(generateAdapter = true)
  data class IMileTrackInfo(
    val content: String,
    val trackStage: Int,
    val trackStageTx: String,
    val time: String,
    val operateStationName: String?,
    val proofs: Any?
  )
}
