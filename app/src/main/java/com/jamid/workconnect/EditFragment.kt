package com.jamid.workconnect

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.workconnect.databinding.FragmentEditBinding
import com.jamid.workconnect.model.Result


class EditFragment : Fragment(R.layout.fragment_edit) {

    private lateinit var binding: FragmentEditBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var imageUri: String? = null
    private val checkChangeListener = MutableLiveData<List<Int>>()
    private var dialog: DialogInterface? = null
    private var usernameExists = false
    private var hasMadeChanges = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X,   true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X,  false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save_user_data -> {
               /* if (!hasMadeChanges) {
                    findNavController().navigateUp()
                    return true
                }

                if (binding.fullNameTextLayout.editText?.text.isNullOrBlank()) {
                    binding.fullNameTextLayout.isErrorEnabled = true
                    binding.fullNameTextLayout.error = "Name cannot be empty."
                    return true
                }

                if (binding.usernameTextLayout.editText?.text.isNullOrBlank()) {
                    binding.usernameTextLayout.isErrorEnabled = true
                    binding.usernameTextLayout.error = "Username cannot be empty"
                    return true
                }

                if (usernameExists) return true

                val fullName = binding.fullNameTextLayout.editText?.text?.trim().toString()
                val username = binding.usernameTextLayout.editText?.text?.trim().toString()

                val interests = mutableListOf<String>()

                for (child in binding.interestsGroup.children) {
                    val chip = child as Chip
                    if (chip.isChecked) {
                        interests.add(chip.text.toString())
                    }
                }

                val about = binding.userAboutText.text.toString()

                val dialogInterface = MaterialAlertDialogBuilder(requireContext())
                    .setView(R.layout.updating_user_process_dialog)
                    .setCancelable(false)
                    .show()

                dialog = dialogInterface
                viewModel.updateUser(fullName, username, about, interests, imageUri)*/


                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentEditBinding.bind(view)
        val behavior = BottomSheetBehavior.from(binding.addInterestLayout)

        behavior.peekHeight = 0
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_HIDDEN

        var initialBottom = 0

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.scrollView4.setPadding(0, top + convertDpToPx(56), 0, bottom + convertDpToPx(56))

            if (initialBottom == 0) {
                initialBottom = bottom
            }

            val params = binding.editBottomBlur.layoutParams as ViewGroup.LayoutParams
            params.height = bottom + convertDpToPx(56)
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.editBottomBlur.layoutParams = params

            /*if (bottom > initialBottom  && (binding.userAboutTextLayout.editText?.hasFocus() == true || binding.interestEditText.hasFocus())) {
                binding.scrollView4.fullScroll(FOCUS_DOWN)
                if (binding.userAboutTextLayout.editText?.hasFocus() == true) {
                    binding.userAboutTextLayout.editText?.requestFocus()
                }

                if (binding.interestEditText.hasFocus()) {

                }
            }

            if (bottom > initialBottom && !binding.interestEditText.hasFocus()) {
                behavior.state = BottomSheetBehavior.STATE_HIDDEN
            }*/

            if (binding.userAboutTextLayout.editText?.hasFocus() == true || binding.interestEditText.hasFocus()) {
                binding.scrollView4.smoothScrollTo(0, binding.scrollView4.scrollY + bottom)
            }

            if (bottom > initialBottom) {
                if (!binding.interestEditText.hasFocus()) {
                    behavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }

        }

        binding.scrollView4.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

//        OverScrollDecoratorHelper.setUpOverScroll(binding.scrollView4)

        val activity = requireActivity() as MainActivity

        val user = viewModel.user.value

        if (user != null) {
            binding.userChangeImg.setImageURI(user.photo)
            binding.userChangeImgLarge.setImageURI(user.photo)
            binding.fullNameTextLayout.editText?.setText(user.name)
            binding.usernameTextLayout.editText?.setText(user.username)
            binding.userAboutTextLayout.editText?.setText(user.about)

            imageUri = user.photo

            for (interest in user.interests) {
                addChip(interest, activity, false)
            }
        }

        binding.userChangeImg.isClickable = true
        binding.userChangeImg.setOnClickListener {
            val fragment = ImageSelectFragment.newInstance()
            activity.showBottomSheet(fragment, ImageSelectFragment.TAG)
        }

        binding.addInterestLayoutBtn.setOnClickListener {
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }


        viewModel.updateUserResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            dialog!!.dismiss()

            when (result) {
                is Result.Success -> {
//                    val changes = result.data
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Something went wrong - ${result.exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        /*ViewCompat.setOnApplyWindowInsetsListener(
            binding.editScroller
        ) { v: View?, insets: WindowInsetsCompat ->
            ViewCompat.onApplyWindowInsets(
                binding.editScroller,
                insets.replaceSystemWindowInsets(
                    insets.systemWindowInsetLeft, 0,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                )
            )
        }*/

        viewModel.profilePhotoUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            binding.profilePhotoUploadProgress.visibility = View.GONE
            binding.userChangeImg.colorFilter = null

            when (result) {
                is Result.Success -> {
                    imageUri = result.data.toString()
                }
                is Result.Error -> {
                    imageUri = null
                    Toast.makeText(activity, result.exception.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.userChangeImg.setImageURI(it.toString())
                val path = it.path
                binding.profilePhotoUploadProgress.visibility = View.VISIBLE
                binding.userChangeImg.setColorFilter(ContextCompat.getColor(activity, R.color.semiTransparentDark))
                viewModel.uploadProfilePhoto(path)
            } else {
                if (hasMadeChanges) {
                    val s: String? = null
                    binding.userChangeImg.setImageURI(s)
                    imageUri = s
                }
            }
        }


        viewModel.userNameExists.observe(viewLifecycleOwner) { exists ->
            binding.usernameProgress.visibility = View.GONE

            usernameExists = exists ?: return@observe

            if (usernameExists) {
                binding.usernameTextLayout.isErrorEnabled = true
                binding.usernameTextLayout.error = "Username already exists."
            } else {
                binding.usernameTextLayout.isErrorEnabled = false
                binding.usernameTextLayout.error = null
            }
        }

        binding.fullNameTextLayout.editText?.doAfterTextChanged {
            binding.usernameTextLayout.isErrorEnabled = false
            binding.usernameTextLayout.error = null
            hasMadeChanges = true
        }

        binding.usernameTextLayout.editText?.doAfterTextChanged {
            if (!it.isNullOrBlank()) {
                viewModel.checkIfUsernameExists(it.toString())
                binding.usernameProgress.visibility = View.VISIBLE
            }
            binding.usernameTextLayout.isErrorEnabled = false
            binding.usernameTextLayout.error = null
            hasMadeChanges = true
        }

        binding.userAboutTextLayout.editText?.doAfterTextChanged {
            hasMadeChanges = true
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
            hasMadeChanges = true
        }

        chip.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                chip.toPrimary(context)
            } else {
                chip.toSecondary(context)
            }
            hasMadeChanges = true
            checkChangeListener.postValue(binding.interestsGroup.checkedChipIds)
        }

        binding.interestsGroup.addView(chip)
    }

    private fun Chip.toPrimary(context: Context) {
        closeIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
        chipBackgroundColor =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.blue_500))
        setTextColor(ContextCompat.getColor(context, R.color.white))
    }

    private fun Chip.toSecondary(context: Context) {
        closeIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
        chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#e3e3e3"))
        setTextColor(ContextCompat.getColor(context, R.color.black))
    }

    companion object {

        const val TAG = "EditFragment"
        private const val REQUEST_GET_IMAGE = 12

        @JvmStatic
        fun newInstance() = EditFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearEditChanges()
    }


}