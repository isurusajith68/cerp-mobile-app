package com.ceyinfo.cerp

import android.app.Application

class CerpApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CerpApp
            private set
    }
}
