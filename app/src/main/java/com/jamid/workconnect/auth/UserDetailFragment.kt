package com.jamid.workconnect.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentUserDetailBinding
import com.jamid.workconnect.interfaces.ImageSelectMenuListener
import com.jamid.workconnect.model.Result
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class UserDetailFragment : Fragment(R.layout.fragment_user_detail), ImageSelectMenuListener {

    private lateinit var binding: FragmentUserDetailBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var imageUrl: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentUserDetailBinding.bind(view)
        val activity = requireActivity() as MainActivity

        val popUpMenu = PopupMenu(requireContext(), binding.userImg, (Gravity.BOTTOM or Gravity.START))
        popUpMenu.menuInflater.inflate(R.menu.image_select_deselect_menu, popUpMenu.menu)
        binding.addImgBtn.setOnClickListener {
            popUpMenu.show()
            /*
            val fragment = ImageSelectFragment.newInstance()
            fragment.show(childFragmentManager, "ImageSelectFragment")*/
        }

        binding.userImg.isClickable = true
        binding.userImg.setOnClickListener {
            /*val fragment = ImageSelectFragment.newInstance()
            fragment.show(childFragmentManager, "ImageSelectFragment")*/
            popUpMenu.show()
        }

        popUpMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.select_from_gallery -> {

                }
                R.id.take_a_photo -> {

                }
                R.id.remove_image -> {

                }
            }
            true
        }

        viewModel.profilePhotoUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            binding.imageUploadProgress.visibility = View.GONE
            binding.userImg.colorFilter = null
            imageUrl = when (result) {
                is Result.Success -> {
                    result.data
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), result.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                    null
                }
            }
        }

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.addImgBtn.visibility = View.GONE

                val path = it.path
                binding.userImg.setImageURI(it.toString())

                binding.imageUploadProgress.visibility = View.VISIBLE
                binding.userImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.semiTransparentDark))
                viewModel.uploadProfilePhoto(path)

            } else {
                binding.addImgBtn.visibility = View.VISIBLE
                val s: String? = null
                binding.userImg.setImageURI(s)
                viewModel.uploadProfilePhoto(null)
            }
        }

        viewModel.firebaseUserUpdateResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    val navOptions = navOptions {
                        anim {
                            enter = R.anim.slide_in_right
                            exit = R.anim.slide_out_left
                            popEnter = R.anim.slide_in_left
                            popExit = R.anim.slide_out_right
                        }
                    }
                    findNavController().navigate(R.id.interestFragment, null, navOptions)
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), result.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val primaryBtn = activity.findViewById<Button>(R.id.primaryBtn)
        primaryBtn.isEnabled = false
        binding.userFullNameText.doAfterTextChanged {
            primaryBtn.isEnabled = !it.isNullOrBlank() && it.length > 3
        }

        primaryBtn.setOnClickListener {
            viewModel.updateFirebaseUser(imageUrl, binding.userFullNameText.text.trim().toString())
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            val windowHeight = getWindowHeight()
//            val rect = windowHeight - top

            val params = binding.root.layoutParams as ViewGroup.LayoutParams
            params.height = windowHeight
            params.width = ViewGroup.LayoutParams.MATCH_PARENT

            binding.root.layoutParams = params

        }

        OverScrollDecoratorHelper.setUpOverScroll(binding.root as ScrollView)

    }

    private fun selectImage() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, IMAGE_SELECT_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_SELECT_REQUEST -> {
                    val image = data?.data
                    viewModel.setCurrentImage(image)
                    findNavController().navigate(R.id.imageCropFragment)
                }
            }
        }
    }

    companion object {

        const val IMAGE_SELECT_REQUEST = 12

        @JvmStatic
        fun newInstance() = UserDetailFragment()
    }

    override fun onSelectImageFromGallery() {
        selectImage()
    }

    override fun onCaptureEvent() {
        //
    }

    override fun onImageRemove() {
        viewModel.setCurrentCroppedImageUri(null)
        viewModel.setCurrentImage(null)
        imageUrl = null
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearPostChanges()
    }
}