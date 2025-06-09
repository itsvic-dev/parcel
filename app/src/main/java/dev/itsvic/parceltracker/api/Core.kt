// SPDX-License-Identifier: GPL-3.0-or-later
package dev.itsvic.parceltracker.api

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import com.squareup.moshi.Moshi
import dev.itsvic.parceltracker.BuildConfig
import dev.itsvic.parceltracker.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.TimeZone

enum class Service {
    UNDEFINED,
    EXAMPLE,

    // International
    DHL,
    GLS,
    UPS,

    // North America
    UNIUNI,

    // United Kingdom
    DPD_UK,
    EVRI,

    // Europe
    AN_POST,
    BELPOST,
    GLS_HUNGARY,
    MAGYAR_POSTA,
    NOVA_POSHTA,
    PACKETA,
    POLISH_POST,
    POSTE_ITALIANE,
    SAMEDAY_BG,
    SAMEDAY_HU,
    SAMEDAY_RO,
    UKRPOSHTA,
    POSTNORD,
    COLISSIMO,

    // Asia
    EKART,
    SPX_TH,
}

val serviceOptions =
    Service.entries.filter { return@filter it != Service.UNDEFINED && it != Service.EXAMPLE }
        .toList()

fun getDeliveryService(service: Service): DeliveryService? {
    return when (service) {
        Service.DHL -> DhlDeliveryService
        Service.GLS -> GLSGlobalDeliveryService
        Service.UPS -> UPSDeliveryService

        Service.UNIUNI -> UniUniDeliveryService

        Service.DPD_UK -> DpdUkDeliveryService
        Service.EVRI -> EvriDeliveryService

        Service.AN_POST -> AnPostDeliveryService
        Service.BELPOST -> BelpostDeliveryService
        Service.GLS_HUNGARY -> GLSHungaryDeliveryService
        Service.MAGYAR_POSTA -> MagyarPostaDeliveryService
        Service.NOVA_POSHTA -> NovaPostDeliveryService
        Service.PACKETA -> PacketaDeliveryService
        Service.POLISH_POST -> PolishPostDeliveryService
        Service.POSTE_ITALIANE -> PosteItalianeDeliveryService
        Service.SAMEDAY_BG -> SamedayBulgariaDeliveryService
        Service.SAMEDAY_HU -> SamedayHungaryDeliveryService
        Service.SAMEDAY_RO -> SamedayRomaniaDeliveryService
        Service.UKRPOSHTA -> UkrposhtaDeliveryService
        Service.POSTNORD -> PostNordDeliveryService
        Service.COLISSIMO -> ColissimoDeliveryService

        Service.EKART -> EKartDeliveryService
        Service.SPX_TH -> SPXThailandDeliveryService

        Service.EXAMPLE -> ExampleDeliveryService
        else -> null
    }
}

internal val api_client = OkHttpClient.Builder()
    .addInterceptor(
        HttpLoggingInterceptor {
            Log.d("OkHttp", it)
        }.setLevel(
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.BASIC
        )
    )
    .build()

internal val api_moshi: Moshi = Moshi.Builder().build()
internal val api_factory = MoshiConverterFactory.create(api_moshi)

// TODO: fill out with more data
data class Parcel(
    val id: String,
    val history: List<ParcelHistoryItem>,
    val currentStatus: Status,
    val properties: Map<Int, String> = mapOf(),
)

data class ParcelHistoryItem(
    val description: String,
    val time: LocalDateTime,
    val location: String,
)

enum class Status(val nameResource: Int) {
    Preadvice(R.string.status_preadvice),
    InTransit(R.string.status_in_transit),
    InWarehouse(R.string.status_in_warehouse),
    Customs(R.string.status_customs),
    OutForDelivery(R.string.status_out_for_delivery),
    DeliveryFailure(R.string.status_delivery_failure),
    Delivered(R.string.status_delivered),
    AwaitingPickup(R.string.status_awaiting_pickup),
    PickedUp(R.string.status_picked_up),
    Unknown(R.string.status_unknown),
    NetworkFailure(R.string.status_network_failure),
    NoData(R.string.status_no_data),
}

suspend fun Context.getParcel(id: String, postCode: String?, service: Service): Parcel {
    // use DeliveryService abstraction if possible, otherwise default to the old hardcoded list
    getDeliveryService(service)?.let {
        return it.getParcel(this, id, postCode)
    }

    throw NotImplementedError("Service $service has no DeliveryService object")
}

fun getDeliveryServiceName(service: Service): Int? {
    return getDeliveryService(service)?.nameResource
}

interface DeliveryService {
    val nameResource: Int
    val acceptsPostCode: Boolean
    val requiresPostCode: Boolean
    val requiresApiKey: Boolean
        get() = false
    val apiKeyPreference: Preferences.Key<String>?
        get() = null

    suspend fun getParcel(trackingId: String, postCode: String?): Parcel {
        return TODO("DeliveryService does not implement getParcel")
    }
    suspend fun getParcel(context: Context, trackingId: String, postalCode: String?): Parcel {
        return getParcel(trackingId, postalCode)
    }

    fun acceptsFormat(trackingId: String): Boolean {
        return false
    }
}

class ParcelNonExistentException : Exception("Parcel does not exist in delivery service API")
class APIKeyMissingException : Exception("Delivery service requires an API key but none is present")

internal fun logUnknownStatus(service: String, data: String): Status {
    Log.d("APICore", "Unknown status reported by $service: $data")
    return Status.Unknown
}

fun localDateFromMilli(milli: Long): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(milli), TimeZone.getDefault().toZoneId())
}
