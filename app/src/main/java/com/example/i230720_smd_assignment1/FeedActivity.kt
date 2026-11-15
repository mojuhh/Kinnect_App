package com.example.i230720_smd_assignment1

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FeedActivity : AppCompatActivity() {

    private lateinit var layoutStoriesContainer: LinearLayout
    private lateinit var btnCreateStory: View
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.feed)

        TabNavigation.setupTabs(this)

        auth = FirebaseAuth.getInstance()

        layoutStoriesContainer = findViewById(R.id.layoutStoriesContainer)
        btnCreateStory = findViewById(R.id.btnCreateStory)

        findViewById<View>(R.id.btnSearch)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        findViewById<View>(R.id.btnChats)?.setOnClickListener {
            startActivity(Intent(this, ChatsActivity::class.java))
        }

        findViewById<View>(R.id.btnCreatePost)?.setOnClickListener {
            startActivity(Intent(this, PostActivity::class.java))
        }

        btnCreateStory.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        findViewById<View>(R.id.btnViewStory)?.setOnClickListener {
            startActivity(Intent(this, ViewStoryActivity::class.java))
        }

        findViewById<View>(R.id.btnYourProfile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<View>(R.id.btnComments)?.setOnClickListener {
            startActivity(Intent(this, CommentsActivity::class.java))
        }

        findViewById<View>(R.id.btnReactions)?.setOnClickListener {
            startActivity(Intent(this, ReactionPickerActivity::class.java))
        }

        loadStoriesFromFirebase()
    }

    private fun loadStoriesFromFirebase() {
        val currentUid = auth.currentUser?.uid ?: ""
        val dbRef = FirebaseDatabase.getInstance().reference.child("stories")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()

                // Keep only "Create story" view in container
                val createStoryView = layoutStoriesContainer.findViewById<View>(R.id.btnCreateStory)
                layoutStoriesContainer.removeAllViews()
                if (createStoryView != null) {
                    layoutStoriesContainer.addView(createStoryView)
                }

                val activeStories = mutableListOf<Story>()

                for (storySnap in snapshot.children) {
                    val story = storySnap.getValue(Story::class.java) ?: continue

                    // 24 Hour Expiration Filter
                    if (story.expiresAt > now) {
                        activeStories.add(story)
                    } else {
                        // Remove expired story from database
                        storySnap.ref.removeValue()
                    }
                }

                activeStories.sortByDescending { it.timestamp }

                for (story in activeStories) {
                    val storyCard = TextView(this@FeedActivity).apply {
                        val params = LinearLayout.LayoutParams(
                            dpToPx(85),
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        params.setMargins(0, 0, dpToPx(6), 0)
                        layoutParams = params

                        val bgColors = arrayOf("#2E3C4D", "#D1B8AC", "#8B9D83", "#5C6B73", "#7A8B99")
                        val colorIdx = (story.username.hashCode() and 0x7FFFFFFF) % bgColors.size
                        setBackgroundColor(Color.parseColor(bgColors[colorIdx]))

                        gravity = Gravity.BOTTOM
                        setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
                        text = story.username
                        setTextColor(Color.WHITE)
                        textSize = 10f

                        setOnClickListener {
                            if (story.userId.isNotEmpty() && story.userId == currentUid) {
                                val intent = Intent(this@FeedActivity, YourStoryActivity::class.java)
                                intent.putExtra("story_id", story.storyId)
                                startActivity(intent)
                            } else {
                                val intent = Intent(this@FeedActivity, ViewStoryActivity::class.java)
                                intent.putExtra("story_id", story.storyId)
                                intent.putExtra("username", story.username)
                                intent.putExtra("text_label", story.textLabel)
                                intent.putExtra("media_uri", story.mediaUri)
                                intent.putExtra("media_type", story.mediaType)
                                startActivity(intent)
                            }
                        }
                    }

                    layoutStoriesContainer.addView(storyCard)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
