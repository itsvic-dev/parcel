package dev.itsvic.parceltracker.api

import com.squareup.moshi.JsonClass
import dev.itsvic.parceltracker.R
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.time.Instant
import java.time.LocalDateTime
import java.util.TimeZone

object CainiaoDeliveryService : DeliveryService {
    override val nameResource: Int = R.string.service_cainiao

    override suspend fun getParcel(trackingId: String, postalCode: String?): Parcel {
        val resp = service.getParcelDetails(trackingId)
        val parcel = resp.module.first()

        if (parcel.detailList.isEmpty())
            throw ParcelNonExistentException()

        val history = parcel.detailList.map {
            val (location, desc) = locationRegex.find(it.standerdDesc)!!.destructured

            ParcelHistoryItem(
                desc,
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(it.time),
                    TimeZone.getDefault().toZoneId()
                ),
                if (location != "") location
                else ""
            )
        }

        val status = when (val statusCode = parcel.detailList.first().actionCode) {
            "PU_PICKUP_SUCCESS" -> Status.Preadvice
            "SC_INBOUND_SUCCESS" -> Status.InWarehouse
            "CW_INBOUND" -> Status.InWarehouse
            "SC_OUTBOUND_SUCCESS" -> Status.InTransit
            "LH_HO_IN_SUCCESS" -> Status.InTransit
            "???" -> Status.Delivered
            else -> logUnknownStatus("Cainiao", statusCode)
        }

        return Parcel(trackingId, history, status)
    }

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://global.cainiao.com/global/")
        .client(api_client)
        .addConverterFactory(api_factory)
        .build()

    private val service = retrofit.create(API::class.java)

    private interface API {
        @GET("detail.json")
        suspend fun getParcelDetails(
            @Query("mailNos") trackingId: String
        ): ParcelResponse
    }

    private val locationRegex = Regex("(?:^\\[(.*)\\]\\s+)?(.*)\$")

    @JsonClass(generateAdapter = true)
    internal data class ParcelResponse(
        val module: List<CainiaoParcel>
    )

    @JsonClass(generateAdapter = true)
    internal data class CainiaoParcel(
        val status: String,
        val detailList: List<ParcelEvent>
    )

    @JsonClass(generateAdapter = true)
    internal data class ParcelEvent(
        val time: Long,
        val standerdDesc: String, // I can't get over standErdDesc, send help
        val actionCode: String
    )
}
