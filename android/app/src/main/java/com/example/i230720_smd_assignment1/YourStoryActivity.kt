package com.example.i230720_smd_assignment1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

        val storyId = intent.getStringExtra("story_id") ?: ""
        val auth = FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid ?: ""

        if (storyId.isNotEmpty()) {
            loadStoryFromFirebase(storyId)
        } else if (currentUid.isNotEmpty()) {
            loadUserLatestStory(currentUid)
        }
    }

    private fun loadStoryFromFirebase(storyId: String) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("stories").child(storyId)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val story = snapshot.getValue(Story::class.java)
                    if (story != null) {
                        bindStory(story)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadUserLatestStory(uid: String) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("stories")
        dbRef.orderByChild("userId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val now = System.currentTimeMillis()
                    var latestStory: Story? = null

                    for (sSnap in snapshot.children) {
                        val story = sSnap.getValue(Story::class.java) ?: continue
                        if (story.expiresAt > now) {
                            if (latestStory == null || story.timestamp > latestStory.timestamp) {
                                latestStory = story
                            }
                        }
                    }

                    if (latestStory != null) {
                        bindStory(latestStory)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun bindStory(story: Story) {
        val hoursAgo = ((System.currentTimeMillis() - story.timestamp) / (1000 * 60 * 60)).coerceAtLeast(1)
        btnBack.text = "Your story · ${hoursAgo}h  ✕"

        val initials = if (story.username.length >= 2) story.username.substring(0, 2).uppercase() else "JW"
        tvAvatar.text = initials

        if (story.textLabel.isNotEmpty()) {
            tvStoryTextLabel.visibility = View.VISIBLE
            tvStoryTextLabel.text = story.textLabel
        } else {
            tvStoryTextLabel.visibility = View.GONE
        }

        if (story.mediaUri.isNotEmpty()) {
            if (story.mediaType == "video") {
                vvStoryMedia.visibility = View.VISIBLE
                ivStoryMedia.visibility = View.GONE
                try {
                    vvStoryMedia.setVideoURI(Uri.parse(story.mediaUri))
                    vvStoryMedia.start()
                } catch (_: Exception) {}
            } else {
                ivStoryMedia.visibility = View.VISIBLE
                vvStoryMedia.visibility = View.GONE
                try {
                    ivStoryMedia.setImageURI(Uri.parse(story.mediaUri))
                } catch (_: Exception) {}
            }
        }
    }
}
