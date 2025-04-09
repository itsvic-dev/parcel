package dev.itsvic.parceltracker.api

import dev.itsvic.parceltracker.R
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.coroutines.executeAsync
import android.util.Log
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import org.json.JSONObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.Retrofit

object USPSDeliveryService : DeliveryService {
    override val nameResource: Int = R.string.service_usps
    override val acceptsPostCode: Boolean = false
    override val requiresPostCode: Boolean = false

    override suspend fun getParcel(trackingId: String, postCode: String?): Parcel {

        return Parcel(
            trackingId,
            "history",
            "status",
            "metadata"
        )
    }

    private val retrofit = Retrofit.Builder().baseUrl("https://api.usps.com/").client(api_client)
        .addConverterFactory(api_factory).build()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getOauthToken(clientId: String, clientSecret: String): String {
        // this is all coming from https://developers.usps.com/Oauth
        val GRANT_TYPE = "authorization_code"
        val TOKEN_URL = "https://apis.usps.com/oauth2/v3/token"

        // this should work trust me
        val body = """{
            |"grant_type": "$GRANT_TYPE",
            |"client_id": "$clientId",
            |"client_secret": "$clientSecret"}""".trimMargin()
                .toRequestBody("application/json".toMediaType())

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
    }
}
