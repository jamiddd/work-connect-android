package com.jamid.workconnect.home

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.*
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.*
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentEditorBinding
import com.jamid.workconnect.databinding.ImageLayoutBinding
import com.jamid.workconnect.model.GenericMenuItem
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditorFragment : SupportFragment(R.layout.fragment_editor) {

    private var currentImagePosition = 0
    private lateinit var binding: FragmentEditorBinding
    private lateinit var container: LinearLayout
    private var blogItemsList = arrayListOf<BlogItem>()
    private var currentImageProgressBar: ProgressBar? = null
    private var currentImage: ImageView? = null
    private var isInitialized = false

    private fun onNavigateUp() {
        hideKeyboard()

        fun showDraftDialog() {
            viewModel.extras[OLD_STATE] = blogItemsList
            val gdf = GenericDialogFragment.newInstance(EDITOR, "Do you want to save the contents?", "If you press No all the contents will be deleted.", isActionOn = true, isCancelable = true)
            activity.showBottomSheet(gdf)
        }

        if (blogItemsList.size < 2) {
            if ( (container.getChildAt(0) as EditText).text.isNullOrBlank() ) {
                findNavController().navigateUp()
                return
            } else {
                showDraftDialog()
            }
        } else {
            showDraftDialog()
        }
        /*MaterialAlertDialogBuilder(activity).setCancelable(
            false
        ).setTitle("Do you want to save the contents?")
            .setMessage("If you press No all the contents will be deleted.")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.extras[OLD_STATE] = blogItemsList
                findNavController().navigateUp()
            }.setNegativeButton("No") { _, _ ->
                findNavController().navigateUp()
            }.show()*/
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditorBinding.bind(view)

        container = binding.blogItemContainer
        var infoText: String

        val user = viewModel.user.value!!

        binding.authorImg.setImageURI(user.photo)
        infoText = user.name + " • " + SimpleDateFormat("h:mm a", Locale.UK).format(Date())
        binding.authorInfo.text = infoText
        initContainer(activity)

        binding.editorFragmentToolbar.setNavigationOnClickListener {
            onNavigateUp()
        }

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            onNavigateUp()
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
                        "Blog created successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Result.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "Something went wrong - " + result.exception.localizedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->

            binding.editorScroll.setPadding(0, convertDpToPx(8), 0, bottom + convertDpToPx(100))
            binding.editorFragmentToolbar.updateLayout(marginTop = top)

            val currentPosition = findCurrentFocusedViewPosition()
            val child = container.getChildAt(currentPosition)
            if (child != null && child is EditText) {
                val threshold = calculateThreshold(bottom + convertDpToPx(80))
                Log.d(BUG_TAG, "Threshold $threshold")

                // get cursor position
                val cursorPosition = findCursorPositionOnScreen(child)
                Log.d(BUG_TAG, "Cursor position - $cursorPosition")
                if (cursorPosition > threshold) {
                    binding.editorScroll.scrollY =
                        binding.editorScroll.scrollY + (cursorPosition - threshold) + convertDpToPx(
                            8
                        )
                    Log.d(
                        BUG_TAG,
                        "The keyboard is hiding the cursor - ${cursorPosition - threshold}"
                    )
                } else {
                    Log.d(
                        BUG_TAG,
                        "The keyboard is not hiding the cursor - ${threshold - cursorPosition}"
                    )
                }
            }

            val params = binding.bottomActions.layoutParams as CoordinatorLayout.LayoutParams
            params.height = bottom + convertDpToPx(48)
            params.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
            binding.bottomActions.layoutParams = params
        }

        viewModel.postPhotoUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            currentImageProgressBar?.visibility = View.GONE

            when (result) {
                is Result.Success -> {
                    currentImage?.colorFilter = null

                    val container = (currentImage?.parent as ConstraintLayout?)!!
                    val pos = findImagePosition(container)
                    blogItemsList[pos].content = result.data.toString()
                }
                is Result.Error -> {
                    val text = TextView(requireContext())
                    text.text = getString(R.string.upload_image_error)
                    val container = (currentImage?.parent as ConstraintLayout?)!!
                    container.addView(
                        text,
                        ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                        )
                    )

                    (text.layoutParams as ConstraintLayout.LayoutParams).topToTop = container.id
                    (text.layoutParams as ConstraintLayout.LayoutParams).startToStart = container.id
                    (text.layoutParams as ConstraintLayout.LayoutParams).endToEnd = container.id
                    (text.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom =
                        container.id
                }
            }
        }

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                container.addViewAfter(IMAGE, it.toString())
                viewModel.uploadPostImage(it, BLOG)
                viewModel.setCurrentImage(null)
            }
        }

        val blue = ContextCompat.getColor(activity, R.color.blue_500)
        viewModel.currentPlace.observe(viewLifecycleOwner) {
            if (it != null) {
                val info = SpannableStringBuilder.valueOf("$infoText • $it")
                val start = infoText.length + 3
                val end = start + it.length
                info.setSpan(ForegroundColorSpan(blue), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                info.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                binding.authorInfo.text = info
                val location = viewModel.currentLocation.value
                if (location != null) {
                    location.place = it
                    viewModel.setCurrentLocation(location)
                }
            } else {
                binding.authorInfo.text = infoText
            }
        }

        viewModel.tag.observe(viewLifecycleOwner) {
            if (it != null) {
                addNewChip(it)
            }
        }

        restorePreviousDraftIfAvailable()

        binding.textTypeBtn.setOnClickListener {
            lifecycleScope.launch {
                delay(5000)
                binding.toggleButtonTextType.visibility = View.GONE
            }
            if (binding.toggleButtonTextType.isVisible) {
                binding.toggleButtonTextType.visibility = View.GONE
            } else {
                binding.toggleButtonTextType.visibility = View.VISIBLE
            }
        }

        binding.toggleButtonTextType.addOnButtonCheckedListener { _, checkedId, _ -> // group, isChecked
            val prev = container.focusedChild as EditText?
            prev ?: return@addOnButtonCheckedListener
            val currentPos = findCurrentFocusedViewPosition()

            val type = when (checkedId) {
                R.id.headingType -> HEADING
                R.id.subHeadingType -> SUB_HEADING
                R.id.paraType -> PARAGRAPH
                else -> PARAGRAPH
            }

            blogItemsList[currentPos].type = type

            setEditTextType(prev, type)
        }

        binding.imageTypeBtn.setOnClickListener {
            hideKeyboard()
            openImageSelectMenu()
        }

        binding.imageTypeBtn.setOnLongClickListener {

            lifecycleScope.launch {
                delay(5000)
                binding.toggleButtonImageType.visibility = View.GONE
            }
            if (binding.toggleButtonImageType.isVisible) {
                binding.toggleButtonImageType.visibility = View.GONE
            } else {
                binding.toggleButtonImageType.visibility = View.VISIBLE
            }
            true
        }


        binding.quoteBtn.setOnClickListener {
            val editText = container.focusedChild as EditText?
            if (editText != null) {
                val currentPosition = findCurrentFocusedViewPosition()
                val type = if (blogItemsList[currentPosition].type == "Quote") {
                    "Paragraph"
                } else {
                    "Quote"
                }

                blogItemsList[currentPosition].type = type

                setEditTextType(editText, type)
            }
        }

        binding.codeBtn.setOnClickListener {
            val e = SpannableStringBuilder("function func_name(){\n\t// write your code here\n}")
            container.addViewAfter("Code", initialText = e)
            Log.d(TAG, "${blogItemsList.size}")
        }

        binding.locationBtn.setOnClickListener {
            hideKeyboard()
            activity.invokeLocationFragment()
        }

        binding.tagBtn.setOnClickListener {
            val fragment = TagFragment.newInstance()
            activity.showBottomSheet(fragment, TagFragment.TAG)
        }

        binding.authorInfo.setOnClickListener {
            if (viewModel.currentPlace.value != null) {
                val instance = GenericDialogFragment.newInstance(
                    REMOVING_LOCATION,
                    "Remove location",
                    "Are you sure you want to remove the given location for this project?",
                    isActionOn = true,
                    isCancelable = true
                )
                activity.showBottomSheet(instance, CreateProjectFragment.TAG)
            } else {
                infoText = user.name + " • " + SimpleDateFormat("h:mm a", Locale.UK).format(Date())
                binding.authorInfo.text = infoText
            }
        }

        binding.editorFragmentDoneButton.setOnClickListener {
            // title, blogItemsList, tags, links
            createBlog()
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun restorePreviousDraftIfAvailable() {
        val bundle = viewModel.extras
        if (bundle.containsKey(OLD_STATE)) {
            val items = bundle[OLD_STATE] as ArrayList<BlogItem>
            container.removeAllViews()
            blogItemsList.clear()

            items.forEachIndexed { index, blogItem ->
                val child = if (blogItem.type != IMAGE) {
                    val e = SpannableStringBuilder.valueOf(blogItem.content)
                    getNewTextField(activity, blogItem.type, e, blogItem.hint)
                } else {
                    getNewImage(activity, blogItem.content!!, isRestored = true)
                }
                container.addView(child, index)
            }
            blogItemsList = items
            viewModel.extras.remove(OLD_STATE)
        }
    }

    private fun createBlog() {
        if (binding.titleText.text.isNullOrBlank()) return
        val title = binding.titleText.text.trim().toString()

        val tags = mutableListOf<String>()

        for (ch in binding.tagsGroup.children) {
            val chip = ch as Chip
            tags.add(chip.text.toString())
        }

        val items = mutableListOf<String>()
        for (blogItem in blogItemsList) {
            items.add(blogItem.toString())
        }

        val post = Post(
            "",
            title,
            viewModel.user.value!!,
            viewModel.user.value!!.id,
            "",
            type = BLOG,
            tags = tags,
            items = items
        )

        viewModel.uploadPost(post)

        val instance = GenericDialogFragment.newInstance(
            CREATING_BLOG,
            "Creating Blog",
            "Creating your blog. Please wait ...",
            isCancelable = false,
            isProgressing = true
        )
        activity.showBottomSheet(instance, TAG)
    }

    private fun openImageSelectMenu() {
        val tag = SELECT_IMAGE_MENU_BLOG
        val item1 = GenericMenuItem(
            tag,
            "Select from gallery",
            R.drawable.ic_baseline_add_photo_alternate_24,
            0
        )
        val item2 = GenericMenuItem(tag, "Take a photo", R.drawable.ic_baseline_camera_alt_24, 1)
        val item3 = GenericMenuItem(tag, "Remove image", R.drawable.ic_baseline_delete_24, 2)

        val fragment =
            GenericMenuFragment.newInstance(tag, "Add Image ...", arrayListOf(item1, item2, item3))
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
    }

    private fun initContainer(activity: FragmentActivity) {
        blogItemsList.add(BlogItem("", "Paragraph"))
        val textField = getNewTextField(activity, "Paragraph", null, null)
        container.addView(textField)
        textField.requestFocus()

        viewLifecycleOwner.lifecycleScope.launch {
            delay(500)
            showKeyboard()
        }
    }

    private fun EditText.setListeners() {
        val floatingActionModeCallback = FloatingActionModeCallback(this)
        customSelectionActionModeCallback = floatingActionModeCallback

        doOnTextChanged { t, _, b, count -> // start, before
            if (!t.isNullOrEmpty()) {
                checkCursorVisibility(this)
                if (b > count) {
                    Log.d(TAG, "Pressed Backspace")
                } else {
                    val currentPos = findCurrentFocusedViewPosition()

                    if (selectionStart == 1 && t[selectionStart - 1] == '\n') {
                        setText(t.toString().trimStart())
                        container.addTextBefore()
                    } else if (selectionEnd != text.length && t[selectionStart - 1] == '\n') {
                        if (selectionEnd != text.length) {
                            Log.d(TAG, "Pressed enter at middle")
                        }
                    } else {
                        Log.d(BUG_TAG, "Pressed enter at last")
                        if (t.last() == '\n' && count == 1 && blogItemsList[currentPos].type != "Code") {
                            setText(t.toString().trimEnd())
                            container.addViewAfter()
                        } else if (count == 1 && t.last() == '\n' && blogItemsList[findCurrentFocusedViewPosition()].type == "Code") {
                            if (t.length > 2 && t[t.lastIndex - 1] == '\n') {
                                setText(t.toString().trimEnd())
                                container.addViewAfter()
                            }
                        }
                    }
                    blogItemsList[currentPos].content = t.trimEnd().toString()
                }
            }
        }

        setOnFocusChangeListener { _, hasFocus ->
            val currentPos = findCurrentFocusedViewPosition()
            if (hasFocus) {
                if (blogItemsList[currentPos].type == "Code") {
                    binding.textTypeBtn.isEnabled = false
                    binding.quoteBtn.isEnabled = false
                    binding.toggleButtonTextType.isEnabled = false
                    binding.toggleButtonTextType.visibility = View.GONE
                }
            } else {
                binding.textTypeBtn.isEnabled = true
                binding.quoteBtn.isEnabled = true
                binding.toggleButtonTextType.isEnabled = true
            }
        }

        setOnKeyListener { _, keyCode, event -> // view
            //This is the filter
            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener true
            }

            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    if (text.isBlank()) {
                        removeCurrentView()
                    }
                }
                KeyEvent.KEYCODE_BACK -> {
                    activity.onBackPressed()
                }
            }
            true
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
            binding.editorScroll.scrollY =
                binding.editorScroll.scrollY + (cursorPosition - threshold) + convertDpToPx(8)
            Log.d(BUG_TAG, "The keyboard is hiding the cursor - ${cursorPosition - threshold}")
        } else {
            Log.d(BUG_TAG, "The keyboard is not hiding the cursor - ${threshold - cursorPosition}")
        }
    }

    /*private fun SimpleDraweeView.setListeners(p: ConstraintLayout, btn: MaterialButton) {
        setOnLongClickListener {
            setColorFilter(ContextCompat.getColor(this.context, R.color.semiTransparentDark))

            btn.visibility = View.VISIBLE
            true
        }

        setOnClickListener {
            clearColorFilter()
            btn.visibility = View.GONE
        }

        btn.setOnClickListener {
            val pos = findImagePosition(p)
            blogItemsList.removeAt(pos)
            container.removeView((parent as ViewGroup))
            container.getChildAt(pos - 1).requestFocus()
        }
    }*/

    /* There's a better and obvious way of doing this */
    private fun findImagePosition(view: View): Int {
        var pos = 0
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is ConstraintLayout && child.id == view.id) {
                pos = i
                break
            }
        }
        return pos
    }

    private fun removeCurrentView() {
        val currentPosition = findCurrentFocusedViewPosition()
        if (currentPosition == 0) return
        val editText = container.getChildAt(currentPosition) as EditText
        container.removeView(editText)
        blogItemsList.removeAt(currentPosition)

        val prev = container.getChildAt(currentPosition - 1)
        if (prev is EditText) {
            prev.requestFocus()
            prev.setSelection(prev.text.length)
        } else {
            blogItemsList.removeAt(currentPosition - 1)
            container.removeView(prev)
        }
    }

    private fun LinearLayout.addTextBefore(
        type: String = "Paragraph",
        initialText: Editable? = null,
        hintText: String? = null
    ) {
        val currentPosition = findCurrentFocusedViewPosition()
        val newEditText = getNewTextField(this.context, type, initialText, hintText)
        if (currentPosition == 0) {
            blogItemsList.add(0, BlogItem("", type))
            container.addView(newEditText, 0)
        } else {
            blogItemsList.add(currentPosition - 1, BlogItem((initialText ?: "").toString(), type))
            container.addView(newEditText, currentPosition - 1)
        }
        newEditText.setPadding(
            convertDpToPx(8),
            convertDpToPx(4),
            convertDpToPx(8),
            convertDpToPx(4)
        )
        newEditText.requestFocus()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(300)
            binding.editorScroll.scrollY -= convertDpToPx(80)
        }
    }

    private fun LinearLayout.addViewAfter(
        type: String = PARAGRAPH,
        img: String? = null,
        isRecursive: Boolean = false,
        initialText: Editable? = null,
        hintText: String? = null
    ) {
        var currentPosition = findCurrentFocusedViewPosition()
        if (type == IMAGE) {
            blogItemsList.add(BlogItem(img, IMAGE))
            val imgContainer = /*getNewImageContainer(this.context, img)*/
                getNewImage(this.context, img!!)
            addView(imgContainer, currentPosition + 1)
            currentImagePosition = currentPosition + 1
            (imgContainer.layoutParams as LinearLayout.LayoutParams).setMargins(
                0,
                convertDpToPx(8),
                0,
                convertDpToPx(8)
            )
            addViewAfter(PARAGRAPH, isRecursive = true)
        } else {
            if (isRecursive) currentPosition += 1
            val newEditText = getNewTextField(this.context, type, initialText, hintText)
            if (currentPosition == container.childCount - 1) {
                blogItemsList.add(BlogItem("", type))
                container.addView(newEditText)
                if (type == CODE) {
                    blogItemsList[blogItemsList.lastIndex].content = initialText.toString()
                }
            } else {
                blogItemsList.add(currentPosition + 1, BlogItem("", type))
                container.addView(newEditText, currentPosition + 1)
                if (type == CODE) {
                    blogItemsList[currentPosition + 1].content = initialText.toString()
                }
            }
            newEditText.setPadding(
                convertDpToPx(8),
                convertDpToPx(4),
                convertDpToPx(8),
                convertDpToPx(4)
            )
            /*(newEditText.layoutParams as LinearLayout.LayoutParams).setMargins(
                0,
                convertDpToPx(4),
                0,
                convertDpToPx(4)
            )*/
            newEditText.requestFocus()
            viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
//                binding.blogItemContainer.requestFocus(FOCUS_UP)
                binding.editorScroll.scrollY += convertDpToPx(80)
            }
        }
    }

    private fun findCurrentFocusedViewPosition(): Int {
        var pos = 0
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is EditText && child.hasFocus()) {
                pos = i
                break
            }
        }
        return pos
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearPostChanges()
    }

    override fun onDestroy() {
        viewModel.clearPostChanges()
        super.onDestroy()
    }


    private fun getNewImage(context: Context, img: String, isRestored: Boolean = false): View {
        val viewGroupBinding = DataBindingUtil.inflate<ImageLayoutBinding>(
            layoutInflater,
            R.layout.image_layout,
            null,
            false
        )

        viewGroupBinding.primaryAction.text = getString(R.string.delete)
        viewGroupBinding.primaryAction.visibility = View.GONE

        viewGroupBinding.image.setColorFilter(
            ContextCompat.getColor(
                context,
                R.color.semiTransparentDark
            )
        )

        currentImage = viewGroupBinding.image
        currentImageProgressBar = viewGroupBinding.imageLayoutProgress

        loadImageIntoImageView(context, viewGroupBinding, img, isRestored)

        viewGroupBinding.image.setOnLongClickListener {
            viewGroupBinding.image.setColorFilter(
                ContextCompat.getColor(
                    context,
                    R.color.semiTransparentDark
                )
            )
            viewGroupBinding.primaryAction.visibility = View.VISIBLE
            true
        }

        viewGroupBinding.image.setOnClickListener {
            viewGroupBinding.image.clearColorFilter()
            viewGroupBinding.primaryAction.visibility = View.GONE
        }

        return viewGroupBinding.root
    }

    private fun loadImageIntoImageView(
        context: Context,
        viewGroupBinding: ImageLayoutBinding,
        img: String,
        isRestored: Boolean = false
    ) {

        Glide.with(context)
            .load(img)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    viewGroupBinding.primaryAction.text = "Retry"
                    viewGroupBinding.primaryAction.visibility = View.VISIBLE
                    viewGroupBinding.imageLayoutProgress.visibility = View.GONE

                    viewGroupBinding.primaryAction.setOnClickListener {
                        viewGroupBinding.imageLayoutProgress.visibility = View.VISIBLE
                        viewGroupBinding.primaryAction.visibility = View.GONE
                        loadImageIntoImageView(context, viewGroupBinding, img, isRestored)
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
                    viewGroupBinding.primaryAction.text = "Delete"
                    viewGroupBinding.primaryAction.visibility = View.GONE

                    if (isRestored) {
                        viewGroupBinding.imageLayoutProgress.visibility = View.GONE
                        viewGroupBinding.image.clearColorFilter()
                    }

                    viewGroupBinding.imageLayoutRoot.id = System.currentTimeMillis().toInt()

                    viewGroupBinding.primaryAction.setOnClickListener {
                        val pos = findImagePosition(viewGroupBinding.imageLayoutRoot)
                        blogItemsList.removeAt(pos)
                        container.removeView(viewGroupBinding.root)
                        container.getChildAt(pos - 1).requestFocus()
                    }

                    return false
                }

            })
            .into(viewGroupBinding.image)
    }

    /*private fun getNewImageContainer(context: Context, imgContent: String?): ConstraintLayout {
        val imgContainer = ConstraintLayout(context)
        val now = System.currentTimeMillis().toInt()
        imgContainer.id = now

        val img = SimpleDraweeView(context)
        img.id = now + 1
        img.isLongClickable = true
        img.isClickable = true
        img.adjustViewBounds = true
        img.setColorFilter(ContextCompat.getColor(context, R.color.semiTransparentDark))
        imgContainer.addView(
            img,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                convertDpToPx(300)
            )
        )

        val constrainSet = ConstraintSet()
        constrainSet.clone(imgContainer)

        val delButton = MaterialButton(context)
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyle)
        progressBar.isIndeterminate = true
        progressBar.tag = img.id
        progressBar.indeterminateTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))

        delButton.text = "Delete"
        delButton.setTextAppearance(R.style.TextAppearance_AppCompat_Button)
        delButton.setPadding(
            convertDpToPx(16),
            convertDpToPx(12),
            convertDpToPx(16),
            convertDpToPx(12)
        )
        delButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        delButton.setTextColor(ContextCompat.getColor(context, R.color.white))
        delButton.strokeColor = ContextCompat.getColorStateList(context, R.color.white)
        delButton.iconTint = ContextCompat.getColorStateList(context, R.color.white)
        delButton.cornerRadius = convertDpToPx(22)
        delButton.strokeWidth = convertDpToPx(2)
        delButton.icon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)
        delButton.tag = img.id
//        delButton.rippleColor = ContextCompat.getColorStateList(context, R.color.white)
        imgContainer.addView(
            delButton,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        )
        imgContainer.addView(
            progressBar,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
        )

        delButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        (delButton.layoutParams as ConstraintLayout.LayoutParams).topToTop = imgContainer.id
        (delButton.layoutParams as ConstraintLayout.LayoutParams).startToStart = imgContainer.id
        (delButton.layoutParams as ConstraintLayout.LayoutParams).endToEnd = imgContainer.id
        (delButton.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom = imgContainer.id

        (progressBar.layoutParams as ConstraintLayout.LayoutParams).topToTop = imgContainer.id
        (progressBar.layoutParams as ConstraintLayout.LayoutParams).startToStart = imgContainer.id
        (progressBar.layoutParams as ConstraintLayout.LayoutParams).endToEnd = imgContainer.id
        (progressBar.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom = imgContainer.id

        currentImageProgressBar = progressBar
        currentImage = img

        img.setImageURI(imgContent)

        img.setListeners(imgContainer, delButton)

        return imgContainer
    }*/

    // preparing for next try
    private fun getNewTextField(
        context: Context,
        type: String,
        initialText: Editable?,
        hintText: String?
    ): EditText {
        val editText = EditText(context)
        editText.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        editText.apply {
            minHeight = convertDpToPx(48)
            setPadding(convertDpToPx(12), 0, convertDpToPx(12), 0)
            isSingleLine = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            text = initialText
            hint = hintText
        }

        setEditTextType(editText, type)

        editText.setListeners()

        return editText
    }

    private fun setEditTextType(editText: EditText, type: String) {
        editText.background = null
        when (type) {
            HEADING -> {
                editText.setTextAppearance(R.style.TextAppearance_AppCompat_Display1)
                editText.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                editText.setTextColor(ContextCompat.getColor(editText.context, R.color.black))
            }
            SUB_HEADING -> {
                editText.setTextAppearance(R.style.TextAppearance_AppCompat_Large)
                editText.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                editText.setTextColor(ContextCompat.getColor(editText.context, R.color.black))
            }
            PARAGRAPH -> {
                editText.setTextAppearance(R.style.TextAppearance_AppCompat_Medium)
                editText.setTextColor(ContextCompat.getColor(editText.context, R.color.black))
            }
            CODE -> {
                editText.setTextAppearance(R.style.TextAppearance_AppCompat_Medium)
                editText.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                editText.background = null
                editText.setBackgroundColor(getColorBasedOnTheme(editText.context))
                editText.setPadding(
                    convertDpToPx(16),
                    convertDpToPx(12),
                    convertDpToPx(16),
                    convertDpToPx(12)
                )
            }
            QUOTE -> {
                editText.setTextAppearance(R.style.TextAppearance_AppCompat_Medium)
                editText.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                editText.background = getDrawableBasedOnTheme(editText.context)
            }
        }
    }

    private fun getDrawableBasedOnTheme(context: Context): Drawable? {

        val quoteBackgroundNight =
            ContextCompat.getDrawable(context, R.drawable.quote_background_night)
        val quoteBackgroundDay = ContextCompat.getDrawable(context, R.drawable.quote_background)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                quoteBackgroundNight
            } else {
                quoteBackgroundDay
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                quoteBackgroundNight
            } else {
                quoteBackgroundDay
            }
        }
    }

    private fun getColorBasedOnTheme(context: Context): Int {

        val colorDay = ContextCompat.getColor(context, R.color.grey)
        val colorNight = ContextCompat.getColor(context, R.color.darkestGrey)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity.resources?.configuration?.isNightModeActive == true) {
                colorNight
            } else {
                colorDay
            }
        } else {
            if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                colorNight
            } else {
                colorDay
            }
        }
    }

    /* private fun setEditTextType(editText: EditText, type: String) {
         when (type) {
             "Heading" -> {
                 editText.textSize = convertDpToPx(13f)
                 editText.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                 editText.background = null
                 editText.setBackgroundColor(Color.TRANSPARENT)
             }
             "SubHeading" -> {
                 editText.textSize = convertDpToPx(10f)
                 editText.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                 editText.background = null
                 editText.setBackgroundColor(Color.TRANSPARENT)
             }
             "Paragraph" -> {
                 editText.textSize = convertDpToPx(7f)
                 editText.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                 editText.background = null
                 editText.setBackgroundColor(Color.TRANSPARENT)
             }
             "Code" -> {
                 editText.textSize = convertDpToPx(5f)
                 editText.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                 editText.background = null
                 editText.setPadding(
                     convertDpToPx(16),
                     convertDpToPx(12),
                     convertDpToPx(16),
                     convertDpToPx(12)
                 )

                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                     if (activity.resources?.configuration?.isNightModeActive == true) {
                         editText.setBackgroundColor(
                             ContextCompat.getColor(
                                 activity,
                                 R.color.darkestGrey
                             )
                         )
                     } else {
                         editText.setBackgroundColor(ContextCompat.getColor(activity, R.color.grey))
                     }
                 } else {
                     if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                         editText.setBackgroundColor(
                             ContextCompat.getColor(
                                 activity,
                                 R.color.darkestGrey
                             )
                         )
                     } else {
                         editText.setBackgroundColor(ContextCompat.getColor(activity, R.color.grey))
                     }
                 }

             }
             "Image" -> {
                 throw Exception("Cannot create edittext of type image")
             }
             "Quote" -> {
                 editText.textSize = convertDpToPx(7f)
                 editText.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)

                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                     if (activity.resources?.configuration?.isNightModeActive == true) {
                         editText.background = ContextCompat.getDrawable(
                             editText.context,
                             R.drawable.quote_background_night
                         )
                     } else {
                         editText.background =
                             ContextCompat.getDrawable(editText.context, R.drawable.quote_background)
                     }
                 } else {
                     if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                         editText.background = ContextCompat.getDrawable(
                             editText.context,
                             R.drawable.quote_background_night
                         )
                     } else {
                         editText.background =
                             ContextCompat.getDrawable(editText.context, R.drawable.quote_background)
                     }
                 }
             }
         }
     }*/

    inner class FloatingActionModeCallback(v: View) : ActionMode.Callback2() {

        private var actionMode: ActionMode? = null
        private val editText = v as EditText

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            mode.menuInflater.inflate(R.menu.edittext_style_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            actionMode = mode
            val spannable = editText.text as Spannable
            val start = editText.selectionStart
            val end = editText.selectionEnd

            var spanText = ""
            val span = when (item.itemId) {
                R.id.bold -> {
                    setSpanRelatively(spannable, start, end, StyleSpan(Typeface.BOLD))
                    spanText = BOLD
                    StyleSpan(Typeface.BOLD)
                }
                R.id.italic -> {
                    setSpanRelatively(spannable, start, end, StyleSpan(Typeface.ITALIC))
                    spanText = ITALIC
                    StyleSpan(Typeface.ITALIC)
                }
                R.id.underline -> {
                    setSpanRelatively(spannable, start, end, UnderlineSpan())
                    spanText = UNDERLINE
                    UnderlineSpan()
                }
                R.id.strike -> {
                    setSpanRelatively(spannable, start, end, StrikethroughSpan())
                    spanText = STRIKETHROUGH
                    StrikethroughSpan()
                }
                else -> null
            }

            // some spans added
            val childPos = findCurrentFocusedViewPosition()

            if (span != null) {
                blogItemsList[childPos].spans.add(spanText)
                blogItemsList[childPos].spanRangesStart.add(start)
                blogItemsList[childPos].spanRangesEnd.add(end)
            }

            mode.finish()
            return true
        }

        override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) {
            outRect.set(100, 0, 400, 100)
        }

    }

    private fun calculateThreshold(bottomInset: Int): Int {
        val totalHeight = binding.editorScroll.measuredHeight
        val partialTop = binding.editorScroll.scrollY
        val screenHeight = getFullScreenHeight()
        val usableScreenHeight = screenHeight - bottomInset - binding.editorAppBar.measuredHeight
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

        val textLayout = e.layout ?: return 0
        val line = textLayout.getLineForOffset(e.selectionStart)
        val currentPosition = findCurrentFocusedViewPosition()
        val constantHeight = getHeightForMultipleViews(
            binding.edittorScrollContainer,
            2
        ) + getHeightForMultipleViews(binding.blogItemContainer, currentPosition)

        return constantHeight + maxOf(textLayout.getLineBaseline(line), convertDpToPx(48))
    }

    /* Need improvements */
    /*private fun getHtmlText(text: String, span: CharacterStyle?, range: IntRange): String {
        val prefix = "<p>${text.substring(0 until range.first)}"
        val middle = when (span) {
            is StyleSpan -> {
                when (span.style) {
                    Typeface.ITALIC -> {
                        "<i>${text.substring(range)}</i>"
                    }
                    Typeface.BOLD -> {
                        "<b>${text.substring(range)}</b>"
                    }
                    else -> {
                        text.substring(range)
                    }
                }
            }
            is UnderlineSpan -> {
                "<u>${text.substring(range)}</u>"
            }
            is StrikethroughSpan -> {
                "<s>${text.substring(range)}</s>"
            }
            else -> {
                text.substring(range)
            }
        }

        val suffix = "${text.substring(range.last + 1..text.length)}</p>"
        return prefix + middle + suffix
    }*/

    companion object {
        const val TITLE = "Create Blog"
        const val OLD_STATE = "OLD_STATE"
        const val TAG = "CreateBlogFragment"

        @JvmStatic
        fun newInstance() = EditorFragment()
    }

}


