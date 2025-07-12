package com.example.v02.timelimit

import android.app.Application

class AppLimitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLimits.initialize(this)
    }
}
