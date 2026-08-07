package com.prom3x209.cornermask

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

class CornerMaskView(context: Context, private val corner: Int, private val radiusPx: Int) : View(context) {

    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas) {
        val r = radiusPx.toFloat()
        val h = height.toFloat()
        val cy = if (corner == 0) r else h - r

        val fullSquare = Path().apply { addRect(0f, 0f, r, h, Path.Direction.CW) }
        val circle = Path().apply { addCircle(r, cy, r, Path.Direction.CW) }
        fullSquare.op(circle, Path.Op.DIFFERENCE)

        canvas.drawPath(fullSquare, blackPaint)
    }
}
