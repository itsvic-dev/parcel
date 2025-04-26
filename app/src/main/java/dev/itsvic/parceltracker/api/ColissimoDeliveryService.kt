// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.api

import android.content.Context
import android.os.LocaleList
import android.util.Log

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException

import dev.itsvic.parceltracker.R

import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ColissimoDeliveryService : DeliveryService {
    override val nameResource: Int = R.string.service_colissimo
    override val acceptsPostCode: Boolean = false
    override val requiresPostCode: Boolean = false
    override val requiresApiKey: Boolean = false

    override fun acceptsFormat(trackingId: String): Boolean {
        val parcelFormat = """^[A-Za-z0-9]{11,15}$""".toRegex()
        return parcelFormat.accepts(trackingId)
    }

    override suspend fun getParcel(
        context: Context,
        trackingId: String,
        postalCode: String?
    ): Parcel {
        val locale = LocaleList.getDefault().get(0)

        val resp = try {
            service.getShipments(trackingId, locale.language)
        } catch (_: HttpException) {
            try {
                service.getShipments(trackingId, "en") // Retry once with fallback to english
            }
            catch(_: HttpException) {
                throw ParcelNonExistentException()
            }
        }
        catch (e: JsonDataException) {
            // Something's wrong with the parsing
            Log.d("Colissimo", e.toString())
            throw UnsupportedResponseException()
        }

        if(resp.isEmpty()) throw ParcelNonExistentException()
        val shipment = resp[0].shipment

        var status = Status.Unknown
        val events = shipment.event.sortedByDescending { it.order }

        if(events.isNotEmpty()) {
            val lastEvent = events.first()
            status = when (lastEvent.code) {
                "DR1" -> Status.Preadvice // Delivery declaration received
                "DR2" -> Status.DeliveryFailure // Issue during preparation
                "PC1" -> Status.InWarehouse // Handled
                "PC2" -> Status.InWarehouse // Handled in the expediting country
                "ET1" -> Status.InTransit // Processing
                "ET2" -> Status.InTransit // Processing in the expediting country
                "ET3" -> Status.InTransit // Processing in the destination country
                "ET4" -> Status.InTransit // Processing in a transit country
                "EP1" -> Status.Unknown // Waiting presentation
                "DO1" -> Status.Customs // Entered customs
                "DO2" -> Status.Customs // Out of customs
                "DO3" -> Status.Customs // Held in customs
                "PB1" -> Status.Unknown // Issue in progress
                "PB2" -> Status.Unknown // Issue resolved
                "MD2" -> Status.OutForDelivery // Distributing
                "ND1" -> Status.DeliveryFailure // Impossible to distribute
                "AG1" -> Status.AwaitingPickup // Awaiting pickup at counter
                "RE1" -> Status.DeliveryFailure // Returned to the expeditor
                "DI0" -> Status.Delivered // Distributed in lot
                "DI1" -> Status.Delivered // Distributed
                "DI2" -> Status.DeliveryFailure // Distributed to the expeditor (If sent back I suppose)
                "DI3" -> Status.OutForDelivery // Delayed (This probably means still distributing)
                "ID0" -> Status.Customs // Customs information
                else -> logUnknownStatus("Colissimo", lastEvent.code)
            }
        }

        val history = events.map {
            ParcelHistoryItem(
                it.label,
                LocalDateTime.parse(it.date, DateTimeFormatter.ISO_DATE_TIME),
                it.country
            )
        }

        return Parcel(shipment.idShip, history, status)
    }

    private val retrofit = Retrofit.Builder().baseUrl("https://www.laposte.fr/ssu/sun/back/suivi-unifie/").client(api_client) .addConverterFactory(api_factory).build()
    private val service = retrofit.create(API::class.java)

    private interface API {
        @GET("{id}")
        suspend fun getShipments(
            @Path("id") trackingId: String,
            @Query("lang") lang: String = "en_GB"
        ): List<ShipmentResponse>
    }

    @JsonClass(generateAdapter = true)
    internal data class ShipmentResponse(
        val shipment: Shipment,
    )

    @JsonClass(generateAdapter = true)
    internal data class Shipment(
        val idShip: String,
        val event: List<Event>,
    )

    @JsonClass(generateAdapter = true)
    internal data class Event(
        val code: String,
        val type: String,
        val group: String,
        val label: String,
        val date: String,
        val country: String,
        val order: Int
    )
}
