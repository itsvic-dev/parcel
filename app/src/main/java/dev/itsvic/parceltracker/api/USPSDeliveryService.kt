package dev.itsvic.parceltracker.api

import dev.itsvic.parceltracker.R
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import org.json.JSONObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

object USPSDeliveryService : DeliveryService {
    override val nameResource: Int = R.string.service_usps
    override val acceptsPostCode: Boolean = false
    override val requiresPostCode: Boolean = false

    override suspend fun getParcel(trackingId: String, postCode: String?): Parcel {
        val oauthToken =
        return Parcel(
            trackingId,
            "history",
            "status",
            "metadata"
        )
    }

    private val retrofit = Retrofit.Builder().baseUrl("https://api.usps.com/").client(api_client)
        .addConverterFactory(api_factory).build()

    private val service = retrofit.create(API::class.java)

    private interface API {
        @POST("oauth2/v3/token")
        suspend fun getOauthToken(
            @Body data: GetOauthToken
        ): OauthTokenResponse

        @GET("tracking/v3/tracking")
        suspend fun getStatus(
            @Path("trackingId") trackingId: String,
            @Header("Authorization") authorization: String
        ): StatusResponse
    }

    @JsonClass(generateAdapter = true)
    internal data class GetOauthToken(
        // from https://developers.usps.com/Oauth
        val clientId: String,
        val clientSecret: String,
        val grantType: String = "authorization_code",
        val scopes: List<String> = listOf("tracking"),
        val tokenUrl: String = "https://apis.usps.com/oauth2/v3/token",
    )

    @JsonClass(generateAdapter = true)
    internal data class OauthTokenResponse(
        val oauthToken: String
    )

    @JsonClass(generateAdapter = true)
    internal data class StatusResponse(
        val expectedDeliveryDate: String,
        val expectedDeliveryTime: String,
        val guaranteedDeliveryDate: String,
        val eventSummaries: List<EventSummary>
    )

    @JsonClass(generateAdapter = true)
    internal data class EventSummary(
        val eventDescription: String
    )

/*
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getOauthToken(clientId: String, clientSecret: String): String {

        // this should work trust me
        val tokenResp = service.getOauthToken()

        val request = Request.Builder().post(body).url(TOKEN_URL).build()

        api_client.newCall(request).executeAsync().use { response ->
            Log.d("USPS", "Got Response")
            var jsonWebToken = ""
            if (response.code == 200) {
                jsonWebToken = JSONObject(response.body.string()).optString("access_token", "")
                Log.d("USPS", "Got Oauth2 token! $jsonWebToken")
                // TODO: verify access token with provided public key
                // if the USPS gets hacked we may have bigger problems on our hands than inaccurate package tracking
            } else Log.d("USPS", "Couldn't get Oauth2 token ): response code was ${response.code}")
            return jsonWebToken
        }
    }*/
}
