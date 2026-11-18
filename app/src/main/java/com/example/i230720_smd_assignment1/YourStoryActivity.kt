package com.example.i230720_smd_assignment1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class YourStoryActivity : AppCompatActivity() {

    private lateinit var ivStoryMedia: ImageView
    private lateinit var vvStoryMedia: VideoView
    private lateinit var tvAvatar: TextView
    private lateinit var tvStoryTextLabel: TextView
    private lateinit var btnBack: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.your_story)

        ivStoryMedia = findViewById(R.id.ivStoryMedia)
        vvStoryMedia = findViewById(R.id.vvStoryMedia)
        tvAvatar = findViewById(R.id.tvAvatar)
        tvStoryTextLabel = findViewById(R.id.tvStoryTextLabel)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btnCreateNewStory)?.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
            finish()
        }

        btnBack.text = "Your story · 2h  ✕"
        tvAvatar.text = "JW"
        tvStoryTextLabel.text = "Saturday plans"
    }
}
