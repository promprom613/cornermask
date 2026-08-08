package com.prom3x209.cornermask

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "One-time setup: on the next screen, find \"CornerMask\" under " +
                "Downloaded/Installed apps and turn it ON. After that you can close " +
                "this app for good — it runs automatically from here on, every reboot."
            textSize = 16f
            setPadding(48, 96, 48, 48)
        })
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
