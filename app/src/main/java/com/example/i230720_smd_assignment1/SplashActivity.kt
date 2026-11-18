package com.example.i230720_smd_assignment1

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private var hasNavigated = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        findViewById<View>(android.R.id.content)?.setOnClickListener {
            proceedNext()
        }

        handler.postDelayed({
            proceedNext()
        }, 2000)
    }

    private fun proceedNext() {
        if (hasNavigated) return
        hasNavigated = true

        val tokenStore = TokenStore(this)
        val token = tokenStore.token()

        if (!token.isNullOrEmpty()) {
            startActivity(Intent(this, FeedActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
