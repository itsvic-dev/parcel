package dev.itsvic.parceltracker.api

import com.squareup.moshi.JsonClass
import dev.itsvic.parceltracker.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

object DpdGerDeliveryService : DeliveryService {
  override val nameResource: Int
    get() = R.string.service_dpd_ger

  override val acceptsPostCode: Boolean
    get() = false

  override val requiresPostCode: Boolean
    get() = false

  override suspend fun getParcel(trackingId: String, postCode: String?): Parcel {
    val resp =
        try {
          service.getShipment(trackingId)
        } catch (_: HttpException) {
          throw ParcelNonExistentException()
        }

    val status =
        when (val current =
            resp.parcellifecycleResponse.parcelLifeCycleData.statusInfo
                .find { it.isCurrentStatus == true }
                ?.status) {
          "ACCEPTED" -> Status.Preadvice
          "ON_THE_ROAD" -> Status.InTransit
          "HANDOVER_CONSIGNOR_TO_PARCELSHOP",
          "AT_DELIVERY_DEPOT" -> Status.InWarehouse
          "OUT_FOR_DELIVERY" -> Status.OutForDelivery
          "DELIVERED" -> Status.Delivered
          else -> logUnknownStatus("DPD Germany", current!!)
        }

    val h =
        resp.parcellifecycleResponse.parcelLifeCycleData.statusInfo.filter {
          it.statusHasBeenReached
        }

    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")
    val history =
        h.map { ParcelHistoryItem(it.label, LocalDateTime.parse(it.date, formatter), it.location) }

    return Parcel(trackingId, history, status)
  }

  private val retrofit =
      Retrofit.Builder()
          .baseUrl("https://tracking.dpd.de/rest/plc/en_DE/")
          .client(api_client)
          .addConverterFactory(api_factory)
          .build()

  private val service = retrofit.create(API::class.java)

  private interface API {
    @GET("{id}") suspend fun getShipment(@Path("id") trackingId: String): ParcelData
  }

  @JsonClass(generateAdapter = true)
  internal data class ParcelData(val parcellifecycleResponse: ParcelLifeCycleResponse)

  @JsonClass(generateAdapter = true)
  internal data class ParcelLifeCycleResponse(val parcelLifeCycleData: ParcelLifeCycleData)

  @JsonClass(generateAdapter = true)
  internal data class ParcelLifeCycleData(
      val statusInfo: List<StatusInfo>,
  )

  @JsonClass(generateAdapter = true)
  internal data class StatusInfo(
      val status: String,
      val label: String,
      val statusHasBeenReached: Boolean,
      val isCurrentStatus: Boolean,
      val location: String = "Unknown",
      val date: String?
  )
}
