package com.jamid.workconnect.views.zoomable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.transition.ChangeBounds
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.addCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentImageViewBinding
import com.jamid.workconnect.message.DetailTransition
import com.jamid.workconnect.model.SimpleMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ImageViewFragment : SupportFragment(R.layout.fragment_image_view, TAG, false) {

    private lateinit var binding: FragmentImageViewBinding
    private var mImageView: ZoomableDraweeView? = null
    private lateinit var message: SimpleMessage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            scrimColor = Color.TRANSPARENT
            duration = 150
        }
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        postponeEnterTransition()
        val root = inflater.inflate(R.layout.fragment_image_view, container, false)
        message = arguments?.getParcelable(ARG_MESSAGE)!!
        root.findViewById<ZoomableDraweeView>(R.id.fullscreenImage)?.transitionName = message.messageId
        return root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentImageViewBinding.bind(view)

        mImageView = binding.fullscreenImage

        binding.imageViewFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        mImageView?.apply {
            setAllowTouchInterceptionWhileZoomed(false)
            setIsLongpressEnabled(false)
            setTapListener(TapListener(mImageView!!))

            view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
                val file = File(it, message.metaData!!.originalFileName)
                val imageRequest = ImageRequest.fromFile(file)

                val controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .setCallerContext(this)
                    .build()
                startPostponedEnterTransition()
                setController(controller)
            }

        }


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

        binding.imageViewFragmentToolbar.title = message.sender.name

        binding.fullscreenImage.setOnClickListener {
            if (binding.imageDetailBottom.translationY == 0f) {
                hideTopAndBottomActions(binding.imageViewFragmentAppBar, binding.imageDetailBottom)
            } else {
                showTopAndBottomActions(binding.imageViewFragmentAppBar, binding.imageDetailBottom)
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
        /*if (activity.mainBinding.primaryAppBar.translationY != 0f) {
            showTopAndBottomActions(activity.mainBinding.primaryAppBar, binding.imageDetailBottom)
        }*/
        val params = activity.mainBinding.navHostFragment.layoutParams as CoordinatorLayout.LayoutParams
        params.behavior = AppBarLayout.ScrollingViewBehavior()
        activity.mainBinding.navHostFragment.layoutParams = params
    }

}