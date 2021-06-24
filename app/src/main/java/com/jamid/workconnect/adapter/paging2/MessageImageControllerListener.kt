package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.graphics.drawable.Animatable
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.model.SimpleMessage

class MessageImageControllerListener(
    private val v: SimpleDraweeView,
    private val v1: ConstraintLayout,
    private val v2: TextView,
    private val v3: SimpleDraweeView?,
    private val context: Context,
    private val progress: ProgressBar,
    private val right: Boolean,
    private val message: SimpleMessage,
    private val viewModel: MainViewModel
) : BaseControllerListener<ImageInfo>() {

	var measuredHeight = 0
	var measuredWidth = 0

	override fun onSubmit(id: String?, callerContext: Any?) {
		super.onSubmit(id, callerContext)
	}

	override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
		v.colorFilter = null
		progress.visibility = View.GONE
		message.isDownloaded = true
		viewModel.updateMessage(message)

		val height = imageInfo?.height ?: 0
		val width = imageInfo?.width ?: 0

		measuredWidth = width
		measuredHeight = height
		v.translationY = convertDpToPx(2, context).toFloat()

		if (width >= height) {
			val newParams = ConstraintLayout.LayoutParams(
				convertDpToPx(200, context), convertDpToPx(150, context)
			)

			newParams.horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED

			if (right) {
				newParams.horizontalBias = 1f

				newParams.topToTop = v1.id
				newParams.startToStart = v1.id
				newParams.endToEnd = v1.id
				newParams.bottomToTop = v2.id

				newParams.setMargins(
					convertDpToPx(0, context),
					convertDpToPx(4, context),
					convertDpToPx(16, context),
					convertDpToPx(4, context)
				)
			} else {
				newParams.topToTop = v1.id
				newParams.endToEnd = v1.id
				newParams.bottomToTop = v2.id
				newParams.startToEnd = v3!!.id

				newParams.setMargins(
					convertDpToPx(16, context),
					convertDpToPx(4, context),
					convertDpToPx(0, context),
					convertDpToPx(4, context)
				)
			}

			v.layoutParams = newParams
		} else {
			val newParams = ConstraintLayout.LayoutParams(
				convertDpToPx(150, context), convertDpToPx(200, context)
			)

			newParams.horizontalChainStyle = ConstraintLayout.LayoutParams.CHAIN_PACKED

			if (right) {
				newParams.horizontalBias = 1f

				newParams.topToTop = v1.id
				newParams.startToStart = v1.id
				newParams.endToEnd = v1.id
				newParams.bottomToTop = v2.id

				newParams.setMargins(
					convertDpToPx(0, context),
					convertDpToPx(4, context),
					convertDpToPx(16, context),
					convertDpToPx(4, context)
				)
			} else {

				newParams.topToTop = v1.id
				newParams.endToEnd = v1.id
				newParams.bottomToTop = v2.id
				newParams.startToEnd = v3!!.id

				newParams.setMargins(
					convertDpToPx(16, context),
					convertDpToPx(4, context),
					convertDpToPx(0, context),
					convertDpToPx(4, context)
				)
			}

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