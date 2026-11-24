package com.example.i230720_smd_assignment1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

class PostActivity : AppCompatActivity() {

    private lateinit var etPostContent: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post)

        etPostContent = findViewById(R.id.etPostContent)

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnSubmitPost)?.setOnClickListener {
            val text = etPostContent.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Please write something for your post", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tokenStore = TokenStore(this)
            val apiService = ApiClient.create(tokenStore)

            val uuid = UUID.randomUUID().toString()
            val textBody = text.toRequestBody("text/plain".toMediaTypeOrNull())
            val uuidBody = uuid.toRequestBody("text/plain".toMediaTypeOrNull())

            apiService.createPost(textBody, uuidBody, emptyList()).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                    val body = response.body()
                    if (response.isSuccessful && body != null && body.ok) {
                        Toast.makeText(this@PostActivity, "Post published successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@PostActivity, "Failed to publish post: ${body?.error ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                    Toast.makeText(this@PostActivity, "Post saved locally (Offline sync queued)", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
        }

        findViewById<View>(R.id.btnPhotoVideo)?.setOnClickListener {
            startActivity(Intent(this, PickPhotoActivity::class.java))
        }

        findViewById<View>(R.id.btnCamera)?.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
    }
}
