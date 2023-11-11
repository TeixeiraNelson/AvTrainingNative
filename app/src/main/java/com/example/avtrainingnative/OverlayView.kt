package com.example.avtrainingnative

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint: Paint = Paint()

    init {
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
    }

    // Set the rectangle coordinates and dimensions
    private var rectLeft = 50f
    private var rectTop = 50f
    private var rectWidth = 200f
    private var rectHeight = 100f

    // Method to update the rectangle position
    fun setRectCenter(left: Float, top: Float) {
        rectLeft = left - rectWidth / 2
        rectTop = top - rectHeight / 2
        invalidate() // Request a redraw
    }

    fun setRectDimensions(width: Float, height: Float) {
        rectWidth = width
        rectHeight = height
        invalidate() // Request a redraw
    }

    override fun onDraw(canvas: Canvas) {
        // Draw the rectangle
        canvas.drawRect(rectLeft, rectTop, rectLeft + rectWidth, rectTop + rectHeight, paint)
    }
}