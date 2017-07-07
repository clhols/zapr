package dk.youtec.zapr.util

import android.content.Context
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class UIDHelper {
    private val TAG = UIDHelper::class.java.simpleName

    fun getUID(context: Context): String {
        var uid = SharedPreferences.getString(context, SharedPreferences.UID)

        //If no UID, then get it and save it.
        if (uid.isBlank()) {
            try {
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                uid = wifiManager.connectionInfo.macAddress

                if (uid == null || uid.isBlank()) {
                    val telManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    uid = telManager.deviceId
                }
            } catch (e: Exception) {
                uid = UUID.randomUUID().toString()
            }

            if (uid != null && uid.isNotBlank()) {
                SharedPreferences.setString(context, SharedPreferences.UID, uid)
            }
        }

        val encoded = Base64.encodeToString(encrypt(uid.toByteArray(), "SHA-1"), 2)
        Log.d(TAG, "UID is " + encoded)
        return encoded
    }


    fun encrypt(input: ByteArray, algorithm: String): ByteArray? {
        try {
            if (input.isNotEmpty()) {
                val instance = MessageDigest.getInstance(algorithm)
                instance.update(input)
                return bytesToHex(instance.digest())
            }
        } catch (e: NoSuchAlgorithmException) {
            val instance = MessageDigest.getInstance("MD5")
            instance.update(input)
            return bytesToHex(instance.digest())
        }

        return null
    }

    fun bytesToHex(input: ByteArray): ByteArray {
        val builder = StringBuilder()
        for (b in input) {
            builder.append(String.format("%02x", b))
        }
        return builder.toString().toByteArray()
    }
}
