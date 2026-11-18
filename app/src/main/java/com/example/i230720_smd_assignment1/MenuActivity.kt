package com.example.i230720_smd_assignment1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)

        TabNavigation.setupTabs(this)

        findViewById<View>(R.id.btnSearch)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            val tokenStore = TokenStore(this)
            val apiService = ApiClient.create(tokenStore)

            apiService.logout().enqueue(object : Callback<ApiResponse<Map<String, Any>>> {
                override fun onResponse(call: Call<ApiResponse<Map<String, Any>>>, response: Response<ApiResponse<Map<String, Any>>>) {}
                override fun onFailure(call: Call<ApiResponse<Map<String, Any>>>, t: Throwable) {}
            })

            tokenStore.clear()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
