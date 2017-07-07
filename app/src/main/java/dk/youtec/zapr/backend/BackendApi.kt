package dk.youtec.zapr.backend

import android.content.Context
import android.net.Uri
import android.os.Build
import android.support.annotation.WorkerThread
import android.util.Log
import dk.youtec.zapr.backend.exception.BackendException
import dk.youtec.zapr.model.*
import dk.youtec.zapr.util.SharedPreferences
import dk.youtec.zapr.util.SharedPreferences.SMART_CARD_ID
import dk.youtec.zapr.util.UIDHelper
import dk.youtec.zapr.util.iterator
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * API for communication with backend.
 */
class BackendApi(val context: Context, var uid: String = UIDHelper().getUID(context)) {
    private val TAG = BackendApi::class.java.simpleName

    /**
     * Login to web tv backend
     */
    @WorkerThread
    fun login(email: String = SharedPreferences.getString(context, SharedPreferences.EMAIL), password: String = SharedPreferences.getString(context, SharedPreferences.PASSWORD)): Boolean {
        val client = OkHttpClientFactory.getInstance(context)

        val body = enrichUrl(buildString {
            append("mitLoginMail=")
            append(email)
            append("&mitLoginPassword=")
            append(password)
        })

        val request = Request.Builder()
                .url(URL_LOGIN)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), body))
                .build()

        var response: Response? = null
        try {
            response = client.newCall(request).execute()

            var jsonResponse = response.body()!!.string()

            Log.d(TAG, "Login response: " + jsonResponse)

            val loginResponse = JSONObject(unwrapJson(jsonResponse))
            fetchLoginData(loginResponse)

            if (loginResponse.optBoolean("status")) {
                val loginData = loginResponse.optJSONObject("loginData")
                if (loginData != null) {
                    val loginStatus = loginData.optBoolean("loginStatus")
                    return loginStatus
                }
            } else {
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        } finally {
            closeResponse(response)
        }

        return false
    }

    /**
     * Get EPG channel data.
     */
    @WorkerThread
    fun retrieveEpgData(genreId: Int = 0): EpgResult {
        val channels = mutableListOf<Channel>()
        var httpResponse: Response? = null
        try {
            val urlAddress = enrichUrl(URL_EPG.replace("[FAVOURITE_KEY]", SharedPreferences.getString(context, SharedPreferences.FAVOURITE_LIST_KEY, "0")))
            httpResponse = getHttpResponse(context, urlAddress)

            val jsonResponse = httpResponse.body()!!.string()

            Log.d(TAG, "EPG response: " + jsonResponse)

            val epgResponse = JSONObject(unwrapJson(jsonResponse))
            fetchLoginData(epgResponse)

            if (!epgResponse.optBoolean("status")) {
                throw BackendException(epgResponse.optInt("error"), epgResponse.optString("info"))
            }

            val data = epgResponse.getJSONObject("data")
            val epgData = data.getJSONArray("epgData")

            for (channel in epgData) {
                channels.add(Channel.fromJSONObject(channel))
            }

            return EpgResult(data.optLong("updateNextInSec"), channels.filter { (genreId == 0 || it.now?.genreId == genreId) })
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
            throw BackendException(-1, e.message)
        } finally {
            closeResponse(httpResponse)
        }
    }

    /**
     * Get EPG program data for a single channel.
     */
    @WorkerThread
    fun retrievePrograms(channelSID: String,
                         startTimeToRetrieveDataFrom: Long = System.currentTimeMillis() - 48 * 60 * 60 * 1000,
                         hoursOfDataToRetrieve: Int = 256): MutableList<Program> {
        val programs = mutableListOf<Program>()
        var httpResponse: Response? = null
        try {
            val urlAddress = enrichUrl(URL_EPG_CHANNEL
                    .replace("[SID]", channelSID)
                    .replace("[HOURS_OF_DATA]", hoursOfDataToRetrieve.toString())
                    .replace("[TIME_FROM]", (startTimeToRetrieveDataFrom / 1000).toString())
            )
            httpResponse = getHttpResponse(context, urlAddress)

            val jsonResponse = httpResponse.body()!!.string()

            Log.d(TAG, "EPG response: " + jsonResponse)

            val epgResponse = JSONObject(unwrapJson(jsonResponse))
            fetchLoginData(epgResponse)

            if (!epgResponse.optBoolean("status")) {
                throw BackendException(epgResponse.optInt("error"), epgResponse.optString("info"))
            }

            val data = epgResponse.getJSONObject("data")
            val epgData = data.getJSONArray("epgData")

            for (program in epgData) {
                programs.add(Program.fromJSONObject(program))
            }

            return programs
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
            throw BackendException(-1, e.message)
        } finally {
            closeResponse(httpResponse)
        }
    }

    /**
     * Get id of TV box to control
     */
    @WorkerThread
    fun retrieveSmartCards(): String {
        var httpResponse: Response? = null
        try {
            val urlAddress = enrichUrl(URL_GET_SMART_CARDS)
            httpResponse = getHttpResponse(context, urlAddress)

            val jsonResponse = httpResponse.body()!!.string()

            Log.d(TAG, "SmartCard response: " + jsonResponse)

            val smartCardResponse = JSONObject(unwrapJson(jsonResponse))
            fetchLoginData(smartCardResponse)

            val smartCards = smartCardResponse.getJSONArray("data")
            if (smartCards.length() > 0) {
                val id = smartCards.getJSONObject(0).getString("id")
                return id
            } else {
                return ""
            }
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
            throw BackendException(-1, e.message)
        } finally {
            closeResponse(httpResponse)
        }
    }

    /**
     * Retrieve channels lists
     */
    @WorkerThread
    fun retrieveFavouriteLists(): List<FavouriteList> {
        val list = mutableListOf<FavouriteList>()

        var httpResponse: Response? = null
        try {
            val urlAddress = enrichUrl(URL_FAVORITE_LISTS)
            httpResponse = getHttpResponse(context, urlAddress)

            val jsonResponse = httpResponse.body()!!.string()

            Log.d(TAG, "FavouriteLists response: " + jsonResponse)

            val favouriteListsResponse = JSONObject(unwrapJson(jsonResponse))
            fetchLoginData(favouriteListsResponse)

            val favourites = favouriteListsResponse.getJSONArray("data")
            if (favourites.length() > 0) {
                for (i in favourites.length() - 1 downTo 0) {
                    val jsonArray = favourites.getJSONArray(i)
                    val key = jsonArray.getInt(0)
                    val name = jsonArray.getString(1)

                    list.add(FavouriteList(key, name))
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
            throw BackendException(-1, e.message)
        } finally {
            closeResponse(httpResponse)
        }

        return list
    }

    @WorkerThread
    fun changeToChannel(sid: String, eventId: Long, restart: Boolean = false): Boolean {
        var httpResponse: Response? = null
        try {

            var urlAddress = enrichUrl(URL_PUSH_SID_TO_TV)

            urlAddress = buildString {
                append(urlAddress)
                append("&smartcardId=")
                append(SharedPreferences.getString(context.applicationContext, SMART_CARD_ID))
                append("&sid=")
                append(sid)
                if (restart && eventId > 0) {
                    append("&eventId=")
                    append(eventId)
                }
            }

            httpResponse = getHttpResponse(context.applicationContext, urlAddress)

            val pushSidResponse = JSONObject(unwrapJson(httpResponse.body()!!.string()))
            fetchLoginData(pushSidResponse)

            Log.d(TAG, "Push SID response: " + pushSidResponse)

            if (!pushSidResponse.optBoolean("data")
                    || !pushSidResponse.optBoolean("status")) {
                return false
            }

            return true
        } finally {
            closeResponse(httpResponse)
        }
    }

    @WorkerThread
    fun getStreamUrl(sid: String, eventId: Long, restart: Boolean = false): String {
        var httpResponse: Response? = null
        try {
            var urlAddress = enrichUrl(URL_GET_STREAM)

            urlAddress = buildString {
                append(urlAddress)
                append("&sid=")
                append(sid)
                if (eventId > 0) {
                    append("&eventId=")
                    append(eventId)
                }
            }

            httpResponse = getHttpResponse(context.applicationContext, urlAddress)

            val streamResponse = JSONObject(unwrapJson(httpResponse.body()!!.string()))
            fetchLoginData(streamResponse)

            Log.d(TAG, "Get stream response: " + streamResponse)

            if (streamResponse.optBoolean("status")) {
                val streams = streamResponse.optJSONObject("data").optJSONObject("streams")

                var uri = ""
                if (!restart)
                    uri = streams.optString("stream")
                else {
                    uri = streams.optString("startover")

                    if (uri.isNotBlank()) {
                        if (streams.optJSONObject("drm").optString("catchup") == "1") {
                            storeDrmCatchup(sid, streams.optLong("eventId")) // Not sure when this call is needed.
                        }
                    } else {
                        uri = streams.optString("stream")
                    }
                }

                Log.d(TAG, "Stream URL is " + uri)

                return uri
            } else {
                throw BackendException(streamResponse.getInt("error"), streamResponse.getString("info"))
            }
        } finally {
            closeResponse(httpResponse)
        }
    }

    @WorkerThread
    fun forceProvisioning() {
        var httpResponse: Response? = null
        try {
            val urlAddress = buildString {
                append(URL_FORCE_RETRY)
                append("&drmKey=")
                append(uid)
            }
            httpResponse = getHttpResponse(context, urlAddress)

            val jsonResponse = httpResponse.body()!!.string()

            Log.d(TAG, "Force provisioning response: " + jsonResponse)
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
            throw BackendException(-1, e.message)
        } finally {
            closeResponse(httpResponse)
        }
    }

    @WorkerThread
    fun keepAlive(sid: String) {
        var httpResponse: Response? = null
        try {
            val urlAddress = enrichUrl(URL_KEEP_ALIVE
                    .replace("[SID]", sid)
                    .replace("[CUSTOMER_ID]", LoginData.customerId)
                    .replace("[INSTALL_ID]", LoginData.installId)
            )
            httpResponse = getHttpResponse(context, urlAddress)

            val jsonResponse = httpResponse.body()!!.string()

            Log.d(TAG, "Keep alive response: " + jsonResponse)
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
            throw BackendException(-1, e.message)
        } finally {
            closeResponse(httpResponse)
        }
    }

    @WorkerThread
    fun storeDrmCatchup(sid: String, eventId: Long) {
        var httpResponse: Response? = null
        try {
            val urlAddress = URL_STORE_DRM_CATCHUP
                    .replace("[SID]", sid)
                    .replace("[EVENT_ID]", eventId.toString())

            httpResponse = getHttpResponse(context, urlAddress)

            val jsonResponse = httpResponse.body()!!.string()

            Log.d(TAG, "Store DRM catchup response: " + jsonResponse)
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
            throw BackendException(-1, e.message)
        } finally {
            closeResponse(httpResponse)
        }
    }

    /**
     * Get genres.
     */
    @WorkerThread
    fun retrieveEpgGenres(): List<Genre> {
        val genres: ArrayList<Genre> = ArrayList()
        var httpResponse: Response? = null
        try {
            val urlAddress = enrichUrl(URL_GET_GENRES)
            httpResponse = getHttpResponse(context, urlAddress)

            val jsonResponse = httpResponse.body()!!.string()

            Log.d(TAG, "Genres response: " + jsonResponse)

            val epgResponse = JSONObject(unwrapJson(jsonResponse))
            fetchLoginData(epgResponse)

            if (!epgResponse.optBoolean("status")) {
                throw BackendException(epgResponse.optInt("error"), epgResponse.optString("info"))
            }

            val data = epgResponse.optJSONObject("data")

            if (data != null) {
                for (id in data.keys()) {
                    val genreJson = data.optJSONObject(id)
                    val genre = Genre.fromJSONObject(id.toInt(), genreJson)
                    genres.add(genre)
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, e.message, e)
            throw BackendException(-1, e.message)
        } finally {
            closeResponse(httpResponse)
        }

        return genres
    }

    fun enrichUrl(url: String): String {
        return buildString {
            append(url)
            append("&manufacturer=")
            append(Uri.encode(Build.MANUFACTURER))
            append("&model=")
            append(Build.MODEL.replace(" ", "+"))
            append("&uid=")
            append(uid)
            append("&token=")
            append(LoginData.token)
            append("&ver=")
            append(VERSION)
            append("&drmKey=")
            append(uid)
            append("&datatype=json")
        }
    }
}