package com.jamid.workconnect.views

import android.view.ScaleGestureDetector

class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    var mScaleFactor = 1.0f
    private var mMaxZoom = 5.0f

    override fun onScale(detector: ScaleGestureDetector?): Boolean {
        mScaleFactor *= detector!!.scaleFactor
        mScaleFactor = 1.0f.coerceAtLeast(mScaleFactor.coerceAtMost(mMaxZoom))

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        return super.onScaleBegin(detector)
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        super.onScaleEnd(detector)
    }
}