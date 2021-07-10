package com.jamid.workconnect.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.FOCUS_DOWN
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.tabs.TabLayout
import com.jamid.workconnect.*
import com.jamid.workconnect.adapter.paging3.GenericComparator2
import com.jamid.workconnect.databinding.FragmentCreateProjectBinding
import com.jamid.workconnect.model.*
import com.jamid.workconnect.views.zoomable.ImageViewFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateProjectFragment : SupportFragment(R.layout.fragment_create_project) {

    private lateinit var binding: FragmentCreateProjectBinding
    private var isInitialized = false
    private val imagesAdapter = ImagesAdapter()
    private var lastImageBeforeCrop: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCreateProjectBinding.bind(view)

        showKeyboard()
        val title = arguments?.getString(ARG_TITLE) ?: return
        binding.createProjectFragmentToolbar.title = title

        binding.addImageBtn.setOnClickListener {
            lastImageBeforeCrop = null
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

        /*viewModel.postPhotoUploadResult.observe(viewLifecycleOwner) {
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
        }*/



        val helper = LinearSnapHelper()
        binding.projectImages.apply {
            onFlingListener = null
            adapter = imagesAdapter
            helper.attachToRecyclerView(this)
            layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        }


        binding.imagesTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.projectImages.smoothScrollToPosition(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })


        viewModel.projectImages.observe(viewLifecycleOwner) {
            if (!it.isNullOrEmpty()) {

                binding.imagesContainer.visibility = View.VISIBLE

                if (it.size <= 1) {
                    binding.imagesTabLayout.visibility = View.GONE
                } else {
                    binding.imagesTabLayout.visibility = View.VISIBLE
                }

                binding.imagesTabLayout.removeAllTabs()

                for (i in it.indices) {
                    val tab = binding.imagesTabLayout.newTab()
                    binding.imagesTabLayout.addTab(tab)
                }

                imagesAdapter.submitList(it)
            } else {
                binding.imagesContainer.visibility = View.GONE
            }
        }

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                if (lastImageBeforeCrop != null) {
                    viewModel.removeProjectImage(lastImageBeforeCrop!!)
                    lastImageBeforeCrop = null
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(500)
                    viewModel.addProjectImages(it)
                }
            }
        }

        /*binding.projectImg.setOnClickListener {
            openRemoveImageMenu()
        }*/

        /*binding.projectImg.setOnLongClickListener {
            openRemoveImageMenu()
            true
        }*/

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.adminImg.setImageURI(it.photo)
                binding.adminInfo.text = it.name
                binding.timeText.text = SimpleDateFormat("hh:mm a", Locale.UK).format(Date())
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
            /*if (!binding.projectContentText.text.isNullOrBlank()) {
                val tags = mutableListOf<String>()

                for (child in binding.tagsGroup.children) {
                    tags.add((child as Chip).text.toString())
                }

                val postContent = binding.projectContentText.text.toString()

                val currentUser = viewModel.user.value
                if (currentUser != null) {

                    val finalImages = mutableListOf<String>()
                    for (img in uploadedImages) {
                        finalImages.add(img.toString())
                    }

                    val post = Post("", title, currentUser, currentUser.id, "", content = postContent, images = finalImages, tags = tags)
                    viewModel.uploadPost(post)

                    val instance = GenericDialogFragment.newInstance(CREATING_PROJECT, "Creating Project", "Creating your project. Please wait ...", isCancelable = false, isProgressing = true)
                    activity.showBottomSheet(instance, TAG)
                }
            } else {
                if (binding.projectContentText.text.isNullOrBlank()) {
                    Toast.makeText(activity, "Content cannot be empty.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }*/
        }

        binding.projectLocation.setOnClickListener {
            if (!viewModel.currentPlace.value.isNullOrBlank()) {
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

        binding.projectImages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val position = (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                    binding.imagesTabLayout.getTabAt(position)?.select()
                }
            }
        })
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
        val tag = SELECT_IMAGE_MENU_PROJECT
        val item1 = GenericMenuItem(tag, "Select from gallery", R.drawable.ic_round_add_photo_alternate_24, 0)
        val item2 = GenericMenuItem(tag, "Take a photo", R.drawable.ic_round_camera_alt_24, 1)
        val item3 = GenericMenuItem(tag, "Remove all images", R.drawable.ic_round_clear_all_24, 2)

        val fragment = GenericMenuFragment.newInstance(tag, "Add Image ...", arrayListOf(item1, item2, item3))
        activity.showBottomSheet(fragment, tag)
    }

    /*private fun openRemoveImageMenu() {
        hideKeyboard()
        val tag = REMOVE_IMAGE_MENU
        val item1 = GenericMenuItem(tag, "Remove image", R.drawable.ic_baseline_delete_24, 0)

        val fragment = GenericMenuFragment.newInstance(tag, "", arrayListOf(item1))
        fragment.shouldHideTitle = true
        activity.showBottomSheet(fragment, tag)
    }*/

    private fun addNewChip(s: String) {
        val chip = Chip(requireContext())
        chip.text = "#${s.filter { !it.isWhitespace() }}"
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
        const val ARG_TITLE = "Create Project"
        const val TAG = "CreateProjectFragment"

        @JvmStatic
        fun newInstance(title: String) = CreateProjectFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TITLE, title)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearPostChanges()
    }


    inner class ImagesAdapter : ListAdapter<Uri, ImagesAdapter.ImageViewHolder>(GenericComparator2(Uri::class.java)) {

        inner class ImageViewHolder(val p: View): RecyclerView.ViewHolder(p) {
            fun bind(image: Uri, position: Int) {
                val actionBtn = p.findViewById<MaterialButton>(R.id.primary_action)
                val imageView = p.findViewById<ImageView>(R.id.image)
                val progressBar = p.findViewById<ProgressBar>(R.id.image_layout_progress)
                val fullscreenBtn = p.findViewById<MaterialButton>(R.id.fullscreenBtn)

                actionBtn.text = getString(R.string.delete)
                actionBtn.visibility = View.GONE

                imageView.setColorFilter(
                    ContextCompat.getColor(
                        p.context,
                        R.color.semiTransparentDark
                    )
                )

                loadImageIntoImageView(position, p.context, actionBtn, imageView, progressBar, image, fullscreenBtn)

                imageView.setOnLongClickListener {
                    imageView.setColorFilter(
                        ContextCompat.getColor(
                            p.context,
                            R.color.semiTransparentDark
                        )
                    )
                    actionBtn.visibility = View.VISIBLE

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(3000)
                        imageView.clearColorFilter()
                        actionBtn.visibility = View.GONE
                    }

                    true
                }

                imageView.setOnClickListener {
                    imageView.clearColorFilter()
                    actionBtn.visibility = View.GONE
                }
            }

            private fun loadImageIntoImageView(
                position: Int,
                context: Context,
                actionButton: MaterialButton,
                imageView: ImageView,
                progressBar: ProgressBar,
                img: Uri,
                fullscreenBtn: MaterialButton
            ) {
                val transitionName = img.toString()
                ViewCompat.setTransitionName(imageView, transitionName)

                val cropBtn = p.findViewById<MaterialButton>(R.id.cropButton)
                cropBtn.visibility = View.VISIBLE
                cropBtn.setOnClickListener {
                    lastImageBeforeCrop = img
                    viewModel.setCurrentImage(img)
                    val fragment = ImageCropFragment.newInstance(null)
                    activity.showBottomSheet(fragment, ImageCropFragment.TAG)
                }

                Glide.with(context)
                    .load(img)
                    .addListener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            actionButton.text = "Retry"
                            actionButton.visibility = View.VISIBLE
                            progressBar.visibility = View.GONE

                            actionButton.setOnClickListener {
                                progressBar.visibility = View.VISIBLE
                                actionButton.visibility = View.GONE
                                loadImageIntoImageView(position, context, actionButton, imageView, progressBar, img, fullscreenBtn)
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {

                            target?.getSize { width, height ->

                                fullscreenBtn.visibility = View.VISIBLE

                                fullscreenBtn.setOnClickListener {

                                    imageView.clearColorFilter()
                                    actionButton.visibility = View.GONE

                                    val imageFragment = ImageViewFragment.newInstance(SimpleMessage(), Pair(transitionName, transitionName), width, height)
                                    activity.supportFragmentManager.beginTransaction()
                                        .addSharedElement(imageView, transitionName)
                                        .add(android.R.id.content, imageFragment, ImageViewFragment.TAG)
                                        .commit()
                                }
                            }

                            actionButton.text = "Delete"
                            actionButton.visibility = View.GONE
                            progressBar.visibility = View.GONE
                            imageView.clearColorFilter()

                            actionButton.setOnClickListener {
                                viewModel.removeProjectImage(img)
                            }

                            return false
                        }

                    })
                    .into(imageView)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            return ImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.image_layout, parent, false))
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            holder.bind(getItem(position), position)
        }

    }

}