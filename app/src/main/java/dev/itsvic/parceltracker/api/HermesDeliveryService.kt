package dev.itsvic.parceltracker.api

import android.content.Context
import com.squareup.moshi.JsonClass
import dev.itsvic.parceltracker.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path

object HermesDeliveryService : DeliveryService {
  override val nameResource: Int
    get() = R.string.service_hermes

  override val acceptsPostCode: Boolean
    get() = false

  override val requiresPostCode: Boolean
    get() = false

  override suspend fun getParcel(
      context: Context,
      trackingId: String,
      postalCode: String?
  ): Parcel {

    val resp =
        try {
          service.getShipments(trackingId)
        } catch (_: HttpException) {
          throw ParcelNonExistentException()
        }

    val status =
        when (resp.status.parcelStatus) {
          "ZUGESTELLT",
          "RETOURE_AUSGELIEFERT_BEIM_ATG" -> Status.Delivered
          "ZUSTELLTOUR" -> Status.OutForDelivery
          "AVISE" -> Status.Preadvice
          "SENDUNG_VON_HERMES_UEBERNOMMEN",
          "AM_PKS_ABGEGEBEN" -> Status.InWarehouse
          "UMSCHLAG_INLAND",
          "SENDUNG_IN_ZIELREGION_ANGEKOMMEN" -> Status.InTransit
          else -> logUnknownStatus("Hermes", resp.status.parcelStatus)
        }

    val statusReached = resp.parcelHistory.filter { it.timestamp != null }

    val history =
        statusReached.map {
          ParcelHistoryItem(
              it.statusHistoryText!!,
              LocalDateTime.parse(it.timestamp, DateTimeFormatter.ISO_DATE_TIME),
              "")
        }

    return Parcel(trackingId, history, status)
  }

  private val retrofit =
      Retrofit.Builder()
          .baseUrl("https://api.my-deliveries.de/tnt/parcelservice/")
          .client(api_client)
          .addConverterFactory(api_factory)
          .build()

  private val service = retrofit.create(API::class.java)

  private interface API {
    @GET("parceldetails/{id}")
    suspend fun getShipments(@Path("id") trackingId: String): HermesParcelData
  }

  @JsonClass(generateAdapter = true)
  internal data class HermesParcelData(
      val barcode: String,
      val receipt: String?,
      val order: String?,
      val status: HermesParcelStatus,
      val parcelHistory: List<HermesParcelHistory>
  )

  @JsonClass(generateAdapter = true)
  internal data class HermesParcelStatus(val parcelStatus: String, val timestamp: String)

  @JsonClass(generateAdapter = true)
  internal data class HermesParcelHistory(
      val timestamp: String?,
      val status: String,
      val statusHistoryText: String?
  )
}
