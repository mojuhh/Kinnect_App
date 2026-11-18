package com.example.i230720_smd_assignment1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val recentCard = findViewById<View>(R.id.recentLoginCard)
        val tvRecentName = findViewById<TextView>(R.id.tvRecentName)
        val tvRecentAvatar = findViewById<TextView>(R.id.tvRecentAvatar)

        val prefs = getSharedPreferences("KinnectPrefs", MODE_PRIVATE)
        val lastEmail = prefs.getString("last_email", "") ?: ""

        if (lastEmail.isNotEmpty()) {
            recentCard?.visibility = View.VISIBLE
            tvRecentName?.text = lastEmail
            val avatarText = if (lastEmail.length >= 2) lastEmail.substring(0, 2).uppercase() else "U"
            tvRecentAvatar?.text = avatarText
        } else {
            recentCard?.visibility = View.GONE
        }

        recentCard?.setOnClickListener {
            if (lastEmail.isNotEmpty()) {
                etEmail?.setText(lastEmail)
                etPassword?.requestFocus()
                Toast.makeText(this, "Prefilled $lastEmail", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.btnLogin)?.setOnClickListener {
            val identifier = etEmail?.text?.toString()?.trim() ?: ""
            val pass = etPassword?.text?.toString()?.trim() ?: ""

            if (identifier.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please enter email or phone number and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tokenStore = TokenStore(this)
            val apiService = ApiClient.create(tokenStore)

            apiService.login(identifier, pass).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                override fun onResponse(
                    call: Call<ApiResponse<Map<String, Any>>>,
                    response: Response<ApiResponse<Map<String, Any>>>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.ok && body.data != null) {
                        val token = body.data["token"] as? String ?: ""
                        val userId = (body.data["user_id"] as? Number)?.toInt() ?: 1
                        val profileComplete = (body.data["profile_complete"] as? Number)?.toInt() ?: 0

                        tokenStore.save(token, userId)

                        prefs.edit()
                            .putBoolean("is_logged_in", true)
                            .putString("last_email", identifier)
                            .apply()

                        val intent = if (profileComplete == 0) {
                            Intent(this@LoginActivity, EditProfileActivity::class.java)
                        } else {
                            Intent(this@LoginActivity, FeedActivity::class.java)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Login failed: ${apiErrorMessage(response, "Request failed (${response.code()})")}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Could not connect to server: ${t.localizedMessage ?: "network error"}", Toast.LENGTH_LONG).show()
                }
            })
        }

        findViewById<View>(R.id.btnCreateAccount)?.setOnClickListener {
            startActivity(Intent(this, CreateAccountActivity::class.java))
        }
    }
}
