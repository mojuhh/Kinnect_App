package com.example.i230720_smd_assignment1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class CameraActivity : AppCompatActivity() {

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = Utils.saveBitmapToCache(this, bitmap)
            openStoryEditor(uri?.toString() ?: "", "image")
        } else {
            openStoryEditor("", "image")
        }
    }

    private val pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val type = contentResolver.getType(uri) ?: ""
            val mediaType = if (type.startsWith("video")) "video" else "image"
            openStoryEditor(uri.toString(), mediaType)
        } else {
            openStoryEditor("", "image")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera)

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnShutter)?.setOnClickListener {
            try {
                takePictureLauncher.launch(null)
            } catch (e: Exception) {
                openStoryEditor("", "image")
            }
        }

        findViewById<View>(R.id.btnGalleryPicker)?.setOnClickListener {
            try {
                pickGalleryLauncher.launch("*/*")
            } catch (e: Exception) {
                startActivity(Intent(this, PickPhotoActivity::class.java))
            }
        }
    }

    private fun openStoryEditor(mediaUri: String, mediaType: String) {
        val intent = Intent(this, StoryActivity::class.java)
        intent.putExtra("media_uri", mediaUri)
        intent.putExtra("media_type", mediaType)
        startActivity(intent)
        finish()
    }
}
