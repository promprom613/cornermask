package com.prom3x209.cornermask

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            setContentView(TextView(this).apply {
                text = "One-time setup: grant the overlay permission on the next screen, " +
                    "then you can close this app — it won't need opening again."
                textSize = 16f
                setPadding(48, 96, 48, 48)
            })
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            startService(Intent(this, CornerMaskService::class.java))
            setContentView(TextView(this).apply {
                text = "Running. You can close this — it starts on its own from now on."
                textSize = 16f
                setPadding(48, 96, 48, 48)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            val svc = Intent(this, CornerMaskService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)
        }
    }
}
