package com.jamid.workconnect.adapter

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.Log
import android.widget.LinearLayout
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import com.jamid.workconnect.convertDpToPx

class MessageImageControllerListener(val v: SimpleDraweeView, val context: Context, private val right: Boolean): BaseControllerListener<ImageInfo>() {

    var measuredHeight = 0
    var measuredWidth = 0

    override fun onSubmit(id: String?, callerContext: Any?) {
        super.onSubmit(id, callerContext)
    }

    override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
        val height = imageInfo?.height ?: 0
        val width = imageInfo?.width ?: 0

        measuredWidth = width
        measuredHeight = height

        if (width >= height) {
            val newParams = LinearLayout.LayoutParams(
                convertDpToPx(200, context), convertDpToPx(150, context)
            )

//            newParams.topToTop = v1.id
//            newParams.endToEnd = v1.id
            if (right) {

//                newParams.horizontalBias = 1f
                newParams.setMargins(
                    convertDpToPx(0, context),
                    convertDpToPx(0, context),
                    convertDpToPx(10, context),
                    convertDpToPx(0, context)
                )
            } else {
//                newParams.horizontalBias = 0f
                newParams.setMargins(
                    convertDpToPx(12, context),
                    convertDpToPx(0, context),
                    convertDpToPx(0, context),
                    convertDpToPx(0, context)
                )
            }
//            newParams.startToStart = v1.id
//            newParams.verticalBias = 0f
//            newParams.bottomToTop = v2.id
//            newParams.verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED

            v.layoutParams = newParams
        } else {
            // vertical

            val newParams = LinearLayout.LayoutParams(
                convertDpToPx(150, context), convertDpToPx(200, context)
            )

            if (right) {
                Log.d("IMAGECONTROLLER", "RIGHT")
//                newParams.horizontalBias = 1f
//
                newParams.setMargins(
                    convertDpToPx(0, context),
                    convertDpToPx(0, context),
                    convertDpToPx(10, context),
                    convertDpToPx(0, context)
                )
            } else {
                Log.d("IMAGECONTROLLER", "LEFT")

//                newParams.horizontalBias = 0f
                newParams.setMargins(
                    convertDpToPx(12, context),
                    convertDpToPx(0, context),
                    convertDpToPx(0, context),
                    convertDpToPx(0, context)
                )
            }
//            newParams.startToStart = v1.id
//            newParams.topToTop = v1.id
//            newParams.endToEnd = v1.id
//            newParams.verticalBias = 0f
//            newParams.bottomToTop = v2.id
//            newParams.verticalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED

            v.layoutParams = newParams
        }

    }

    override fun onIntermediateImageSet(id: String?, imageInfo: ImageInfo?) {
        super.onIntermediateImageSet(id, imageInfo)
    }

    override fun onIntermediateImageFailed(id: String?, throwable: Throwable?) {
        super.onIntermediateImageFailed(id, throwable)
    }

    override fun onFailure(id: String?, throwable: Throwable?) {
        super.onFailure(id, throwable)
    }

    override fun onRelease(id: String?) {
        super.onRelease(id)
    }
}