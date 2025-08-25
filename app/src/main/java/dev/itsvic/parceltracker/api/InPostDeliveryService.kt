package dev.itsvic.parceltracker.api

import android.os.LocaleList
import com.squareup.moshi.JsonClass
import dev.itsvic.parceltracker.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

// Reverse-engineered from https://inpost.pl/en/find-parcel
object InPostDeliveryService : DeliveryService {
  override val nameResource: Int = R.string.service_inpost
  override val acceptsPostCode: Boolean = false
  override val requiresPostCode: Boolean = false

  private const val BASE_URL = "https://inpost.pl/"

  private val retrofit =
      Retrofit.Builder()
          .baseUrl(BASE_URL)
          .client(api_client)
          .addConverterFactory(api_factory)
          .build()

  private val service = retrofit.create(API::class.java)

  override fun acceptsFormat(trackingId: String): Boolean {
    val inpostRegex = """^\d{24}$""".toRegex()
    return inpostRegex.matchEntire(trackingId) != null
  }

  override suspend fun getParcel(trackingId: String, postalCode: String?): Parcel {
    val language = mapLanguageToAPIFormat(LocaleList.getDefault().get(0).language)
    val response =
        try {
          service.getParcel(trackingId, true, language)
        } catch (_: Exception) {
          throw ParcelNonExistentException()
        }

    val parcelData = response.firstOrNull() ?: throw ParcelNonExistentException()

    return Parcel(
        parcelData.mainTrackingNumber,
        eventsToHistory(parcelData.events),
        eventCodeToStatus(parcelData.events.first().eventCode))
  }

  private fun mapLanguageToAPIFormat(language: String): String {
    return when (language) {
      "pl" -> "pl_PL"
      "uk" -> "en_UK"
      "ua" -> "uk_UA"
      else -> "en_EN" // Fallback to English
    }
  }

