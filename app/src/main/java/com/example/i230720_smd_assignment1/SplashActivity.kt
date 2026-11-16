package com.example.i230720_smd_assignment1

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val uid = currentUser.uid
            val dbRef = FirebaseDatabase.getInstance().reference

            try {
                dbRef.child("users").child(uid).child("status").setValue("online")
            } catch (_: Exception) {}

            val dbTimeoutRunnable = Runnable {
                if (!isFinishing && !isDestroyed) {
                    startActivity(Intent(this@SplashActivity, FeedActivity::class.java))
                    finish()
                }
            }
            handler.postDelayed(dbTimeoutRunnable, 2500)

            try {
                dbRef.child("users").child(uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            handler.removeCallbacks(dbTimeoutRunnable)
                            if (isFinishing || isDestroyed) return

                            val isFirstTime = snapshot.child("isFirstTime").getValue(Boolean::class.java) ?: false
                            val username = snapshot.child("username").getValue(String::class.java) ?: ""

                            if (isFirstTime || username.isEmpty()) {
                                startActivity(Intent(this@SplashActivity, EditProfileActivity::class.java))
                            } else {
                                startActivity(Intent(this@SplashActivity, FeedActivity::class.java))
                            }
                            finish()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            handler.removeCallbacks(dbTimeoutRunnable)
                            if (!isFinishing && !isDestroyed) {
                                startActivity(Intent(this@SplashActivity, FeedActivity::class.java))
                                finish()
                            }
                        }
                    })
            } catch (_: Exception) {
                handler.removeCallbacks(dbTimeoutRunnable)
                startActivity(Intent(this@SplashActivity, FeedActivity::class.java))
                finish()
            }
        } else {
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
