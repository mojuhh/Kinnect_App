package com.example.i230720_smd_assignment1

import android.app.Application

class KinnectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
