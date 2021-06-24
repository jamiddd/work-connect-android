package com.jamid.workconnect

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.jamid.workconnect.databinding.FragmentEditBinding
import com.jamid.workconnect.model.GenericMenuItem
import com.jamid.workconnect.model.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class EditFragment : SupportFragment(R.layout.fragment_edit, TAG, false) {

    private lateinit var binding: FragmentEditBinding
    private var imageUri: String? = null
    private val checkChangeListener = MutableLiveData<List<Int>>()
    private var usernameExists = false
    private var hasChangedImage = false
    private var canRemoveImage = false
    private var isInitialized = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentEditBinding.bind(view)
        val behavior = BottomSheetBehavior.from(binding.addInterestLayout)

        binding.editFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        var initialBottom = 0

        viewModel.primaryBottomSheetState.observe(viewLifecycleOwner) {
            if (isInitialized) {
                when (it) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        findNavController().navigateUp()
                    }
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.editScroll.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(56))

            binding.editFragmentToolbar.updateLayout(marginTop = top)

            if (initialBottom == 0) {
                initialBottom = bottom
            }

            /*val params = binding.editBottomBlur.layoutParams as ViewGroup.LayoutParams
            params.height = bottom + convertDpToPx(56)
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.editBottomBlur.layoutParams = params*/

            binding.addInterestLayout.setContentPadding(0, 0, 0, bottom)

            if (binding.userAboutTextLayout.editText?.hasFocus() == true || binding.interestEditText.hasFocus()) {
                binding.editScroll.smoothScrollTo(0, binding.editScroll.scrollY + bottom)
            }

            if (bottom > initialBottom) {
                if (!binding.interestEditText.hasFocus()) {
                    behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }

        viewModel.updateUserResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            isInitialized = true
            activity.hideBottomSheet()

            when (result) {
                is Result.Success -> {
                    viewModel.clearEditChanges()
                }
                is Result.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Something went wrong while updating user - ${result.exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


        binding.editScroll.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        val user = viewModel.user.value

        if (user != null) {
            binding.userChangeImg.setImageURI(user.photo)
            binding.fullNameTextLayout.editText?.setText(user.name)
            binding.usernameTextLayout.editText?.setText(user.username)
            binding.userAboutTextLayout.editText?.setText(user.about)

            imageUri = user.photo

            for (interest in user.userPrivate.interests) {
                addChip(interest, activity, false)
            }
        }

        binding.userChangeImg.isClickable = true
        binding.userChangeImg.setOnClickListener {
            openImageSelectMenu()
        }

        binding.addInterestLayoutBtn.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }


        viewModel.profilePhotoUploadResult.observe(viewLifecycleOwner) {
            binding.profilePhotoUploadProgress.visibility = View.GONE
            binding.userChangeImg.colorFilter = null
            imageUri = it?.toString()
        }

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                hasChangedImage = true
                binding.userChangeImg.setImageURI(it.toString())

                binding.profilePhotoUploadProgress.visibility = View.VISIBLE
                binding.userChangeImg.setColorFilter(ContextCompat.getColor(activity, R.color.semiTransparentDark))
                viewModel.uploadProfilePhoto(it)
            } else {
                if (canRemoveImage) {
                    val s: String? = null
                    binding.userChangeImg.setImageURI(s)
                    imageUri = s
                }
                canRemoveImage = true
            }
        }

        viewModel.userNameExists.observe(viewLifecycleOwner) { exists ->
            binding.usernameProgress.visibility = View.GONE

            val usernameExistsResult = exists ?: return@observe

            if (usernameExistsResult is Result.Success) {
                usernameExists = usernameExistsResult.data
                if (usernameExists) {
                    binding.usernameTextLayout.isErrorEnabled = true
                    binding.usernameTextLayout.error = "Username already exists."
                } else {
                    binding.usernameTextLayout.isErrorEnabled = false
                    binding.usernameTextLayout.error = null
                }
            } else if (usernameExistsResult is Result.Error){
                usernameExists = false
                binding.usernameTextLayout.isErrorEnabled = false
                binding.usernameTextLayout.error = null
                Log.d(TAG, usernameExistsResult.exception.localizedMessage)
            }
        }

        binding.fullNameTextLayout.editText?.doAfterTextChanged {
            binding.usernameTextLayout.isErrorEnabled = false
            binding.usernameTextLayout.error = null
        }

        binding.usernameTextLayout.editText?.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                viewModel.checkIfUsernameExists(it.toString())
                binding.usernameProgress.visibility = View.VISIBLE
            }
            binding.usernameTextLayout.isErrorEnabled = false
            binding.usernameTextLayout.error = null
        }

        binding.userAboutTextLayout.editText?.doAfterTextChanged {

        }

        binding.addInterestBtn.setOnClickListener {
            val tag = binding.interestEditText.text.toString()
            addChip(tag, activity, false)
            binding.interestEditText.text.clear()
        }

        binding.interestEditText.doAfterTextChanged {
            binding.addInterestBtn.isEnabled = !binding.interestEditText.text.isNullOrBlank()
        }

        binding.interestEditText.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    val t = binding.interestEditText.text
                    if (!t.isNullOrBlank()) {
                        addChip(t.toString(), activity, false)
                        binding.interestEditText.text.clear()
                    }
                }
            }
            true
        }

        binding.saveEditBtn.setOnClickListener {
            val interests = mutableListOf<String>()
            for (child in binding.interestsGroup.children) {
                if (child is Chip && child.isChecked) {
                    interests.add(child.text.toString())
                }
            }

            val image = if (hasChangedImage) {
                imageUri
            } else {
                user?.photo
            }

            val instance = GenericDialogFragment.newInstance(UPDATE_USER, "Updating profile ...", "Please wait while your profile is updating.", isCancelable = false, isProgressing = true, isActionOn = false)
            activity.showBottomSheet(instance, UPDATE_USER)

            val changes = mutableMapOf(
                NAME to binding.fullNameTextLayout.editText?.text?.trim().toString(),
                USERNAME to binding.usernameTextLayout.editText?.text?.trim().toString(),
                PHOTO to image,
                ABOUT to binding.userAboutTextLayout.editText?.text?.trim().toString(),
                INTERESTS to interests
            )
            viewModel.updateUser(changes)
        }

    }

    private fun openImageSelectMenu() {
        val tag = SELECT_IMAGE_MENU_USER
        val item1 = GenericMenuItem(tag, "Select from gallery", R.drawable.ic_baseline_add_photo_alternate_24, 0)
        val item2 = GenericMenuItem(tag, "Take a photo", R.drawable.ic_baseline_camera_alt_24, 1)
        val item3 = GenericMenuItem(tag, "Remove image", R.drawable.ic_baseline_delete_24, 2)

        val fragment = GenericMenuFragment.newInstance(tag, "Add Image ...", arrayListOf(item1, item2, item3))
        activity.showBottomSheet(fragment, tag)
    }

    private fun addChip(interest: String, context: Activity, initial: Boolean = true) {
        val chip = Chip(context)
        chip.text = interest
        chip.isCloseIconVisible = true
        chip.isCheckedIconVisible = true
        chip.closeIcon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_close_24)
        chip.isCheckable = true

        if (initial) {
            chip.isChecked = false
            chip.toSecondary(context)
        } else {
            chip.isChecked = true
            chip.toPrimary(context)
        }

        chip.setOnCloseIconClickListener {
            binding.interestsGroup.removeView(chip)
        }

        chip.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                chip.toPrimary(context)
            } else {
                chip.toSecondary(context)
            }
            checkChangeListener.postValue(binding.interestsGroup.checkedChipIds)
        }

        binding.interestsGroup.addView(chip)
    }

    private fun Chip.toPrimary(context: Context) {
        closeIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))

        chipBackgroundColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple_200))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.blue_500))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.purple_200))
            } else {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.blue_500))
            }
        }

        setTextColor(ContextCompat.getColor(context, R.color.white))
    }

    private fun Chip.toSecondary(context: Context) {
        closeIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
        chipBackgroundColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.darkerGrey))
            } else {
                ColorStateList.valueOf(Color.parseColor("#e3e3e3"))
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.darkerGrey))
            } else {
                ColorStateList.valueOf(Color.parseColor("#e3e3e3"))
            }
        }
        setTextColor(ContextCompat.getColor(context, R.color.black))
    }

    companion object {

        const val TAG = "EditFragment"
        const val TITLE = "Edit Profile"
        private const val REQUEST_GET_IMAGE = 12

        @JvmStatic
        fun newInstance() = EditFragment()
    }

}