package com.prom3x209.cornermask

import android.accessibilityservice.AccessibilityService
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class CornerMaskAccessibilityService : AccessibilityService() {

    companion object {
        const val RADIUS_PX = 108
    }

    private lateinit var wm: WindowManager
    private val overlayViews = mutableListOf<View>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showForCurrentOrientation()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::wm.isInitialized) showForCurrentOrientation()
    }

    private fun showForCurrentOrientation() {
        removeOverlays()
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            addCorner(0, Gravity.TOP or Gravity.START)
            addCorner(2, Gravity.BOTTOM or Gravity.START)
        }
    }

    private fun addCorner(cornerId: Int, gravity: Int) {
        val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY

        val view = CornerMaskView(this, cornerId, RADIUS_PX)
        val lp = WindowManager.LayoutParams(
            RADIUS_PX, RADIUS_PX,
            type, flags, PixelFormat.TRANSLUCENT
        )
        lp.gravity = gravity
        wm.addView(view, lp)
        overlayViews.add(view)
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
}
