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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class StoryActivity : AppCompatActivity() {

    private lateinit var ivStoryMedia: ImageView
    private lateinit var vvStoryMedia: VideoView
    private lateinit var etStoryTextLabel: EditText

    private var selectedMediaUri: String = ""
    private var selectedMediaType: String = "image"
    private var selectedAudience: String = "your_story"

    private lateinit var auth: FirebaseAuth
    private var currentUid = ""
    private var currentUsername = "User"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.story)

        auth = FirebaseAuth.getInstance()
        currentUid = auth.currentUser?.uid ?: ""

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

        fetchUserInfo()

        val btnYourStory = findViewById<View>(R.id.btnYourStory)
        val btnCloseFriends = findViewById<View>(R.id.btnCloseFriends)
        val btnShareYourStory = findViewById<View>(R.id.btnShareYourStory)

        btnYourStory?.setOnClickListener {
            selectedAudience = "your_story"
            Toast.makeText(this, "Target: Your story", Toast.LENGTH_SHORT).show()
            shareStoryToFirebase()
        }

        btnCloseFriends?.setOnClickListener {
            selectedAudience = "close_friends"
            Toast.makeText(this, "Target: Close friends", Toast.LENGTH_SHORT).show()
            shareStoryToFirebase()
        }

        btnShareYourStory?.setOnClickListener {
            shareStoryToFirebase()
        }
    }

    private fun fetchUserInfo() {
        if (currentUid.isEmpty()) return
        val dbRef = FirebaseDatabase.getInstance().reference.child("users").child(currentUid)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val username = snapshot.child("username").getValue(String::class.java)
                    currentUsername = when {
                        !name.isNullOrEmpty() -> name
                        !username.isNullOrEmpty() -> username
                        else -> "User"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun shareStoryToFirebase() {
        val textLabel = etStoryTextLabel.text.toString().trim()
        val dbRef = FirebaseDatabase.getInstance().reference

        val storyId = dbRef.child("stories").push().key ?: UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val expiresAt = now + (24 * 60 * 60 * 1000) // 24 Hours duration

        val initials = if (currentUsername.length >= 2) currentUsername.substring(0, 2).uppercase() else "JW"

        val story = Story(
            storyId = storyId,
            userId = currentUid,
            username = currentUsername,
            userAvatar = initials,
            mediaUri = selectedMediaUri,
            mediaType = selectedMediaType,
            textLabel = textLabel,
            audience = selectedAudience,
            timestamp = now,
            expiresAt = expiresAt
        )

        dbRef.child("stories").child(storyId).setValue(story)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val prefs = getSharedPreferences("KinnectPrefs", MODE_PRIVATE)
                    prefs.edit()
                        .putLong("story_timestamp", now)
                        .putBoolean("has_active_story", true)
                        .putString("latest_story_id", storyId)
                        .apply()

                    Toast.makeText(this, "Story shared successfully! (Valid for 24h)", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, YourStoryActivity::class.java)
                    intent.putExtra("story_id", storyId)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to share story: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
