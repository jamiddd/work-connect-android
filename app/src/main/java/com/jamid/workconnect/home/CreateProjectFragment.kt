package com.jamid.workconnect.home

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.FOCUS_DOWN
import android.widget.EditText
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentCreateProjectBinding
import com.jamid.workconnect.model.*
import java.text.SimpleDateFormat
import java.util.*

class CreateProjectFragment : SupportFragment(R.layout.fragment_create_project, TAG, false) {

    private lateinit var binding: FragmentCreateProjectBinding
    private var imageUri: String? = null
    private var isInitialized = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreateProjectBinding.bind(view)

        showKeyboard()

        binding.addImageBtn.setOnClickListener {
            openImageSelectMenu()
        }

        binding.createProjectFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.primaryBottomSheetState.observe(viewLifecycleOwner) {
            if (isInitialized) {
                if (it == BottomSheetBehavior.STATE_HIDDEN) {
                    findNavController().navigateUp()
                }
            }
        }

        viewModel.postUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            isInitialized = true
            activity.hideBottomSheet()

            when (result) {
                is Result.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Project created successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), "Something went wrong - " + result.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.createProjectScroll.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(100))
            binding.createProjectFragmentToolbar.updateLayout(marginTop = top)

            if (binding.projectContentText.hasFocus()) {
                checkCursorVisibility(binding.projectContentText)
            }

            val params = binding.bottomActions.layoutParams as CoordinatorLayout.LayoutParams
            params.height = bottom + convertDpToPx(48)
            params.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
            binding.bottomActions.layoutParams = params

        }

        binding.projectContentText.setOnClickListener {
            checkCursorVisibility(binding.projectContentText)
        }

        viewModel.postPhotoUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            when (result) {
                is Result.Success -> {
                    imageUri = result.data.toString()
                }
                is Result.Error -> {
                    imageUri = null
                    Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.projectImg.visibility = View.VISIBLE
                Glide.with(activity)
                    .load(it)
                    .into(binding.projectImg)

                viewModel.uploadPostImage(it, PROJECT)
                viewModel.setCurrentImage(null)
            } else {
                val s: String? = null
                binding.projectImg.visibility = View.GONE
                binding.projectImg.setImageDrawable(null)
                imageUri = s
            }
        }

        binding.projectImg.setOnClickListener {
            openRemoveImageMenu()
        }

        binding.projectImg.setOnLongClickListener {
            openRemoveImageMenu()
            true
        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.adminImg.setImageURI(it.photo)
                val nameAndTime = it.name + " • " + SimpleDateFormat("hh:mm a", Locale.UK).format(Date())
                binding.adminInfo.text = nameAndTime
            }
        }
        
        viewModel.tag.observe(viewLifecycleOwner) {
            if (it != null) {
                addNewChip(it)
            }
        }

        viewModel.currentPlace.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.projectLocation.text = it
                binding.projectLocation.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_location_on_24, 0, 0, 0)

                val location = viewModel.currentLocation.value
                if (location != null) {
                    viewModel.setCurrentLocation(SimpleLocation(location.latitude, location.latitude, it))
                }
            } else {
                binding.projectLocation.text = getString(R.string.add_location)
                binding.projectLocation.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_baseline_add_location_alt_24, 0, 0, 0)
            }
        }

        binding.addProjectLocationBtn.setOnClickListener {
            activity.invokeLocationFragment()
        }

        binding.addProjectTagBtn.setOnClickListener {
            val fragment = TagFragment.newInstance()
            activity.showBottomSheet(fragment, TagFragment.TAG)
        }

        binding.createProjectBtn.setOnClickListener {
            if (!binding.projectTitleText.text.isNullOrBlank() && !binding.projectContentText.text.isNullOrBlank() && imageUri != null) {
                val tags = mutableListOf<String>()

                for (child in binding.tagsGroup.children) {
                    tags.add((child as Chip).text.toString())
                }

                val postTitle = binding.projectTitleText.text.toString()
                val postContent = binding.projectContentText.text.toString()
                val image = imageUri.toString()

                val currentUser = viewModel.user.value
                if (currentUser != null) {
                    val post = Post("", postTitle, currentUser, currentUser.id, "", content = postContent, thumbnail = image, tags = tags)
                    viewModel.uploadPost(post)

                    val instance = GenericDialogFragment.newInstance(CREATING_PROJECT, "Creating Project", "Creating your project. Please wait ...", isCancelable = false, isProgressing = true)
                    activity.showBottomSheet(instance, TAG)
                }
            } else {
                if (binding.projectTitleText.text.isNullOrBlank()) {
                    Toast.makeText(activity, "Title cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (binding.projectContentText.text.isNullOrBlank()) {
                    Toast.makeText(activity, "Content cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (imageUri == null) {
                    Toast.makeText(activity, "Must contain an image.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
        }

        binding.projectLocation.setOnClickListener {
            if (viewModel.currentLocation.value != null) {
                val instance = GenericDialogFragment.newInstance(REMOVING_LOCATION, "Remove location", "Are you sure you want to remove the given location for this project?", isActionOn = true, isCancelable = true)
                activity.showBottomSheet(instance, TAG)
            } else {
                activity.invokeLocationFragment()
            }
        }

        binding.projectContentText.doAfterTextChanged {
            if (!it.isNullOrEmpty()) {
                checkCursorVisibility(binding.projectContentText)
            }
        }

    }

    private fun checkCursorVisibility(child: EditText) {
        val threshold =
            calculateThreshold(viewModel.windowInsets.value!!.second + convertDpToPx(80))
        Log.d(BUG_TAG, "Threshold $threshold")

        // get cursor position
        val cursorPosition = findCursorPositionOnScreen(child)
        Log.d(BUG_TAG, "Cursor position - $cursorPosition")
        if (cursorPosition > threshold) {
            binding.createProjectScroll.scrollY =
                binding.createProjectScroll.scrollY + (cursorPosition - threshold) + convertDpToPx(8)
            Log.d(BUG_TAG, "The keyboard is hiding the cursor - ${cursorPosition - threshold}")
        } else {
            Log.d(BUG_TAG, "The keyboard is not hiding the cursor - ${threshold - cursorPosition}")
        }
    }

    private fun openImageSelectMenu() {
        hideKeyboard()
        val tag = SELECT_IMAGE_MENU_POST
        val item1 = GenericMenuItem(tag, "Select from gallery", R.drawable.ic_baseline_add_photo_alternate_24, 0)
        val item2 = GenericMenuItem(tag, "Take a photo", R.drawable.ic_baseline_camera_alt_24, 1)

        val fragment = GenericMenuFragment.newInstance(tag, "Add Image ...", arrayListOf(item1, item2))
        activity.showBottomSheet(fragment, tag)
    }

    private fun openRemoveImageMenu() {
        hideKeyboard()
        val tag = REMOVE_IMAGE_MENU
        val item1 = GenericMenuItem(tag, "Remove image", R.drawable.ic_baseline_delete_24, 0)

        val fragment = GenericMenuFragment.newInstance(tag, "", arrayListOf(item1))
        fragment.shouldHideTitle = true
        activity.showBottomSheet(fragment, tag)
    }

    private fun addNewChip(s: String) {
        val chip = Chip(requireContext())
        chip.text = s
        chip.isCloseIconVisible = true
        chip.setCloseIconResource(R.drawable.ic_baseline_close_24)
        binding.tagsGroup.addView(chip)

        chip.setOnCloseIconClickListener {
            binding.tagsGroup.removeView(chip)
        }

        binding.createProjectScroll.fullScroll(FOCUS_DOWN)
    }

    private fun calculateThreshold(bottomInset: Int): Int {
        val totalHeight = binding.createProjectScroll.measuredHeight
        val partialTop = binding.createProjectScroll.scrollY
        val screenHeight = getFullScreenHeight()
        val usableScreenHeight = screenHeight - bottomInset - binding.createProjectFragmentAppBar.measuredHeight
        val partialBottom = totalHeight - (partialTop + usableScreenHeight + bottomInset)
        return totalHeight - (partialBottom + bottomInset)
    }

    private fun findCursorPositionOnScreen(e: EditText): Int {
        /*fun getHeightForMultipleViews(
            container: ViewGroup,
            inclusiveStartIndex: Int = 0,
            exclusiveEndIndex: Int
        ): Int {
            var totalHeight = 0
            for (i in inclusiveStartIndex until exclusiveEndIndex) {
                totalHeight += container.getChildAt(i).measuredHeight
            }
            return totalHeight
        }*/

        fun getHeightForMultipleViews(container: ViewGroup, exclusiveEndIndex: Int): Int {
            var totalHeight = 0
            for (i in 0 until exclusiveEndIndex) {
                totalHeight += container.getChildAt(i).measuredHeight
            }
            return totalHeight
        }
        val constantHeight = getHeightForMultipleViews(
            binding.itemsContainer,
            3
        )
        val textLayout = e.layout ?: return 0
        val line = textLayout.getLineForOffset(e.selectionStart)

        return constantHeight + maxOf(textLayout.getLineBaseline(line), convertDpToPx(48))
    }


    companion object {
        const val TITLE = "Create Project"
        const val TAG = "CreateProjectFragment"

        @JvmStatic
        fun newInstance() = CreateProjectFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearPostChanges()
    }
}