  private fun eventCodeToStatus(eventCode: String): Status {
    // All event codes: https://developers.inpost-group.com/tracking-events
    // COD: Cash On Delivery
    // PUDO: Pick-Up Drop-Off
    // APM: Automated Parcel Machine

    return when (eventCode) {
      "CRE.1001" -> Status.Preadvice // [Generic] Parcel Creation
      "CRE.1002" -> Status.AwaitingPickup // 	[Generic] Ready for acceptance
      "FMD.1001" -> Status.LockerboxAcceptedParcel // [Generic] Ready for courier collection
      "FMD.1002" -> Status.InTransit // [Generic] Collected by courier
      "FMD.1003" -> Status.InTransit // [Generic] In-transit (first-mile)
      "FMD.1004" -> Status.PickedUpByCourier // Collected by courier in PUDO
      "FMD.1005" -> Status.PickedUpByCourier // Collected by courier in APM
      "MMD.1001" -> Status.InWarehouse // [Generic] Adopted at Logistics Centre
      "MMD.1002" -> Status.InWarehouse // [Generic] Processed at Logistics Centre
      "MMD.1003" -> Status.InTransit // [Generic] Dispatched from Logistics Centre
      "MMD.1004" -> Status.InTransit // [Generic] Line-Haul
      "LMD.1001" -> Status.InTransit // [Generic] In-transit (last-mile)
      "LMD.1002" -> Status.Delivered // [Generic] Arrived at destination
      "LMD.1003" -> Status.AwaitingPickup // [Generic] Ready to collect
      "LMD.1004" -> Status.AwaitingPickup // Ready to collect PUDO
      "LMD.1005" -> Status.AwaitingPickup // Ready to collect APM
      "LMD.3002" -> Status.Readdressed // Alternative temporary collection point assigned
      "LMD.3003" -> Status.Readdressed // Alternative collection point assigned
      "LMD.3004" -> Status.InTransit // Branch collection assigned
      "LMD.3005" -> Status.Readdressed // Original collection point reassigned
      "LMD.3006" -> Status.Readdressed // Delivery readdressed
      "LMD.3007" -> Status.AwaitingPickup // Stored temporary in a service point
      "LMD.3008" -> Status.DeliveryFailure // Expired stored parcel
      "LMD.3009" -> Status.DeliveryFailure // Expired on temporary box machine
      "LMD.3010" -> Status.DeliveryFailure // Expired on temporary box machine
      "LMD.3011" -> Status.DeliveryFailure // Expired on temporary collection point
      "LMD.3012" ->
          Status
              .DeliveryFailure // Redirect cancelled, The redirection of the parcel wasn’t possible
      "LMD.3013" -> Status.Readdressed // Redirected to PUDO
      "LMD.3014" -> Status.Readdressed // Redirected to APM
      "CC.001" -> Status.Customs // Parcel at customs
      "CC.002" -> Status.CustomsSuccess // Parcel customs cleared
      "CC.003" -> Status.CustomsHeld // Parcel held at customs
      "LMD.9001" -> Status.PickupTimeEndingSoon // Reminder to collect
      "LMD.9002" -> Status.DeliveryFailure // Expired
      "LMD.9003" -> Status.DeliveryFailure // Oversized
      "LMD.9004" -> Status.DeliveryFailure // [Generic] Attempted delivery
      "LMD.9005" -> Status.DeliveryFailure // [Generic] Undeliverable
      "LMD.9006" -> Status.DeliveryFailure // Undeliverable: Rejected by recipient
      "LMD.9007" -> Status.DeliveryFailure // Undeliverable: Incorrect delivery details
      "LMD.9008" -> Status.DeliveryFailure // Undeliverable: Receiver unknown
      "LMD.9009" -> Status.DeliveryFailure // Undeliverable: COD conditions not met
      "LMD.9010" -> Status.DeliveryFailure // Undeliverable: No mailbox
      "LMD.9011" -> Status.DeliveryFailure // Undeliverable: No access to location
      "LMD.9012" -> Status.AwaitingPickup // Stored temporary in a box machine
      "LMD.9013" -> Status.AwaitingPickup // Parcel ready to collect at customer service point
      "EOL.1001" -> Status.Delivered // [Generic] Delivered
      "EOL.1002" -> Status.PickedUp // Parcel collected
      "EOL.1003" -> Status.DeliveredToASafePlace // Delivered at Safe Place
      "EOL.1004" -> Status.DeliveredToNeighbor // Delivered at neighbour
      "EOL.1005" -> Status.Delivered // Delivered with verified recipient
      "EOL.9001" -> Status.DeliveryFailure // Missing
      "EOL.9002" -> Status.Damaged // Damaged
      "EOL.9003" -> Status.Destroyed // Destroyed
      "EOL.9004" -> Status.DeliveryFailure // Cancelled
      "RTS.1001" -> Status.ReturningToSender // [Generic] Returning to Sender
      "RTS.1002" -> Status.ReturnedToSender // [Generic] Returned to Sender
      "FUL.1001" -> Status.InWarehouse // [Generic] Picked
      "FUL.1002" -> Status.InWarehouse // [Generic] Packed
      "FUL.1003" -> Status.OutForDelivery // [Generic] Dispatched
      "INF.1001" -> Status.Unknown // FIXME: COD payment received. Is this even displayed on
      // FIXME: the tracking site? I guess it may be their internal event code. Anyway, I
      // FIXME: don't see any matching status codes for this one
      "INF.9001" -> Status.Delayed // Delay in Delivery
      "HAN.1001" -> Status.InTransit // [Generic] Handover
      else -> logUnknownStatus("InPost", eventCode)
    }
  }

  private fun eventsToHistory(events: List<Event>): List<ParcelHistoryItem> {
    return events.map { item ->
      ParcelHistoryItem(
          item.eventDescription,
          LocalDateTime.parse(item.timestamp, DateTimeFormatter.ISO_DATE_TIME),
          item.location ?: "")
    }
  }

  private interface API {
    @GET("shipx-proxy/")
    @Headers(
        "X-Requested-With: XMLHttpRequest") // This is a necessary hack to spoof Ajax. Otherwise we
    // would get 403
    suspend fun getParcel(
        @Query("number") number: String,
        @Query("new_api") newApi: Boolean,
        @Query("language") language: String
    ): List<ParcelResponse>
  }

  @JsonClass(generateAdapter = true)
  internal data class ParcelResponse(
      val mainTrackingNumber: String,
      val status: Any?, // seems to always be null
      val statusTitle: Any?, // seems to always be null
      val statusDescription: Any?, // seems to always be null
      val trackingNumbers: List<TrackingNumbers>,
      val events: List<Event>
  )

  @JsonClass(generateAdapter = true)
  internal data class TrackingNumbers(val trackingNumber: String, val kind: String)

  @JsonClass(generateAdapter = true)
  internal data class Event(
      val eventCode: String,
      val status: Any?, // seems to always be null
      val timestamp: String, // format: 0000-00-00T00:00:00.000+00:00
      val carrier: String,
      val eventTitle: String,
      val eventDescription: String,
      val statusTitle: Any?, // seems to always be null
      val statusDescription: Any?, // seems to always be null
      val location: String? // seems to always be null
  )
}
