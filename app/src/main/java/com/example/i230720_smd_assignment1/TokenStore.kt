package com.example.i230720_smd_assignment1

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(token: String, userId: Int) {
        prefs.edit()
            .putString("token", token)
            .putInt("uid", userId)
            .apply()
    }

    fun token(): String? = prefs.getString("token", null)

    fun userId(): Int = prefs.getInt("uid", -1)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
