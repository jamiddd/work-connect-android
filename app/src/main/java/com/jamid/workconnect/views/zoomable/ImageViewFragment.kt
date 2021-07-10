package com.jamid.workconnect.views.zoomable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.navigation.fragment.findNavController
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentImageViewBinding
import com.jamid.workconnect.interfaces.OnScaleListener
import com.jamid.workconnect.model.SimpleMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ImageViewFragment : SupportFragment(R.layout.fragment_image_view), OnScaleListener {

    private lateinit var binding: FragmentImageViewBinding
    private var mImageView: ZoomableDraweeView? = null
    private var message: SimpleMessage? = null
    private var transitionName: String? = null
    private var params = Pair(0, 0)
    private var image: String? = null
    private var windowWidth = 0
    private var scaleRatio = 1f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            scrimColor = Color.TRANSPARENT
        }
        exitTransition = MaterialFadeThrough()
        postponeEnterTransition()
        val root = inflater.inflate(R.layout.fragment_image_view, container, false)
        message = arguments?.getParcelable(ARG_MESSAGE)
        params = Pair(arguments?.getInt(ARG_WIDTH) ?: 0, arguments?.getInt(ARG_HEIGHT) ?: 0)

        scaleRatio = (params.first/params.second).toFloat()

        windowWidth = getWindowWidth()
        val zoomView = root.findViewById<ZoomableDraweeView>(R.id.fullscreenImage)
        zoomView?.updateLayout(height = convertDpToPx(params.second), width = windowWidth)
        transitionName = arguments?.getString(ARG_TRANSITION_NAME)
        zoomView?.transitionName = message?.messageId ?: transitionName

        return root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentImageViewBinding.bind(view)

        mImageView = binding.fullscreenImage
        image = arguments?.getString(ARG_IMAGE)

        binding.imageViewFragmentToolbar.setNavigationOnClickListener {
            if (message != null) {
                activity.supportFragmentManager.beginTransaction().remove(this).commit()
            } else {
                findNavController().navigateUp()
            }
        }

        mImageView?.apply {
            setAllowTouchInterceptionWhileZoomed(false)
            setIsLongpressEnabled(false)
            setTapListener(TapListener(mImageView!!))

            val imageRequest = if (message != null && message!!.isNotEmpty()) {
                val senderText = "Sent by " + message!!.sender.name
                binding.sentByText.text = senderText

                val size = message!!.metaData?.size_b ?: 0

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

                val timeText = SimpleDateFormat("hh:mm a E, dd MMM", Locale.US).format(message!!.createdAt) + " • " + sizeText
                binding.timeAndSizeText.text = timeText

                binding.imageViewFragmentToolbar.title = message!!.sender.name

                view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
                    val file = File(it, message!!.metaData!!.originalFileName)
                    val imageRequest = ImageRequest.fromFile(file)
                    imageRequest
                }
            } else {
                binding.imageDetailBottom.visibility = View.GONE
                activity.mainBinding.bottomNavBackground.visibility = View.VISIBLE
                ImageRequest.fromUri(image)
            }

            val controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setCallerContext(this)
                .build()
            setController(controller)

            startPostponedEnterTransition()

        }

        binding.fullscreenImage.setScaleListener(this)

        binding.fullscreenImage.setOnClickListener {
            if (message != null && message!!.isNotEmpty()) {
                if (binding.imageDetailBottom.translationY == 0f) {
                    hideTopAndBottomActions(binding.imageViewFragmentAppBar, binding.imageDetailBottom)
                } else {
                    showTopAndBottomActions(binding.imageViewFragmentAppBar, binding.imageDetailBottom)
                }
            } else {
                if (activity.mainBinding.bottomNavBackground.translationY == 0f) {
                    hideTopAndBottomActions(binding.imageViewFragmentAppBar, binding.imageDetailBottom)
                } else {
                    showTopAndBottomActions(binding.imageViewFragmentAppBar, binding.imageDetailBottom)
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.imageViewFragmentToolbar.updateLayout(marginTop = top)

            binding.timeAndSizeText.updateLayout(marginBottom = bottom + convertDpToPx(8), extras = mapOf(
                START_TO_START to binding.imageDetailContainer.id, END_TO_END to binding.imageDetailContainer.id, BOTTOM_TO_BOTTOM to binding.imageDetailContainer.id, TOP_TO_BOTTOM to binding.sentByText.id))

        }
    }

    private fun hideTopAndBottomActions(top: View, bottom: View) {
        val animator = ObjectAnimator.ofFloat(top, View.TRANSLATION_Y, -top.measuredHeight.toFloat())
        val animator1 = ObjectAnimator.ofFloat(bottom, View.TRANSLATION_Y, bottom.measuredHeight.toFloat())
        val animator2 = ObjectAnimator.ofFloat(activity.mainBinding.bottomNavBackground, View.TRANSLATION_Y, activity.mainBinding.bottomNavBackground.measuredHeight.toFloat())

        AnimatorSet().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(animator, animator1, animator2)
            start()
        }
    }

    private fun showTopAndBottomActions(top: View, bottom: View) {
        val animator = ObjectAnimator.ofFloat(top, View.TRANSLATION_Y, 0f)
        val animator1 = ObjectAnimator.ofFloat(bottom, View.TRANSLATION_Y, 0f)
        val animator2 = ObjectAnimator.ofFloat(activity.mainBinding.bottomNavBackground, View.TRANSLATION_Y, 0f)

        AnimatorSet().apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            playTogether(animator, animator1, animator2)
            start()
        }
    }

    companion object {

        const val ARG_MESSAGE = "ARG_MESSAGE"
        const val ARG_TRANSITION_NAME = "ARG_TRANSITION_NAME"
        const val ARG_IMAGE = "ARG_IMAGE"
        const val ARG_WIDTH = "ARG_WIDTH"
        const val ARG_HEIGHT = "ARG_HEIGHT"
        const val TAG = "ImageViewFragment"

        @JvmStatic
        fun newInstance(message: SimpleMessage? = null, image: Pair<String, String>? = null, width: Int = 0, height: Int = 0)
            = ImageViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MESSAGE, message)
                    putString(ARG_TRANSITION_NAME, image?.first)
                    putString(ARG_IMAGE, image?.second)
                    putInt(ARG_WIDTH, width)
                    putInt(ARG_HEIGHT, height)
                }
            }
    }

    override fun onImageChange(scaleFactor: Float) {
        Log.d(TAG, scaleFactor.toString())
        if (scaleFactor <= 1f) {
            binding.fullscreenImage.updateLayout(convertDpToPx(params.second), windowWidth)
        } else {
            binding.fullscreenImage.updateLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

}