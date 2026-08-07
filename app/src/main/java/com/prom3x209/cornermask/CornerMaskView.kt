package com.prom3x209.cornermask

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View

class CornerMaskView(context: Context, private val corner: Int, private val radiusPx: Int) : View(context) {

    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = radiusPx.toFloat()
        canvas.drawRect(0f, 0f, w, h, blackPaint)

        val cx = r
        val cy = if (corner == 0) r else h - r

        val path = Path()
        path.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.drawPath(path, clearPaint)
    }
}
