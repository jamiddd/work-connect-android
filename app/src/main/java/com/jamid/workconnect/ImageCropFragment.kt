package com.jamid.workconnect

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.canhub.cropper.CropImageOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.jamid.workconnect.databinding.FragmentImageCropBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageCropFragment : Fragment(R.layout.fragment_image_crop) {

    private lateinit var binding: FragmentImageCropBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentImageCropBinding.bind(view)
        val activity = requireActivity() as MainActivity
        val options = arguments?.getParcelable<CropImageOptions>("menu")
        if (options != null) {
            Log.d(TAG, "Options is not null")
            binding.cropArea.apply {
                cropShape = options.cropShape
                setAspectRatio(options.aspectRatioX, options.aspectRatioY)
                setFixedAspectRatio(options.fixAspectRatio)
            }
        } else {
            Log.d(TAG, "Options is null")
        }

        var currentDegrees = 0

        viewModel.currentImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.cropArea.setImageUriAsync(it)
                viewModel.setCurrentImage(null)
            }
        }

        binding.cropToolbar.setNavigationOnClickListener {
            activity.bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.rotateRightBtn.setOnClickListener {
            binding.cropArea.rotatedDegrees = currentDegrees + 90
            currentDegrees += 90
        }

        binding.rotateLeftBtn.setOnClickListener {
            binding.cropArea.rotatedDegrees = currentDegrees - 90
            currentDegrees -= 90
        }

        binding.cropButton.setOnClickListener {

            val bitmap = if (options != null) {
                binding.cropArea.getCroppedImage(options.maxCropResultWidth, options.maxCropResultHeight)
            } else {
                binding.cropArea.croppedImage
            }

            if (bitmap != null) {

                val externalDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val name = "temp_" + System.currentTimeMillis().toString()
                val file = File(externalDir, name)

                if (file.createNewFile()) {
                    val byteArrayOutputStream = ByteArrayOutputStream()

                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                    val ba = byteArrayOutputStream.toByteArray()

                    val fos = FileOutputStream(file)
                    fos.write(ba)
                    fos.flush()
                    fos.close()
                    byteArrayOutputStream.flush()

                    val uri = FileProvider.getUriForFile(activity, "com.jamid.workconnect.fileprovider", file)
                    viewModel.setCurrentCroppedImageUri(uri)
                } else {
                    viewModel.setCurrentError(Exception("Could not create file."))
                }

                activity.hideBottomSheet()
            }

        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (_, bottom) ->
            val windowHeight = getWindowHeight()
            binding.root.updateLayout(windowHeight)
            binding.cropBottomAction.setContentPadding(0, 0, 0, bottom)
            binding.cropArea.updateLayout(marginBottom = binding.cropBottomActionContainer.measuredHeight + bottom)
        }

    }

    companion object {

        const val TAG = "ImageCropFragment"
        const val ARG_FREE_MODE = "ARG_FREE_MODE"
        const val ARG_CROP_OPTIONS = "ARG_CROP_OPTIONS"

        @JvmStatic
        fun newInstance(bundle: Bundle? = null) : ImageCropFragment {
            return ImageCropFragment().apply {
                arguments = bundle
            }
        }
    }
}