package com.jamid.workconnect

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jamid.workconnect.databinding.GenericDialogLayoutBinding
import com.jamid.workconnect.interfaces.GenericDialogListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GenericDialogFragment: Fragment(R.layout.generic_dialog_layout) {

    private lateinit var binding: GenericDialogLayoutBinding
    private lateinit var activity: MainActivity
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var genericDialogListener: GenericDialogListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = context as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = GenericDialogLayoutBinding.bind(view)
        genericDialogListener = activity

        if (Build.VERSION.SDK_INT <= 27) {
//            binding.genericDialogRoot.removeView(binding.dialogBackgroundBlur)
            binding.genericDialogRoot.setBackgroundColor(Color.WHITE)
        }

        val title = arguments?.getString(ARG_TITLE)
        val message = arguments?.getString(ARG_MESSAGE)
        val isProgressing = arguments?.getBoolean(ARG_IS_PROGRESSING) ?: false
        val isActionOn = arguments?.getBoolean(ARG_IS_ACTION_ON) ?: false
        val tag = arguments?.getString(ARG_DIALOG_TAG) ?: return
        val isCancelable = arguments?.getBoolean(ARG_IS_CANCELABLE) ?: false
        val selfDestructDuration = arguments?.getLong(ARG_SELF_DESTRUCT) ?: 0

        binding.dialogTitle.text = title
        binding.dialogMessage.text = message

        when (tag) {
            REMOVING_LOCATION -> {
                binding.dialogPositiveBtn.text = "Remove"
            }
            CREATING_BLOG, CREATING_PROJECT -> {
                /*viewModel.postUploadResult.observe(viewLifecycleOwner) {
                    val result = it ?: return@observe

                    activity.hideBottomSheet()

                    when (result) {
                        is Result.Success -> {
                            viewModel.clearPostChanges()
                        }
                        is Result.Error -> {
                            viewModel.setCurrentError(result.exception)
                        }
                    }
                }*/
            }
            CREATING_USER -> {

            }
            UPDATE_USER -> {

            }
            SIGN_IN_PROMPT -> {
                binding.dialogPositiveBtn.text = "Sign In"
            }
        }

        if (isProgressing) {
            binding.dialogProgressBar.visibility = View.VISIBLE
        } else {
            binding.dialogProgressBar.visibility = View.GONE
        }

        if (isActionOn) {
            binding.dialogActionBar.visibility = View.VISIBLE

            binding.dialogPositiveBtn.setOnClickListener {
                genericDialogListener.onDialogPositiveActionClicked(tag)
            }

            binding.dialogCancelBtn.setOnClickListener {
                genericDialogListener.onDialogNegativeActionClicked(tag)
            }

        } else {
            binding.dialogActionBar.visibility = View.GONE
        }

        activity.bottomSheetBehavior.isDraggable = isCancelable
        activity.bottomSheetBehavior.isHideable = isCancelable


        if (selfDestructDuration > 0) {
            lifecycleScope.launch {
                delay(selfDestructDuration)
                activity.hideBottomSheet()
            }
        }

        when (val image = arguments?.get(ARG_IMAGE)) {
            is String -> {
                binding.dialogImageView.visibility = View.VISIBLE
                Glide.with(activity)
                    .load(image)
                    .addListener(object: RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.dialogImageView.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_empty_posts))
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            //
                            return false
                        }

                    })
                    .into(binding.dialogImageView)
            }
            is Int -> {
                binding.dialogImageView.visibility = View.VISIBLE
                binding.dialogImageView.setImageDrawable(ContextCompat.getDrawable(activity, image))
            }
            else -> {
                binding.dialogImageView.visibility = View.GONE
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.dialogItemsContainer.setPadding(0, 0, 0, bottom + convertDpToPx(12))
        }
    }


    companion object {

        const val TAG = "GenericDialogFragment"
        const val ARG_TITLE = "ARG_TITLE"
        const val ARG_MESSAGE = "ARG_MESSAGE"
        const val ARG_IS_PROGRESSING = "ARG_IS_PROGRESSING"
        const val ARG_IMAGE = "ARG_IMAGE"
        const val ARG_IS_ACTION_ON = "ARG_IS_ACTION_ON"
        const val ARG_DIALOG_TAG = "ARG_DIALOG_TAG"
        const val ARG_IS_CANCELABLE = "ARG_IS_CANCELABLE"
        const val ARG_SELF_DESTRUCT = "ARG_SELF_DESTRUCT"

        @JvmStatic
        fun newInstance(tag: String, title: String, message: String, image: Any? = null, isProgressing: Boolean = false, isActionOn: Boolean = false, isCancelable: Boolean = false, selfDestructDuration: Long = 0) = GenericDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_DIALOG_TAG, tag)
                putString(ARG_TITLE, title)
                putString(ARG_MESSAGE, message)
                putBoolean(ARG_IS_PROGRESSING, isProgressing)
                putBoolean(ARG_IS_ACTION_ON, isActionOn)
                putBoolean(ARG_IS_CANCELABLE, isCancelable)
                putLong(ARG_SELF_DESTRUCT, selfDestructDuration)

                when (image) {
                    is String -> {
                        putString(ARG_IMAGE, image)
                    }
                    is Int -> {
                        putInt(ARG_IMAGE, image)
                    }
                    is Bitmap -> {
                        putParcelable(ARG_IMAGE, image)
                    }
                }

            }
        }

    }
}