package com.jamid.workconnect.views.zoomable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.facebook.drawee.backends.pipeline.Fresco
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentImageViewBinding
import com.jamid.workconnect.message.DetailTransition
import com.jamid.workconnect.model.SimpleMessage
import java.text.SimpleDateFormat
import java.util.*

class ImageViewFragment : SupportFragment(R.layout.fragment_image_view, TAG, false) {

    private lateinit var binding: FragmentImageViewBinding
    private var mImageView: ZoomableDraweeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = null
        exitTransition = null
        sharedElementEnterTransition = DetailTransition(300, 100)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentImageViewBinding.bind(view)
        val message = arguments?.getParcelable<SimpleMessage>(ARG_MESSAGE) ?: return

        mImageView = binding.fullscreenImage
        mImageView?.transitionName = message.messageId

        mImageView?.apply {
            setAllowTouchInterceptionWhileZoomed(false)
            setIsLongpressEnabled(false)
            setTapListener(TapListener(mImageView!!))
            val controller = Fresco.newDraweeControllerBuilder()
                .setUri(message.content)
                .setCallerContext(this)
                .build()
            setController(controller)
        }
//        activity.setFragmentTitle(message.sender.name)
        viewModel.extras[ARG_MESSAGE] = message

        binding.sentByText.text = "Sent by " + message.sender.name

        val size = message.metaData?.size_b ?: 0

        val sizeText = when {
            size > (1024 * 1024) -> {
                val sizeInMB = size.toFloat()/(1024 * 1024)
                sizeInMB.toString().take(4) + " MB"
            }
            size/1024 > 100 -> {
                val sizeInMB = size.toFloat()/(1024 * 1024)
                sizeInMB.toString().take(4) + " MB"
            }
            else -> {
                val sizeInKB = size.toFloat()/1024
                sizeInKB.toString().take(3) + " KB"
            }
        }

        binding.timeAndSizeText.text = SimpleDateFormat("hh:mm a E, dd MMM", Locale.US).format(message.createdAt) + " • " + sizeText
        val insets = viewModel.windowInsets.value!!

        binding.timeAndSizeText.updateLayout(marginBottom = insets.second + convertDpToPx(8), extras = mapOf(
            START_TO_START to binding.imageDetailContainer.id, END_TO_END to binding.imageDetailContainer.id, BOTTOM_TO_BOTTOM to binding.imageDetailContainer.id, TOP_TO_BOTTOM to binding.sentByText.id))

        binding.fullscreenImage.setOnClickListener {
            if (binding.imageDetailBottom.translationY == 0f) {
                hideTopAndBottomActions(activity.mainBinding.primaryAppBar, binding.imageDetailBottom)
            } else {
                showTopAndBottomActions(activity.mainBinding.primaryAppBar, binding.imageDetailBottom)
            }
        }

    }

    private fun hideTopAndBottomActions(top: View, bottom: View) {
        val animator = ObjectAnimator.ofFloat(top, View.TRANSLATION_Y, -top.measuredHeight.toFloat())
        val animator1 = ObjectAnimator.ofFloat(bottom, View.TRANSLATION_Y, bottom.measuredHeight.toFloat())

        AnimatorSet().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(animator, animator1)
            start()
        }
    }

    private fun showTopAndBottomActions(top: View, bottom: View) {
        val animator = ObjectAnimator.ofFloat(top, View.TRANSLATION_Y, 0f)
        val animator1 = ObjectAnimator.ofFloat(bottom, View.TRANSLATION_Y, 0f)

        AnimatorSet().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(animator, animator1)
            start()
        }
    }

    companion object {

        const val ARG_MESSAGE = "ARG_MESSAGE"
        const val TAG = "ImageViewFragment"

        @JvmStatic
        fun newInstance(message: SimpleMessage)
            = ImageViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MESSAGE, message)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activity.mainBinding.primaryAppBar.translationY != 0f) {
            showTopAndBottomActions(activity.mainBinding.primaryAppBar, binding.imageDetailBottom)
        }
    }

}