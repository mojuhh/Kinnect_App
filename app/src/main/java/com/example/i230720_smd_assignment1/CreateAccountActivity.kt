package com.example.i230720_smd_assignment1

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateAccountActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_account)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvPasswordStrength = findViewById<TextView>(R.id.tvPasswordStrength)

        etPassword?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s?.toString() ?: ""
                when {
                    pass.isEmpty() || pass.length < 8 -> {
                        tvPasswordStrength?.text = "Password Strength: Weak (Min 8 chars)"
                        tvPasswordStrength?.setTextColor(Color.parseColor("#E53935"))
                    }
                    pass.length < 10 || !pass.any { it.isDigit() } -> {
                        tvPasswordStrength?.text = "Password Strength: Medium"
                        tvPasswordStrength?.setTextColor(Color.parseColor("#FB8C00"))
                    }
                    else -> {
                        tvPasswordStrength?.text = "Password Strength: Strong"
                        tvPasswordStrength?.setTextColor(Color.parseColor("#43A047"))
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnCreateAccountSubmit)?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val phone = etPhone?.text?.toString()?.trim() ?: ""
            val pass = etPassword?.text?.toString()?.trim() ?: ""

            if (email.isEmpty() || phone.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill in email, phone number and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tokenStore = TokenStore(this)
            val apiService = ApiClient.create(tokenStore)

            apiService.signup("Jacob West", email, phone, pass).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                override fun onResponse(
                    call: Call<ApiResponse<Map<String, Any>>>,
                    response: Response<ApiResponse<Map<String, Any>>>
                ) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.ok && body.data != null) {
                        val token = body.data["token"] as? String ?: ""
                        val userId = (body.data["user_id"] as? Number)?.toInt() ?: 1

                        tokenStore.save(token, userId)

                        val prefs = getSharedPreferences("KinnectPrefs", MODE_PRIVATE)
                        prefs.edit()
                            .putBoolean("is_logged_in", true)
                            .putString("last_email", email)
                            .putBoolean("is_first_login", true)
                            .apply()

                        val intent = Intent(this@CreateAccountActivity, EditProfileActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@CreateAccountActivity, "Sign up failed: ${apiErrorMessage(response, "Request failed (${response.code()})")}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                    Toast.makeText(this@CreateAccountActivity, "Could not connect to server: ${t.localizedMessage ?: "network error"}", Toast.LENGTH_LONG).show()
                }
            })
        }

        findViewById<View>(R.id.tvLogin)?.setOnClickListener {
            finish()
        }
    }
}
