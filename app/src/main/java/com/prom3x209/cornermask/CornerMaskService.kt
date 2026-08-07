package com.prom3x209.cornermask

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.sqrt

class CornerMaskService : Service() {

    companion object {
        const val RADIUS_PX = 108
        const val STRIPS = 16
    }

    private lateinit var wm: WindowManager
    private val overlayViews = mutableListOf<View>()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        showForCurrentOrientation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        showForCurrentOrientation()
    }

    private fun startForegroundNotification() {
        val channelId = "corner_mask_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Corner Mask",
                NotificationManager.IMPORTANCE_MIN
            )
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Corner mask running")
            .setSmallIcon(android.R.drawable.ic_menu_crop)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    private fun showForCurrentOrientation() {
        removeOverlays()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            addCornerStrips(top = true, left = true)
            addCornerStrips(top = false, left = true)
        }
        // Portrait: no corners at all — don't touch normal browsing/wallpaper.
    }

    private fun addCornerStrips(top: Boolean, left: Boolean) {
        val r = RADIUS_PX.toFloat()
        val stripH = RADIUS_PX / STRIPS

        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val gravity = (if (top) Gravity.TOP else Gravity.BOTTOM) or
            (if (left) Gravity.START else Gravity.END)

        for (i in 0 until STRIPS) {
            val y0 = i * r / STRIPS
            val y1 = (i + 1) * r / STRIPS
            val yMid = (y0 + y1) / 2f
            val inside = r * r - (r - yMid) * (r - yMid)
            val blackWidth = (r - sqrt(if (inside < 0f) 0f else inside)).toInt() + 1

            if (blackWidth <= 0) continue

            val view = View(this).apply { setBackgroundColor(Color.BLACK) }
            val lp = WindowManager.LayoutParams(
                blackWidth.coerceAtMost(RADIUS_PX), stripH + 1,
                type, flags, PixelFormat.OPAQUE
            )
            lp.gravity = gravity
            lp.y = y0.toInt()
            wm.addView(view, lp)
            overlayViews.add(view)
        }
    }

    private fun removeOverlays() {
        for (v in overlayViews) {
            try { wm.removeView(v) } catch (e: Exception) { }
        }
        overlayViews.clear()
    }

    override fun onDestroy() {
        removeOverlays()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
