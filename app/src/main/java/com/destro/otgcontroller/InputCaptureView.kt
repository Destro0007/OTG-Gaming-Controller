package com.destro.otgcontroller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class InputCaptureView(context: Context) : View(context) {
    private val paint = Paint(3)
    private var px = -1f
    private var py = -1f
    var onStatus: ((String) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        paint.color = 0xFF151B27.toInt()
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 18f, 18f, paint)
        paint.color = 0xFF35D0A0.toInt()
        if (px >= 0) canvas.drawCircle(px, py, 16f, paint)
        paint.color = -1
        paint.textSize = 32f
        canvas.drawText("Touch / mouse test area", 22f, 42f, paint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        px = e.x; py = e.y
        onStatus?.invoke("Touch: x=${e.x.toInt()} y=${e.y.toInt()} action=${e.actionMasked}")
        invalidate()
        return true
    }

    override fun onGenericMotionEvent(e: MotionEvent): Boolean {
        px = e.x; py = e.y
        onStatus?.invoke("Pointer: x=${e.x.toInt()} y=${e.y.toInt()} scroll=${e.getAxisValue(MotionEvent.AXIS_VSCROLL)}")
        invalidate()
        return true
    }
}
