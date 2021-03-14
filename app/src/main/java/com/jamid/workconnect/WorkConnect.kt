package com.jamid.workconnect

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco

class WorkConnect: Application() {
    override fun onCreate() {
        super.onCreate()
        Fresco.initialize(this)
    }
}