package dk.youtec.zapr.util

import android.content.Context
import android.preference.PreferenceManager


object SharedPreferences {
    const val SMART_CARD_ID = "selectedSmartCardId"
    const val FAVOURITE_LIST_KEY = "selectedFavouriteList"
    const val REMOTE_CONTROL_MODE = "remoteControlMode"
    const val EMAIL = "email"
    const val PASSWORD = "password"
    const val UID = "uid"

    fun getString(context: Context, key: String, default: String = ""): String {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, default)
    }

    fun setString(context: Context, key: String, value: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, value)
                .apply()
    }

    fun setInt(context: Context, key: String, value: Int) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putInt(key, value)
                .apply()
    }

    fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, default)
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(key, value)
                .apply()
    }
}