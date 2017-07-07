package dk.youtec.zapr.backend

import android.content.Context
import android.util.Log
import dk.youtec.zapr.backend.exception.HttpException
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

val TAG = "http"

//Url parameter constants
const val VERSION = "3.2.4"

//Urls
const val URL_LOGIN = "http://"
const val URL_EPG = "http://"
const val URL_EPG_CHANNEL = "http://"
const val URL_EPG_PROGRAM = "http://"
const val URL_EPG_FOR_GENRES = "http://"
const val URL_PUSH_SID_TO_TV = "http://"
const val URL_GET_SMART_CARDS = "http://"
const val URL_GET_GFX = "http://"
const val URL_CUSTOMER_DATA = "http://"
const val URL_FAVORITE_LISTS = "http://"
const val URL_CHANNEL_ICONS = "http://"
const val URL_GET_GENRES = "http://"
const val URL_GET_STREAM = "http://"
const val URL_KEEP_ALIVE = "http://"
const val URL_LOG_ERROR = "http://"
const val URL_FORCE_RETRY = "http://"
const val URL_STORE_DRM_CATCHUP = "http://"

const val ERROR_MUST_BE_LOGGED_IN = 1110045

/**
 * Get a response from a http request to the URL.

 * @param urlAddress The URL to open.
 * *
 * @return [Response] response of the http request. Remember to close it when done.
 */
@Throws(IOException::class)
fun getHttpResponse(context: Context, urlAddress: String): Response {
    try {
        val request = Request.Builder()
                .url(urlAddress)
                .build()

        val client = OkHttpClientFactory.getInstance(context)

        var response: Response? = null
        try {
            response = client.newCall(request).execute()
            if (!response.isSuccessful)
                throw HttpException(response.code(), "Unexpected code " + response)

            return response
        } catch (e: HttpException) {
            if (e.code !== 404) {
                Log.w(TAG, e.message, e)
            } else {
                Log.w(TAG, e.message)
            }
            closeResponse(response)
            throw e
        }

    } catch (e: IllegalArgumentException) {
        throw IOException(e.message, e)
    }
}

fun closeResponse(response: Response?) {
    response?.body()?.close()
}

fun unwrapJson(json: String): String {
    val temp = json.trim()
    if (temp.startsWith("(") && temp.endsWith(")")) {
        return temp.substring(1, temp.length - 1)
    }
    return temp
}

fun fetchLoginData(response: JSONObject) {
    if (response.optBoolean("status")) {
        val loginData = response.optJSONObject("loginData")
        val loginStatus = loginData?.optBoolean("loginStatus") ?: false
        val token = loginData.optString("token")
        if (loginStatus && token.isNotEmpty()) {
            LoginData.token = token
            LoginData.customerId = loginData.optString("customerId")
            LoginData.installId = loginData.optString("installId")
        } else {
            Log.e(TAG, "Setting empty token, loginStatus=$loginStatus, token=$token")
            LoginData.token = ""
        }
    } else {
        Log.e(TAG, "Setting empty token, response status is false")
        LoginData.token = ""
    }
}