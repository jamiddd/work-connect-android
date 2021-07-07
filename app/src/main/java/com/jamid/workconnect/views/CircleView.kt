package com.jamid.workconnect.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.util.AttributeSet
import android.view.View


class CircleView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var circleColor = DEFAULT_CIRCLE_COLOR
    private var paint: Paint? = null

    init {
        paint = Paint()
        paint?.isAntiAlias = true
    }

    fun setCircleColor(circleColor: Int) {
        this.circleColor = circleColor
        invalidate()
    }

    fun getCircleColor(): Int {
        return circleColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width
        val h = height
        val pl: Int = paddingLeft
        val pr: Int = paddingRight
        val pt: Int = paddingTop
        val pb: Int = paddingBottom
        val usableWidth = w - (pl + pr)
        val usableHeight = h - (pt + pb)
        val radius = usableWidth.coerceAtMost(usableHeight) / 2
        val cx = pl + usableWidth / 2
        val cy = pt + usableHeight / 2
        paint?.color = circleColor
        canvas.drawCircle(cx.toFloat(), cy.toFloat(), radius.toFloat(), paint!!)
    }

    companion object {
        private const val DEFAULT_CIRCLE_COLOR: Int = Color.RED
    }
}