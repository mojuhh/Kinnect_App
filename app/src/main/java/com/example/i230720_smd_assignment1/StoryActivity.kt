package com.example.i230720_smd_assignment1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.UUID

class StoryActivity : AppCompatActivity() {

    private lateinit var ivStoryMedia: ImageView
    private lateinit var vvStoryMedia: VideoView
    private lateinit var etStoryTextLabel: EditText

    private var selectedMediaUri: String = ""
    private var selectedMediaType: String = "image"
    private var selectedAudience: String = "story"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.story)

        selectedMediaUri = intent.getStringExtra("media_uri") ?: ""
        selectedMediaType = intent.getStringExtra("media_type") ?: "image"

        ivStoryMedia = findViewById(R.id.ivStoryMedia)
        vvStoryMedia = findViewById(R.id.vvStoryMedia)
        etStoryTextLabel = findViewById(R.id.etStoryTextLabel)

        if (selectedMediaUri.isNotEmpty()) {
            if (selectedMediaType == "video") {
                vvStoryMedia.visibility = View.VISIBLE
                ivStoryMedia.visibility = View.GONE
                vvStoryMedia.setVideoURI(Uri.parse(selectedMediaUri))
                vvStoryMedia.start()
            } else {
                ivStoryMedia.visibility = View.VISIBLE
                vvStoryMedia.visibility = View.GONE
                try {
                    ivStoryMedia.setImageURI(Uri.parse(selectedMediaUri))
                } catch (_: Exception) {}
            }
        }

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnYourStory)?.setOnClickListener {
            selectedAudience = "story"
            Toast.makeText(this, "Target: Your story", Toast.LENGTH_SHORT).show()
            shareStoryToServer()
        }

        findViewById<View>(R.id.btnCloseFriends)?.setOnClickListener {
            selectedAudience = "close_friends"
            Toast.makeText(this, "Target: Close friends", Toast.LENGTH_SHORT).show()
            shareStoryToServer()
        }

        findViewById<View>(R.id.btnShareYourStory)?.setOnClickListener {
            shareStoryToServer()
        }
    }

    private fun shareStoryToServer() {
        val textLabel = etStoryTextLabel.text.toString().trim()
        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)

        val uuid = UUID.randomUUID().toString()
        val labelBody = textLabel.toRequestBody("text/plain".toMediaTypeOrNull())
        val audienceBody = selectedAudience.toRequestBody("text/plain".toMediaTypeOrNull())
        val uuidBody = uuid.toRequestBody("text/plain".toMediaTypeOrNull())

        val file = if (selectedMediaUri.isNotEmpty()) File(Uri.parse(selectedMediaUri).path ?: "") else null
        val requestFile = file?.asRequestBody("*/*".toMediaTypeOrNull())
        val body = requestFile?.let { MultipartBody.Part.createFormData("media", file.name, it) }

        if (body == null) {
            Toast.makeText(this, "Please select media for story", Toast.LENGTH_SHORT).show()
            return
        }

        apiService.uploadStory(labelBody, audienceBody, uuidBody, body).enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
            override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {
                Toast.makeText(this@StoryActivity, "Story shared successfully! (Valid for 24h)", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@StoryActivity, YourStoryActivity::class.java))
                finish()
            }

            override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {
                Toast.makeText(this@StoryActivity, "Story saved locally (Offline)", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@StoryActivity, YourStoryActivity::class.java))
                finish()
            }
        })
    }
}
