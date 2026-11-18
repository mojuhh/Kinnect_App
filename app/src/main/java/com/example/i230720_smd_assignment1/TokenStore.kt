package com.example.i230720_smd_assignment1

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class TokenStore(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        prefs = EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
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
