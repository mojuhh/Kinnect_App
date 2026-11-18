package com.example.i230720_smd_assignment1

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeedActivity : AppCompatActivity() {

    private lateinit var layoutStoriesContainer: LinearLayout
    private lateinit var btnCreateStory: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.feed)

        TabNavigation.setupTabs(this)

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

        loadStoriesFromServer()
    }

    private fun loadStoriesFromServer() {
        val tokenStore = TokenStore(this)
        val apiService = ApiClient.create(tokenStore)

        apiService.getStories().enqueue(object : Callback<ApiResponse<List<Map<String, Any>>>> {
            override fun onResponse(call: Call<ApiResponse<List<Map<String, Any>>>>, response: Response<ApiResponse<List<Map<String, Any>>>>) {
                val body = response.body()
                if (response.isSuccessful && body != null && body.ok && body.data != null) {
                    val createStoryView = layoutStoriesContainer.findViewById<View>(R.id.btnCreateStory)
                    layoutStoriesContainer.removeAllViews()
                    if (createStoryView != null) {
                        layoutStoriesContainer.addView(createStoryView)
                    }

                    for (s in body.data) {
                        val username = s["username"] as? String ?: s["name"] as? String ?: "User"
                        val mediaPath = s["media_path"] as? String ?: ""
                        val mediaType = s["media_type"] as? String ?: "image"
                        val label = s["label"] as? String ?: ""

                        addStoryCard(username, mediaPath, mediaType, label)
                    }

                    if (body.data.isEmpty()) {
                        addStoryCard("Omar Farooq", "", "image", "Karachi nights")
                        addStoryCard("Sara Iqbal", "", "image", "Weekend vibes")
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<Map<String, Any>>>>, t: Throwable) {
                addStoryCard("Omar Farooq", "", "image", "Karachi nights")
                addStoryCard("Sara Iqbal", "", "image", "Weekend vibes")
            }
        })
    }

    private fun addStoryCard(username: String, mediaPath: String, mediaType: String, label: String) {
        val storyCard = TextView(this).apply {
            val params = LinearLayout.LayoutParams(
                dpToPx(85),
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            params.setMargins(0, 0, dpToPx(6), 0)
            layoutParams = params

            val bgColors = arrayOf("#2E3C4D", "#D1B8AC", "#8B9D83", "#5C6B73", "#7A8B99")
            val colorIdx = (username.hashCode() and 0x7FFFFFFF) % bgColors.size
            setBackgroundColor(Color.parseColor(bgColors[colorIdx]))

            gravity = Gravity.BOTTOM
            setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            text = username
            setTextColor(Color.WHITE)
            textSize = 10f

            setOnClickListener {
                val intent = Intent(this@FeedActivity, ViewStoryActivity::class.java)
                intent.putExtra("username", username)
                intent.putExtra("text_label", label)
                intent.putExtra("media_uri", if (mediaPath.isNotEmpty()) "${BuildConfig.BASE_URL}$mediaPath" else "")
                intent.putExtra("media_type", mediaType)
                startActivity(intent)
            }
        }
        layoutStoriesContainer.addView(storyCard)
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
