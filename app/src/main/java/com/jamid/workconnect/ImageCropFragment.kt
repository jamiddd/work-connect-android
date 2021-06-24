package com.jamid.workconnect

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.canhub.cropper.CropImageView
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

        var currentDegrees = 0

        val freeMode = arguments?.getBoolean(ARG_FREE_MODE) ?: false
        var height = 0
        var width = 0
        val x: Int
        val y: Int
        val shape: String
        if (!freeMode) {
            shape = arguments?.getString(ARG_SHAPE) ?: ARG_SHAPE_OVAL
            x = arguments?.getInt(ARG_X) ?: 1
            y = arguments?.getInt(ARG_Y) ?: 1
            height = arguments?.getInt(ARG_HEIGHT) ?: 100
            width = arguments?.getInt(ARG_WIDTH) ?: 100

            when (shape) {
                ARG_SHAPE_OVAL -> {
                    binding.cropArea.cropShape = CropImageView.CropShape.OVAL
                }
                ARG_SHAPE_RECT -> {
                    binding.cropArea.cropShape = CropImageView.CropShape.RECTANGLE
                }
            }


            binding.cropArea.setFixedAspectRatio(true)
            binding.cropArea.setAspectRatio(x, y)
        }

        viewModel.currentImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.cropArea.setImageUriAsync(it)
                viewModel.setCurrentImage(null)
            }
        }

        binding.cancelCropBtn.setOnClickListener {
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

        binding.cropBtn.setOnClickListener {
            val bitmap = if (width == 0) {
                binding.cropArea.croppedImage
            } else {
                binding.cropArea.getCroppedImage(width, height)
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

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val windowHeight = getWindowHeight()
//            val rect = windowHeight - top

            val params = binding.root.layoutParams as ViewGroup.LayoutParams
            params.height = windowHeight
            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            binding.root.layoutParams = params

        }

    }

    companion object {

        const val TAG = "ImageCropFragment"
        const val ARG_SHAPE = "ARG_SHAPE"
        const val ARG_X = "ARG_X"
        const val ARG_Y = "ARG_Y"
        const val ARG_WIDTH = "ARG_WIDTH"
        const val ARG_HEIGHT = "ARG_HEIGHT"
        const val ARG_SHAPE_RECT = "RECTANGLE"
        const val ARG_SHAPE_OVAL = "OVAL"
        const val ARG_FREE_MODE = "ARG_FREE_MODE"

        @JvmStatic
        fun newInstance(x: Int = 1, y: Int = 1, width: Int = 300, height: Int = 300, shape: String = ARG_SHAPE_OVAL) =
            ImageCropFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_X, x)
                    putInt(ARG_Y, y)
                    putInt(ARG_HEIGHT, height)
                    putInt(ARG_WIDTH, width)
                    putString(ARG_SHAPE, shape)
                }
            }

        @JvmStatic
        fun newInstance(bundle: Bundle?) : ImageCropFragment {
            return ImageCropFragment().apply {
                arguments = bundle
            }
        }
    }
}