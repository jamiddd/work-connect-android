package com.jamid.workconnect.home

import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.text.*
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.*
import android.view.View
import android.view.View.FOCUS_DOWN
import android.widget.*
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.jamid.workconnect.*
import com.jamid.workconnect.databinding.FragmentEditorBinding
import com.jamid.workconnect.model.ObjectType
import com.jamid.workconnect.model.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class EditorFragment : Fragment() {

    private var currentImagePosition = 0
    private lateinit var binding: FragmentEditorBinding
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var container: LinearLayout
    private var blogItemsList = arrayListOf<BlogItem>()
    private var currentImageProgressBar: ProgressBar? = null
    private var currentImage: SimpleDraweeView? = null
    private var currentDialog: DialogInterface? = null
    private var mActivePointerId: Int = 0
    private var positionFromBottom = 0
    private var prevLineCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEditorBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.blog_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.done_blog -> {
                // title, blogItemsList, tags, links
                if (binding.titleText.text.isNullOrBlank()) return true
                val title = binding.titleText.text.trim().toString()

                val tags = mutableListOf<String>()

                for (ch in binding.tagsGroup.children) {
                    val chip = ch as Chip
                    tags.add(chip.text.toString())
                }
                val links = emptyList<String>()

                viewModel.upload(title, type = ObjectType.Blog, tags = tags, links = links, items = blogItemsList)

                currentDialog = MaterialAlertDialogBuilder(requireContext())
                    .setCancelable(false)
                    .setView(R.layout.creating_blog_progress_dialog)
                    .show()

                true
            }
            else -> true
        }
    }

    private fun onNavigateUp() {
        hideKeyboard()
        val dialogInterface = MaterialAlertDialogBuilder(requireContext()).setCancelable(
            false
        ).setTitle("Do you want to save the contents?")
            .setMessage("If you press No all the contents will be deleted.")
            .setPositiveButton("Yes") { dialogInterface: DialogInterface, i: Int ->
                viewModel.setBlogFragmentData(blogItemsList)
            }.setNegativeButton("No") { dialogInterface: DialogInterface, i: Int ->
                viewModel.setBlogFragmentData(null)
            }.show()

        dialogInterface.setOnCancelListener {
            findNavController().navigateUp()
        }

        dialogInterface.setOnDismissListener {
            findNavController().navigateUp()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity
        container = binding.blogItemContainer
        var infoText = ""

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.authorImg.setImageURI(it.photo)
                infoText = it.name + " ● " + SimpleDateFormat("h:mm a", Locale.UK).format(Date())
                binding.authorInfo.text = infoText
            }
        }
        initContainer(activity)


        /*lifecycleScope.launch {
            delay(1000)
            ViewCompat.setOnApplyWindowInsetsListener(
                binding.cardView
            ) { _: View?, insets: WindowInsetsCompat ->
                ViewCompat.onApplyWindowInsets(
                    binding.cardView,
                    insets.replaceSystemWindowInsets(
                        insets.systemWindowInsetLeft, 0,
                        insets.systemWindowInsetRight, 0
                    )
                )
            }
        }*/

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.cardView
        ) { _: View?, insets: WindowInsetsCompat ->
            ViewCompat.onApplyWindowInsets(
                binding.cardView,
                insets.replaceSystemWindowInsets(
                    insets.systemWindowInsetLeft, 0,
                    insets.systemWindowInsetRight, 0
                )
            )
        }

        val screenHeight = getWindowHeight()

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->
            binding.editorScroll.setPadding(0, top + convertDpToPx(56), 0, bottom + convertDpToPx(100))

            val currentPosition = findCurrentFocusedViewPosition()
            val child = container.getChildAt(currentPosition)
            if (child != null && child is EditText) {
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
                        binding.editorScroll.scrollY = binding.editorScroll.scrollY + diff
                    } else {
                        // when the cursor is above the keyboard
                    }

                }
            }

            val params = binding.cardView.layoutParams as ConstraintLayout.LayoutParams
            params.height = bottom + convertDpToPx(48)
            params.width = ConstraintLayout.LayoutParams.MATCH_PARENT
            params.bottomToBottom = binding.editorRoot.id
            binding.cardView.layoutParams = params

            val params1 = binding.cardBlur.layoutParams as ViewGroup.LayoutParams
            params1.height = bottom + convertDpToPx(48)
            params1.width = ViewGroup.LayoutParams.MATCH_PARENT
            binding.cardBlur.layoutParams = params1

        }

        activity.onBackPressedDispatcher.addCallback(this) {
            onNavigateUp()
//            activity.supportFragmentManager.popBackStack()

        }

        viewModel.postUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            currentDialog?.dismiss()

            when (result) {
                is Result.Success -> {
                    Toast.makeText(
                        requireContext(),
                        "Created blog successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.clearPostChanges()
                    findNavController().navigateUp()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), result.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.postPhotoUploadResult.observe(viewLifecycleOwner) {
            val result = it ?: return@observe

            currentImageProgressBar?.visibility = View.GONE

            when (result) {
                is Result.Success -> {
                    currentImage?.colorFilter = null

                    val container = (currentImage?.parent as ConstraintLayout?)!!
                    val pos = findImagePosition(container)
                    blogItemsList[pos].content = result.data
                }
                is Result.Error -> {
                    val text = TextView(requireContext())
                    text.text = "There was an error uploading this image."
                    val container = (currentImage?.parent as ConstraintLayout?)!!
                    container.addView(text, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT))

                    (text.layoutParams as ConstraintLayout.LayoutParams).topToTop = container.id
                    (text.layoutParams as ConstraintLayout.LayoutParams).startToStart = container.id
                    (text.layoutParams as ConstraintLayout.LayoutParams).endToEnd = container.id
                    (text.layoutParams as ConstraintLayout.LayoutParams).bottomToBottom = container.id
                }
            }
        }

        viewModel.currentCroppedImageUri.observe(viewLifecycleOwner) {
            if (it != null) {
                container.addViewAfter("Image", it.toString())
                val path = it.path
                viewModel.uploadPostImage(path, ObjectType.Blog)
                viewModel.setCurrentImage(null)
            }
        }

        viewModel.currentPlace.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.authorInfo.text = "$infoText ● $it"
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

        val prevData = viewModel.blogFragmentData.value
        if (prevData != null) {
            container.removeAllViews()
            blogItemsList.clear()
            prevData.forEachIndexed { index, blogItem ->
                val child = if (blogItem.type != "Image") {
                    val e = SpannableStringBuilder(blogItem.content)
                    getNewTextField(activity, blogItem.type, e, blogItem.hint)
                } else {
                    getNewImageContainer(activity, blogItem.content)
                }
                container.addView(child, index)
            }
            blogItemsList = prevData
        }

        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {

            override fun onViewAttachedToWindow(v: View?) {
                /*v?.let {
                    Log.d("Editor", "Scrolling to new child.")
                    it.requestFocus()
                    val height = it.height
                    binding.editorScroll.smoothScrollTo(0, binding.editorScroll.scrollY + height)
                }*/
            }

            override fun onViewDetachedFromWindow(v: View?) {

            }

        })

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
                R.id.headingType -> "Heading"
                R.id.subHeadingType -> "SubHeading"
                R.id.paraType -> "Paragraph"
                else -> "Paragraph"
            }

            blogItemsList[currentPos].type = type

            setEditTextType(prev, type)
        }

        binding.imageTypeBtn.setOnClickListener {
            hideKeyboard()
            activity.invokeImageSelectOptions()
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
            val editText = container.focusedChild as EditText
            val currentPosition = findCurrentFocusedViewPosition()
            val type = if (blogItemsList[currentPosition].type == "Quote") {
                "Paragraph"
            } else {
                "Quote"
            }

            blogItemsList[currentPosition].type = type

            setEditTextType(editText, type)
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
            activity.invokeTagFragment()
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
    }

    private fun initContainer(activity: FragmentActivity) {
        blogItemsList.add(BlogItem("", "Paragraph"))
        val textField = getNewTextField(activity, "Paragraph", null, null)
        container.addView(textField)
        textField.requestFocus()
        showKeyboard()
    }

    private fun EditText.setListeners() {
        val floatingActionModeCallback = FloatingActionModeCallback(this)
        customSelectionActionModeCallback = floatingActionModeCallback
        doOnTextChanged { text, _, _, count -> // start, before
            if (!text.isNullOrEmpty()) {
                val currentPos = findCurrentFocusedViewPosition()
                if (text.last() == '\n' && count == 1 && blogItemsList[currentPos].type != "Code") {
                    setText(text.trimEnd().toString())
                    container.addViewAfter("Paragraph")
                } else if (count == 1 && text.last() == '\n' && blogItemsList[findCurrentFocusedViewPosition()].type == "Code") {
                    if (text.length > 2 && text[text.lastIndex - 1] == '\n') {
                        setText(text.trimEnd().toString())
                        container.addViewAfter("Paragraph")
                    }
                }
                blogItemsList[currentPos].content = text.trimEnd().toString()
                if (prevLineCount != this.lineCount) {
                    adjustments()
                }
                prevLineCount = this.lineCount
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
            if (event.action !=KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener true
            }

            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> {
                    if (text.isBlank()) {
                        removeCurrentView()
                    }
                }
                KeyEvent.KEYCODE_BACK -> {
                    activity?.onBackPressed()
                }
            }
            true
        }

    }

    private fun adjustments() {
        val currentPosition = findCurrentFocusedViewPosition()
        val child = container.getChildAt(currentPosition)
        if (child != null && child is EditText) {
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

                val newPositionFromBottom = positionFromBottom - convertDpToPx(40)
                if (cursorY > newPositionFromBottom) {
                    // when the cursor is below the keyboard
                    Log.d("Editor", "cursorY - $cursorY, cursorNext - ${cursorY + convertDpToPx(50)}")
                    binding.editorScroll.scrollTo(0, cursorY + convertDpToPx(50))
                } else {
                    // when the cursor is above the keyboard
                }
            }
        }

    }

    private fun SimpleDraweeView.setListeners(p: ConstraintLayout, btn: MaterialButton) {
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
    }

    /* There's a better and obvious way of doing this */
    private fun findImagePosition(view: View) : Int {
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

    private fun LinearLayout.addViewAfter(type: String = "Paragraph", img: String? = null, isRecursive: Boolean = false, initialText: Editable? = null, hintText: String? = null) {
        var currentPosition = findCurrentFocusedViewPosition()
        if (type == "Image") {
            blogItemsList.add(BlogItem(img, "Image"))
            val imgContainer = getNewImageContainer(this.context, img)
            addView(imgContainer, currentPosition + 1)
            currentImagePosition = currentPosition + 1
            (imgContainer.layoutParams as LinearLayout.LayoutParams).setMargins(0, convertDpToPx(8), 0, convertDpToPx(8))
            addViewAfter("Paragraph", isRecursive = true)
        } else {
            if (isRecursive) currentPosition += 1
            val newEditText = getNewTextField(this.context, type, initialText, hintText)
            if (currentPosition == container.childCount - 1) {
                blogItemsList.add(BlogItem("", type))
                container.addView(newEditText)
                if (type == "Code") {
                    blogItemsList[blogItemsList.lastIndex].content = initialText.toString()
                }
            } else {
                blogItemsList.add(currentPosition + 1, BlogItem("", type))
                container.addView(newEditText, currentPosition + 1)
                if (type == "Code") {
                    blogItemsList[currentPosition + 1].content = initialText.toString()
                }
            }
            (newEditText.layoutParams as LinearLayout.LayoutParams).setMargins(0, convertDpToPx(4), 0, convertDpToPx(4))
            newEditText.requestFocus()

            lifecycleScope.launch {
                delay(500)
                binding.editorScroll.fullScroll(FOCUS_DOWN)
            }
        }
    }

    private fun findCurrentFocusedViewPosition() : Int {
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

    companion object {
        const val IMAGE_SELECT_REQUEST = 12
        const val TAG = "CreateBlogFragment"
        @JvmStatic
        fun newInstance() = EditorFragment()
    }

    private fun getNewImageContainer(context: Context, imgContent: String?): ConstraintLayout {
        val imgContainer = ConstraintLayout(context)
        val now = System.currentTimeMillis().toInt()
        imgContainer.id = now

        val img = SimpleDraweeView(context)
        img.id = now + 1
        img.isLongClickable = true
        img.isClickable = true
        img.adjustViewBounds = true
        img.setColorFilter(ContextCompat.getColor(context, R.color.semiTransparentDark))
        imgContainer.addView(img, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, convertDpToPx(300)))

        val constrainSet = ConstraintSet()
        constrainSet.clone(imgContainer)

        val delButton = MaterialButton(context)
        val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyle)
        progressBar.isIndeterminate = true
        progressBar.tag = img.id
        progressBar.indeterminateTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))

        delButton.text = "Delete"
        delButton.setTextAppearance(R.style.TextAppearance_AppCompat_Button)
        delButton.setPadding(convertDpToPx(16), convertDpToPx(12), convertDpToPx(16), convertDpToPx(12))
        delButton.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        delButton.setTextColor(ContextCompat.getColor(context, R.color.white))
        delButton.strokeColor = ContextCompat.getColorStateList(context, R.color.white)
        delButton.iconTint = ContextCompat.getColorStateList(context, R.color.white)
        delButton.cornerRadius = convertDpToPx(22)
        delButton.strokeWidth = convertDpToPx(2)
        delButton.icon = ContextCompat.getDrawable(context, R.drawable.ic_baseline_delete_24)
        delButton.tag = img.id
