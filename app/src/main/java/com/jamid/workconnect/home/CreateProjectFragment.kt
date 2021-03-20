package com.jamid.workconnect.home

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.FOCUS_DOWN
import android.widget.Toast
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentCreateProjectBinding
import com.jamid.workconnect.interfaces.ImageSelectMenuListener
import com.jamid.workconnect.model.ObjectType
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleLocation
import java.text.SimpleDateFormat
import java.util.*

class CreateProjectFragment : Fragment(), ImageSelectMenuListener {

    private lateinit var binding: FragmentCreateProjectBinding
    private val viewModel: MainViewModel by activityViewModels()
    private var imageUri: String? = null
    private var dialogInterface: DialogInterface? = null
    private var positionFromBottom = 0
    private var prevLineCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_project, container, false)
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.create_project_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.create_project -> {
                // check if the title is empty, title is must
                if (binding.projectTitleText.text.isNullOrBlank()) return true

                // check if both the image and content are empty, at least one needs to be there
                if (binding.projectContentText.text.isNullOrBlank()) return true

                dialogInterface = MaterialAlertDialogBuilder(requireContext())
                    .setView(R.layout.creating_project_progress_dialog)
                    .setCancelable(false)
                    .show()

                val title = binding.projectTitleText.text.trim().toString()
                val content =  binding.projectContentText.text.trim().toString()

                val tags = mutableListOf<String>()
                val links = emptyList<String>()

                for (ch in binding.tagsGroup.children) {
                    val tag = (ch as Chip).text.toString()
                    tags.add(tag)
                }

                viewModel.upload(title, content, ObjectType.Project, image = imageUri, tags, links)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        binding.addProjectImageBtn.setOnClickListener {
            val fragment = ImageSelectFragment.newInstance()
            activity.showBottomSheet(fragment, ImageSelectFragment.TAG)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigateUp()
        }

        viewModel.postUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            dialogInterface?.dismiss()

            when (result) {
                is Result.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Project created successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), "Something went wrong - " + result.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.cardView3
        ) { v, insets ->
            ViewCompat.onApplyWindowInsets(
                binding.cardView3,
                insets.replaceSystemWindowInsets(
                    insets.systemWindowInsetLeft, 0,
                    insets.systemWindowInsetRight, 0
                )
            )
            insets
        }

        val screenHeight = getWindowHeight()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->

            binding.scrollView3.setPadding(0, top + convertDpToPx(56), 0, bottom + convertDpToPx(100))

            val params = binding.cardView3.layoutParams as ConstraintLayout.LayoutParams
            params.height = bottom + convertDpToPx(48)
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.bottomToBottom = binding.createProjectRoot.id
            binding.cardView3.layoutParams = params

            val params1 = binding.bottomCardBlur.layoutParams as ViewGroup.LayoutParams
            params1.height = bottom + convertDpToPx(48)
            params1.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.bottomCardBlur.layoutParams = params1

            if (binding.projectContentText.hasFocus()) {
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
//                    val cursorX = layout.getPrimaryHorizontal(p) + r.left
                    // with respect to global rect
                    val cursorY = baseline + ascent + r.top

                    positionFromBottom = screenHeight - (bottom + convertDpToPx(48))
                    Log.d("Editor", "($cursorY, $positionFromBottom)")

                    if (cursorY > positionFromBottom) {
                        // when the cursor is below the keyboard
                        val diff = cursorY - positionFromBottom
                        binding.scrollView3.scrollY = binding.scrollView3.scrollY + diff
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
                    imageUri = result.data
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
                val path = it.path
                viewModel.uploadPostImage(path, ObjectType.Project)
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

        binding.projectThumnailImg.setOnClickListener {
            val fragment = ImageSelectFragment.newInstance()
            activity.showBottomSheet(fragment, ImageSelectFragment.TAG)
        }

        binding.projectThumnailImg.setOnLongClickListener {
            if (viewModel.currentCroppedImageUri.value != null) {
                binding.projectThumnailImg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.semiTransparentDark))
                binding.removeProjectImgBtn.visibility = View.VISIBLE
            } else {
                val fragment = ImageSelectFragment.newInstance()
                activity.showBottomSheet(fragment, ImageSelectFragment.TAG)
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

        binding.projectLocation.isClickable = true
        binding.projectLocation.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove location")
            .setMessage("Are you sure you want to remove the given location for this project?")
                .setPositiveButton("Delete") { d, w ->
                    viewModel.setCurrentLocation(null)
                    viewModel.setCurrentPlace(null)
                    d.dismiss()
                }
                .setNegativeButton("Cancel") { d, w ->
                    d.dismiss()
                }
                .show()
        }

//        OverScrollDecoratorHelper.setUpOverScroll(binding.scrollView3)

        binding.projectContentText.doAfterTextChanged {
            if (prevLineCount != binding.projectContentText.lineCount) {
                adjustments()
            }
            prevLineCount = binding.projectContentText.lineCount
        }

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
                binding.scrollView3.scrollTo(0, cursorY + convertDpToPx(50))
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

        binding.scrollView3.fullScroll(FOCUS_DOWN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
                    findNavController().navigate(R.id.imageCropFragment, bundle)
                }
            }
        }
    }


    companion object {

        private const val REQUEST_GET_IMAGE = 12

        @JvmStatic
        fun newInstance() = CreateProjectFragment()
    }

    override fun onSelectImageFromGallery() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(intent, REQUEST_GET_IMAGE)
    }

    override fun onCaptureEvent() {

    }

    override fun onImageRemove() {
        viewModel.setCurrentCroppedImageUri(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearPostChanges()
    }
}