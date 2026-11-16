package com.example.i230720_smd_assignment1

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ViewStoryActivity : AppCompatActivity() {

    private lateinit var ivStoryMedia: ImageView
    private lateinit var vvStoryMedia: VideoView
    private lateinit var tvAvatar: TextView
    private lateinit var tvUsernameTime: TextView
    private lateinit var tvStoryTextLabel: TextView
    private lateinit var etReplyMessage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_story)

        ivStoryMedia = findViewById(R.id.ivStoryMedia)
        vvStoryMedia = findViewById(R.id.vvStoryMedia)
        tvAvatar = findViewById(R.id.tvAvatar)
        tvUsernameTime = findViewById(R.id.tvUsernameTime)
        tvStoryTextLabel = findViewById(R.id.tvStoryTextLabel)
        etReplyMessage = findViewById(R.id.etReplyMessage)

        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            finish()
        }

        val storyId = intent.getStringExtra("story_id") ?: ""
        val username = intent.getStringExtra("username") ?: "Omar Farooq"
        val textLabel = intent.getStringExtra("text_label") ?: "Karachi nights"
        val mediaUri = intent.getStringExtra("media_uri") ?: ""
        val mediaType = intent.getStringExtra("media_type") ?: "image"
        val timeAgo = intent.getStringExtra("time_ago") ?: "5h"

        if (storyId.isNotEmpty()) {
            loadStoryFromFirebase(storyId)
        } else {
            bindStoryData(username, timeAgo, textLabel, mediaUri, mediaType)
        }

        findViewById<View>(R.id.btnReactionLike)?.setOnClickListener {
            Toast.makeText(this, "Liked story!", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnReactionHeart)?.setOnClickListener {
            Toast.makeText(this, "Loved story! ❤️", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnReactionHaha)?.setOnClickListener {
            Toast.makeText(this, "Reacted to story! 😂", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadStoryFromFirebase(storyId: String) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("stories").child(storyId)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val story = snapshot.getValue(Story::class.java)
                    if (story != null) {
                        val hoursAgo = ((System.currentTimeMillis() - story.timestamp) / (1000 * 60 * 60)).coerceAtLeast(1)
                        bindStoryData(story.username, "${hoursAgo}h", story.textLabel, story.mediaUri, story.mediaType)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun bindStoryData(username: String, timeAgo: String, textLabel: String, mediaUri: String, mediaType: String) {
        tvUsernameTime.text = "$username · $timeAgo"
        val initials = if (username.length >= 2) username.substring(0, 2).uppercase() else "OF"
        tvAvatar.text = initials

        if (textLabel.isNotEmpty()) {
            tvStoryTextLabel.visibility = View.VISIBLE
            tvStoryTextLabel.text = textLabel
        } else {
            tvStoryTextLabel.visibility = View.GONE
        }

        if (mediaUri.isNotEmpty()) {
            if (mediaType == "video") {
                vvStoryMedia.visibility = View.VISIBLE
                ivStoryMedia.visibility = View.GONE
                try {
                    vvStoryMedia.setVideoURI(Uri.parse(mediaUri))
                    vvStoryMedia.start()
                } catch (_: Exception) {}
            } else {
                ivStoryMedia.visibility = View.VISIBLE
                vvStoryMedia.visibility = View.GONE
                try {
                    ivStoryMedia.setImageURI(Uri.parse(mediaUri))
                } catch (_: Exception) {}
            }
        }
    }
}