//        delButton.rippleColor = ContextCompat.getColorStateList(context, R.color.white)
        imgContainer.addView(delButton, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT))
        imgContainer.addView(progressBar, ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT))

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
    }

    // preparing for next try
    private fun getNewTextField(context: Context, type: String, initialText: Editable?, hintText: String?) : EditText {
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
                editText.setBackgroundColor(Color.parseColor("#F1F1F1"))
                editText.setPadding(convertDpToPx(16), convertDpToPx(12), convertDpToPx(16), convertDpToPx(12))
            }
            "Image" -> {
                throw Exception("Cannot create edittext of type image")
            }
            "Quote" -> {
                editText.textSize = convertDpToPx(7f)
                editText.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                editText.background = ContextCompat.getDrawable(editText.context, R.drawable.quote_background)
            }
        }
    }

    inner class FloatingActionModeCallback(v: View) : ActionMode.Callback2() {

        var actionMode: ActionMode? = null
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
                    spanText = "BOLD"
                    StyleSpan(Typeface.BOLD)
                }
                R.id.italic -> {
                    setSpanRelatively(spannable, start, end, StyleSpan(Typeface.ITALIC))
                    spanText = "ITALIC"
                    StyleSpan(Typeface.ITALIC)
                }
                R.id.underline -> {
                    setSpanRelatively(spannable, start, end, UnderlineSpan())
                    spanText = "UNDERLINE"
                    UnderlineSpan()
                }
                R.id.strike -> {
                    setSpanRelatively(spannable, start, end, StrikethroughSpan())
                    spanText = "STRIKETHROUGH"
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

    /* Need improvements */
    private fun getHtmlText(text: String, span: CharacterStyle?, range: IntRange): String {
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
    }

}