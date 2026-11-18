package com.example.i230720_smd_assignment1

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionManager {
    private const val PREFS = "kinnect_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSession(token: String, userId: Int) {
        prefs.edit().putString(KEY_TOKEN, token).putInt(KEY_USER_ID, userId).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun isLoggedIn(): Boolean = getToken() != null

    fun clear() = prefs.edit().clear().apply()
}
