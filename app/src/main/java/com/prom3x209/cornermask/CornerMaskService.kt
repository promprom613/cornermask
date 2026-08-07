package com.prom3x209.cornermask

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class CornerMaskService : Service() {

    companion object {
        const val RADIUS_PX = 108
    }

    private lateinit var wm: WindowManager
    private val overlayViews = mutableListOf<View>()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundNotification()
        addOverlays()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

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

    private fun addOverlays() {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val corners = listOf(
            0 to (Gravity.TOP or Gravity.START),
            1 to (Gravity.BOTTOM or Gravity.START)
        )

        for ((cornerId, gravity) in corners) {
            val view = CornerMaskView(this, cornerId, RADIUS_PX)
            val lp = WindowManager.LayoutParams(
                RADIUS_PX, RADIUS_PX,
                type, flags, PixelFormat.TRANSLUCENT
            )
            lp.gravity = gravity
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
