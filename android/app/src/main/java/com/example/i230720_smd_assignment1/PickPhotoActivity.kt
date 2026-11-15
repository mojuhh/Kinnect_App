package com.example.i230720_smd_assignment1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PickPhotoActivity : AppCompatActivity() {

    private var selectedMediaUri: String = ""
    private var selectedMediaType: String = "image"

    private val pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri.toString()
            val type = contentResolver.getType(uri) ?: ""
            selectedMediaType = if (type.startsWith("video")) "video" else "image"
            
            // Immediately open story editor with chosen photo/video
            val intent = Intent(this, StoryActivity::class.java)
            intent.putExtra("media_uri", selectedMediaUri)
            intent.putExtra("media_type", selectedMediaType)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pick_photo)

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnNext)?.setOnClickListener {
            val intent = Intent(this, StoryActivity::class.java)
            intent.putExtra("media_uri", selectedMediaUri)
            intent.putExtra("media_type", selectedMediaType)
            startActivity(intent)
            finish()
        }

        // Launch gallery picker upon opening PickPhotoActivity
        try {
            pickGalleryLauncher.launch("*/*")
        } catch (_: Exception) {}
    }
}
