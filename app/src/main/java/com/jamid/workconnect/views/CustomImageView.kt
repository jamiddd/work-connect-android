package com.jamid.workconnect.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.GestureDetectorCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.jamid.workconnect.BUG_TAG
import com.jamid.workconnect.interfaces.CustomImageViewFlingListener
import kotlin.math.roundToInt


class CustomImageView(context:Context, attributeSet: AttributeSet? = null): AppCompatImageView(context, attributeSet), GestureDetector.OnGestureListener {

    private var mContext = context

    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mPosX = 0f
    private var mPosY = 0f
    private var returnPoint = 0f
    private val invalidPointerId = -1
    private var mActivePointerId = invalidPointerId
    private var mScaleListener: ScaleGestureListener? = null
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetectorCompat? = null

    private var mImageWidth = 0
    private var mImageHeight = 0
    private var mImageBitmap: Bitmap? = null

    private var customImageViewFlingListener: CustomImageViewFlingListener? = null

    init {
        mScaleListener = ScaleGestureListener()
        mScaleDetector = ScaleGestureDetector(mContext, mScaleListener)
        mGestureDetector = GestureDetectorCompat(context, this)
    }

    fun setOnFlingListener(listener: CustomImageViewFlingListener) {
        customImageViewFlingListener = listener
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (mImageBitmap != null && canvas != null) {
            val blank = (measuredHeight - mImageHeight)/2
            if (returnPoint == 0f) {
                returnPoint = blank.toFloat()
            }
            pivotX = (measuredHeight/2).toFloat()
            pivotY = (measuredWidth/2).toFloat()
            canvas.save()
            canvas.scale(mScaleListener!!.mScaleFactor, mScaleListener!!.mScaleFactor)
            canvas.translate(mPosX, mPosY)
            canvas.drawBitmap(mImageBitmap!!, 0f, blank.toFloat(), null)
            canvas.restore()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            mScaleDetector?.onTouchEvent(event)
            mGestureDetector?.onTouchEvent(event)

            val action = event.action
            when (action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    val x = event.x
                    val y = event.y

                    mLastTouchX = x
                    mLastTouchY = y

                    mActivePointerId = event.getPointerId(0)
                }
                MotionEvent.ACTION_MOVE -> {

                    val pointerIndex = event.findPointerIndex(mActivePointerId)

                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)

                    val dx = x - mLastTouchX
                    val dy = y - mLastTouchY

                    mPosX += dx
                    mPosY += dy

                    invalidate()

                    mLastTouchX = x
                    mLastTouchY = y

                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    performClick()
                    mActivePointerId = invalidPointerId
                    pullBackImage()
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = (action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr (MotionEvent.ACTION_POINTER_INDEX_SHIFT)
                    val pointerId = event.getPointerId(pointerIndex)
                    if (pointerId == mActivePointerId) {
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        mLastTouchX = event.getX(newPointerIndex)
                        mLastTouchY = event.getY(newPointerIndex)
                        mActivePointerId = event.getPointerId(newPointerIndex)
                    }
                }
            }

            return true
        } else {
            return super.onTouchEvent(event)
        }
    }

    private fun pullBackImage() {
        while (mPosY != 0f || mPosX != 0f) {
            if (mScaleListener!!.mScaleFactor != 1f) {
                if (mScaleListener!!.mScaleFactor - 0.1f < 1f) {
                    mScaleListener!!.mScaleFactor = 1f
                } else {
                    mScaleListener!!.mScaleFactor = mScaleListener!!.mScaleFactor - 0.1f
                }
            }
            when {
                mPosY > 0 -> {
                    if (mPosY - 0.5f < 0f) {
                        mPosY = 0f
                    } else {
                        mPosY -= 0.5f
                    }
                }
                mPosY < 0 -> {
                    if (mPosY + 0.5f > 0f) {
                        mPosY = 0f
                    } else {
                        mPosY += 0.5f
                    }
                }
                else -> {
                    mPosY = 0f
                }
            }

            when {
                mPosX > 0 -> {
                    if (mPosX - 0.5f < 0f) {
                        mPosX = 0f
                    } else {
                        mPosX -= 0.5f
                    }
                }
                mPosX < 0 -> {
                    if (mPosX + 0.5f > 0f) {
                        mPosX = 0f
                    } else {
                        mPosX += 0.5f
                    }
                }
                else -> {
                    mPosX = 0f
                }
            }
            Log.d(BUG_TAG, "$mPosX, $mPosY, ${mScaleListener!!.mScaleFactor}")
            invalidate()
        }
    }

    fun loadImage(uri: String?) {
        Glide.with(mContext)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    if (resource.height > 0 && resource.width > 0) {
                        val aspectRatio = resource.height.toFloat()/resource.width
                        mImageWidth = resources.displayMetrics.widthPixels
                        mImageHeight = (mImageWidth * aspectRatio).toDouble().roundToInt()
                        mImageBitmap = Bitmap.createScaledBitmap(resource, mImageWidth, mImageHeight, false)
                        invalidate()
                        requestLayout()
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
//                    mImageBitmap = null
                }
            })

    }

    override fun onDown(e: MotionEvent?): Boolean {
        return true
    }

    override fun onShowPress(e: MotionEvent?) {

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return true
    }

    override fun onLongPress(e: MotionEvent?) {

    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        /*Log.d(BUG_TAG, velocityY.toString())
        if (velocityY > 15000f || velocityY < 15000f) {
            customImageViewFlingListener?.onCustomImageFling()
        }*/
        return true
    }
}