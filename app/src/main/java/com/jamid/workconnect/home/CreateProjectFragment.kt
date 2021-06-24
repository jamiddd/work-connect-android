package com.jamid.workconnect.home

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.FOCUS_DOWN
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentCreateProjectBinding
import com.jamid.workconnect.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateProjectFragment : SupportFragment(R.layout.fragment_create_project, TAG, false) {

    private lateinit var binding: FragmentCreateProjectBinding
    private var imageUri: String? = null
    private var positionFromBottom = 0
    private var prevLineCount = 0
    private var isInitialized = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreateProjectBinding.bind(view)

        binding.addProjectImageBtn.setOnClickListener {
            openImageSelectMenu()
        }

        binding.createProjectFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.primaryBottomSheetState.observe(viewLifecycleOwner) {
            if (isInitialized) {
                if (it == BottomSheetBehavior.STATE_HIDDEN) {
                    activity.onBackPressed()
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

//        OverScrollDecoratorHelper.setUpOverScroll(binding.createProjectScroll)

        val screenHeight = getWindowHeight()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.createProjectScroll.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(100))

            binding.createProjectFragmentToolbar.updateLayout(marginTop = top)

            val params = binding.cardView3.layoutParams as CoordinatorLayout.LayoutParams
            params.height = bottom + convertDpToPx(48)
            params.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
            binding.cardView3.layoutParams = params

//            val params1 = binding.bottomCardBlur.layoutParams as ViewGroup.LayoutParams
//            params1.height = bottom + convertDpToPx(48)
//            params1.width = ViewGroup.LayoutParams.MATCH_PARENT
//            binding.bottomCardBlur.layoutParams = params1

            if (binding.projectContentText.hasFocus()) {
                val child = binding.projectContentText
                val r = Rect()
                child.getGlobalVisibleRect(r)

                val p = child.selectionStart
                val layout = child.layout
                if (layout != null) {
                    val line = layout.getLineForOffset(p)
                    val baseline = layout.getLineBaseline(line)
                    val ascent = layout.getLineAscent(line)
//                    val cursorX = layout.getPrimaryHorizontal(p) + r.left
                    // with respect to global rect
                    val cursorY = baseline + ascent + r.top

                    positionFromBottom = screenHeight - (bottom + convertDpToPx(48))

                    if (cursorY > positionFromBottom) {
                        // when the cursor is below the keyboard
                        val diff = cursorY - positionFromBottom
                        binding.createProjectScroll.scrollY = binding.createProjectScroll.scrollY + diff
                    } else {
                        // when the cursor is above the keyboard
                    }

                }
            }
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
                binding.projectThumnailImg.setImageURI(it.toString())
                binding.addProjectImageBtn.visibility = View.GONE
                viewModel.uploadPostImage(it, PROJECT)
                viewModel.setCurrentImage(null)
            } else {
                val s: String? = null
                binding.projectThumnailImg.setImageURI(s)
                binding.addProjectImageBtn.visibility = View.VISIBLE
                binding.removeProjectImgBtn.visibility = View.GONE
                imageUri = s
            }
        }

        binding.projectThumnailImg.isClickable = true
        binding.projectThumnailImg.isLongClickable = true

        var removeMode = false
        var job: Job? = null

        binding.projectThumnailImg.setOnClickListener {
            if (removeMode) {
                removeMode = false
                job?.cancel()
                binding.removeProjectImgBtn.visibility = View.GONE
                binding.projectThumnailImg.colorFilter = null
            } else {
                openImageSelectMenu()
            }
        }

        /*ViewCompat.setOnApplyWindowInsetsListener(
            binding.cardView3
        ) { _: View?, insets: WindowInsetsCompat ->
            ViewCompat.onApplyWindowInsets(
                binding.cardView3,
                insets.replaceSystemWindowInsets(
                    insets.systemWindowInsetLeft, 0,
                    insets.systemWindowInsetRight, 0
                )
            )
        }*/

        binding.projectThumnailImg.setOnLongClickListener {
            if (viewModel.currentCroppedImageUri.value != null) {
                removeMode = true
                job = lifecycleScope.launch {
                    delay(5000)
                    removeMode = false
                    binding.removeProjectImgBtn.visibility = View.GONE
                    binding.projectThumnailImg.colorFilter = null
                }

                binding.projectThumnailImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.semiTransparentDark))
                binding.removeProjectImgBtn.visibility = View.VISIBLE
            } else {
                openImageSelectMenu()
                /*val fragment = ImageSelectFragment.newInstance()
                activity.showBottomSheet(fragment, ImageSelectFragment.TAG)*/
            }
            true
        }

        binding.removeProjectImgBtn.setOnClickListener {
            viewModel.setCurrentCroppedImageUri(null)
            binding.projectThumnailImg.colorFilter = null
        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.projectAdminPhoto.setImageURI(it.photo)
                val nameAndTime = it.name + " • " + SimpleDateFormat("hh:mm a", Locale.UK).format(Date())
                binding.projectAdminNameAndTime.text = nameAndTime
            }
        }
        
        viewModel.tag.observe(viewLifecycleOwner) {
            if (it != null) {
                addNewChip(it)
            }
        }

        viewModel.currentPlace.observe(viewLifecycleOwner) {
            if (it != null) {
                val t = binding.projectAdminNameAndTime.text.toString() + " • "
                binding.projectAdminNameAndTime.text = t
                binding.projectLocation.text = it
                val location = viewModel.currentLocation.value
                if (location != null) {
                    viewModel.setCurrentLocation(SimpleLocation(location.latitude, location.latitude, it))
                }
            } else {
                val user = viewModel.user.value
                if (user != null) {
                    val nameAndTime = user.name + " • " + SimpleDateFormat("hh:mm a", Locale.UK).format(Date())
                    binding.projectAdminNameAndTime.text = nameAndTime
                }
                binding.projectLocation.text = null
            }
        }

        binding.addProjectLocationBtn.setOnClickListener {
            activity.invokeLocationFragment()
        }

        binding.addProjectTagBtn.setOnClickListener {
            val fragment = TagFragment.newInstance()
            activity.showBottomSheet(fragment, TagFragment.TAG)
        }

        /*binding.projectContentText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.scrollView3.smoothScrollTo(0, binding.scrollView3.measuredHeight)
            }
        }*/

        binding.projectLocation.setOnClickListener {
            val instance = GenericDialogFragment.newInstance(REMOVING_LOCATION, "Remove location", "Are you sure you want to remove the given location for this project?", isActionOn = true, isCancelable = true)
            activity.showBottomSheet(instance, TAG)
        }

