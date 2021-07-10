package com.jamid.workconnect.auth

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentUserDetailBinding
import com.jamid.workconnect.model.GenericMenuItem
import com.jamid.workconnect.model.Result

class UserDetailFragment : SupportFragment(R.layout.fragment_user_detail) {

    private lateinit var binding: FragmentUserDetailBinding
    private var imageUrl: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentUserDetailBinding.bind(view)

        binding.addImgBtn.setOnClickListener {
            showMenu()
        }

        binding.userImg.setColorFilter(ContextCompat.getColor(activity, R.color.semiTransparentDark))

        binding.userImg.isClickable = true
        binding.userImg.colorFilter
        binding.userImg.setOnClickListener {
            showMenu()
        }

        setListeners()

        binding.userFullNameText.doAfterTextChanged {
            binding.updateUserBtn.isEnabled = !it.isNullOrBlank() && it.length > 3
        }

        val currentUser = Firebase.auth.currentUser

        if (currentUser != null) {
            currentUser.photoUrl?.let {
                binding.userImg.setImageURI(it.toString())
                imageUrl = it
            }
        }

        binding.updateUserBtn.setOnClickListener {
            hideKeyboard()
            viewModel.updateFirebaseUser(imageUrl, binding.userFullNameText.text?.trimEnd().toString())
//            activity.toFragment(InterestFragment.newInstance(true), InterestFragment.TAG)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.userDetailToolbar.updateLayout(marginTop = top)
        }

    }

    private fun setListeners() {

        viewModel.profilePhotoUploadResult.observe(viewLifecycleOwner) {
            binding.imageUploadProgress.visibility = View.GONE
            binding.userImg.colorFilter = null
            imageUrl = it
        }

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.addImgBtn.visibility = View.GONE
                binding.userImg.setImageURI(it.toString())

                binding.imageUploadProgress.visibility = View.VISIBLE
                binding.userImg.setColorFilter(ContextCompat.getColor(activity, R.color.semiTransparentDark))
                viewModel.uploadProfilePhoto(it)
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
                    findNavController().navigate(R.id.interestFragment, Bundle().apply { putBoolean(InterestFragment.ARG_IS_SIGN_IN, true) })
//                    activity.toFragment(InterestFragment.newInstance(true), InterestFragment.TAG)
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), result.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showMenu() {

        hideKeyboard()

        val tag = SELECT_IMAGE_MENU_USER
        val menu = GenericMenuFragment.newInstance(tag, "Upload or Remove Image", arrayListOf(
            GenericMenuItem(tag, "Select Image", R.drawable.ic_baseline_add_photo_alternate_24, 0),
            GenericMenuItem(tag, "Take a Photo", R.drawable.ic_baseline_camera_alt_24, 1),
            GenericMenuItem(tag, "Remove Image", R.drawable.ic_baseline_delete_24, 2)
        ))

        activity.showBottomSheet(menu, tag)
    }

    companion object {

        const val TAG = "UserDetailFragment"
        const val TITLE = "User Detail"

        @JvmStatic
        fun newInstance() = UserDetailFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearPostChanges()
    }
}