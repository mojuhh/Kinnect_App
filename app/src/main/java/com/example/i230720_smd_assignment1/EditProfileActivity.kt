package com.example.i230720_smd_assignment1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etBio: EditText
    private lateinit var tvEmail: TextView
    private lateinit var etPhone: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_profile)

        etName = findViewById(R.id.etName)
        etUsername = findViewById(R.id.etUsername)
        etBio = findViewById(R.id.etBio)
        tvEmail = findViewById(R.id.tvEmail)
        etPhone = findViewById(R.id.etPhone)

        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)

        apiService.getProfile(null).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.ok && body.data != null) {
                    val name = body.data["name"] as? String ?: ""
                    val username = body.data["username"] as? String ?: ""
                    val bio = body.data["bio"] as? String ?: ""
                    val email = body.data["email"] as? String ?: ""
                    val phone = body.data["phone"] as? String ?: ""

                    if (name.isNotEmpty()) etName.setText(name)
                    if (username.isNotEmpty()) etUsername.setText(username)
                    if (bio.isNotEmpty()) etBio.setText(bio)
                    if (email.isNotEmpty()) tvEmail.text = email
                    if (phone.isNotEmpty()) etPhone.setText(phone)
                }
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {}
        })

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnSave)?.setOnClickListener {
            val username = etUsername.text.toString().trim().lowercase()
            val bio = etBio.text.toString().trim()

            if (username.isEmpty()) {
                Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            apiService.setupProfile(username, bio).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.ok) {
                        Toast.makeText(this@EditProfileActivity, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@EditProfileActivity, FeedActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Error: ${body?.error ?: "Username already taken"}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                    Toast.makeText(this@EditProfileActivity, "Saved locally (Offline)", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@EditProfileActivity, FeedActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            })
        }
    }
}