//        OverScrollDecoratorHelper.setUpOverScroll(binding.scrollView3)

        binding.projectContentText.doAfterTextChanged {
            if (prevLineCount != binding.projectContentText.lineCount) {
                adjustments()
            }
            prevLineCount = binding.projectContentText.lineCount
        }

        /*activity.mainBinding.primaryMenuBtn.setOnClickListener {
            // check if the title is empty, title is must
            if (binding.projectTitleText.text.isNullOrBlank()) return@setOnClickListener

            // check if both the image and content are empty, at least one needs to be there
            if (binding.projectContentText.text.isNullOrBlank()) return@setOnClickListener

            activity.showBottomSheet(GenericDialogFragment.newInstance(CREATING_PROJECT,"Creating Project", "Creating your project. Please wait for a while ... ", isProgressing = true), GenericDialogFragment.TAG)
*//*
            val dialog = MaterialAlertDialogBuilder(activity)
                .setView(R.layout.creating_project_progress_dialog)
                .setCancelable(false)
                .show()

            lifecycleScope.launch {
                delay(10000)
                dialog.dismiss()
            }*//*

            val title = binding.projectTitleText.text.trim().toString()
            val content =  binding.projectContentText.text.trim().toString()

            val tags = mutableListOf<String>()
            val links = emptyList<String>()

            for (ch in binding.tagsGroup.children) {
                val tag = (ch as Chip).text.toString()
                tags.add(tag)
            }

            val post = Post("", title, User(), "", content = content, type = PROJECT, thumbnail = imageUri, tags = tags)
            viewModel.uploadPost(post)
        }*/

    }

    private fun openImageSelectMenu() {
        hideKeyboard()
        val tag = SELECT_IMAGE_MENU_POST
        val item1 = GenericMenuItem(tag, "Select from gallery", R.drawable.ic_baseline_add_photo_alternate_24, 0)
        val item2 = GenericMenuItem(tag, "Take a photo", R.drawable.ic_baseline_camera_alt_24, 1)
        val item3 = GenericMenuItem(tag, "Remove image", R.drawable.ic_baseline_delete_24, 2)

        val fragment = GenericMenuFragment.newInstance(tag, "Add Image ...", arrayListOf(item1, item2, item3))
        activity.showBottomSheet(fragment, tag)
    }

    private fun adjustments() {
        val child = binding.projectContentText
        val r = Rect()
        child.getGlobalVisibleRect(r)

        Log.d("Editor", "Rect - " + r.toShortString())
        val p = child.selectionStart
        val layout = child.layout
        if (layout != null) {
            val line = layout.getLineForOffset(p)
            val baseline = layout.getLineBaseline(line)
            val ascent = layout.getLineAscent(line)
            val cursorY = baseline + ascent + r.top

            if (cursorY > positionFromBottom) {
                // when the cursor is below the keyboard
                Log.d("Editor", "cursorY - $cursorY, cursorNext - ${cursorY + convertDpToPx(50)}")
                binding.createProjectScroll.scrollTo(0, cursorY + convertDpToPx(50))
            } else {
                // when the cursor is above the keyboard
            }
        }

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

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_GET_IMAGE -> {
                    val image = data?.data
                    viewModel.setCurrentImage(image)

                    val bundle = Bundle().apply {
                        putInt("x", 4)
                        putInt("y", 3)
                        putInt("height", 300)
                        putInt("width", 400)
                        putString("shape", "RECTANGLE")
                    }
//                    findNavController().navigate(R.id.imageCropFragment, bundle)
                }
            }
        }
    }*/


    companion object {
        const val TITLE = "Create Project"
        const val TAG = "CreateProjectFragment"
        private const val REQUEST_GET_IMAGE = 12

        @JvmStatic
        fun newInstance() = CreateProjectFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearPostChanges()
    }
}