package com.jamid.workconnect

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.databinding.FragmentBlogBinding
import com.jamid.workconnect.model.BlogItemConverter
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SpanItem
import com.jamid.workconnect.profile.ProfileFragment

class BlogFragment : BasePostFragment(R.layout.fragment_blog, TAG, false) {

    private lateinit var binding: FragmentBlogBinding
    private var time: Long = 0

    @SuppressLint("Recycle")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentBlogBinding.bind(view)
        time = System.currentTimeMillis()
        postId = arguments?.getString(ARG_POST_ID)
        bottomBinding = binding.blogMetadata
        initLayoutChanges(binding.blogBottomBlur, binding.blogScroller)
        hideKeyboard()

        if (Build.VERSION.SDK_INT <= 27) {
            val tempView = View(activity)
            tempView.setBackgroundColor(Color.WHITE)
            tempView.elevation = convertDpToPx(4).toFloat()
            binding.blogFragmentRoot.addView(tempView)
            val params = tempView.layoutParams as CoordinatorLayout.LayoutParams
            params.gravity = Gravity.BOTTOM
            params.height = viewModel.windowInsets.value!!.second
            tempView.layoutParams = params
        }

        if (postId == null) {
            post = arguments?.getParcelable(ARG_POST)
            setPost()
            initBlogItems(post!!)
        } else {
            viewModel.getCachedPost(postId ?: "").observe(viewLifecycleOwner) { p0 ->
                if (p0 != null) {
                    post = p0
                    setPost()
                    initBlogItems(post!!)
                } else {
                    viewModel.getPost(postId ?: "")
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity.mainBinding.primaryMenuBtn.isVisible = false
        /* activity.mainBinding.primaryMenuBtn.setOnClickListener {
            val user = viewModel.user.value
            if (user != null) {

                viewModel.joinProject(post!!)

                if (user.id != post!!.uid) {
                    when {
                        user.userPrivate.collaborationIds.contains(post!!.id) -> {
                            activity.mainBinding.primaryMenuBtn.visibility = View.GONE
                        }
                        user.userPrivate.activeRequests.contains(post!!.id) -> {
                            activity.mainBinding.primaryMenuBtn.isEnabled = false
                        }
                        else -> {
                            activity.mainBinding.primaryMenuBtn.visibility = View.VISIBLE
                            activity.mainBinding.primaryMenuBtn.isEnabled = true
                        }
                    }
                } else {
                    activity.mainBinding.primaryMenuBtn.visibility = View.GONE
                }

            } else {
                activity.showSignInDialog(POST)
            }
        }*/
    }

    private fun initBlogItems(post: Post) {
        viewModel.extras[ProjectFragment.ARG_POST] = post
        activity.setFragmentTitle(post.title)

        val titleEditable = SpannableStringBuilder(post.title)

        val title = getNewTextField(activity, "Heading", titleEditable, null)
        (title.layoutParams as LinearLayout.LayoutParams).setMargins(0, convertDpToPx(4), 0, convertDpToPx(4))
        binding.blogItemContainer.addView(title)

        post.items?.forEach { item ->
            val blogItem = BlogItemConverter(item)
            val child = if (blogItem.type != "Image") {
                val e = SpannableStringBuilder(blogItem.content)

                val spans = mutableListOf<SpanItem>()

                for (span in blogItem.spans) {
                    val sp = when (span.span) {
                        "BOLD" -> {
                            StyleSpan(Typeface.BOLD)
                        }
                        "ITALIC" -> {
                            StyleSpan(Typeface.ITALIC)
                        }
                        "UNDERLINE" -> {
                            UnderlineSpan()
                        }
                        "STRIKETHROUGH" -> {
                            StrikethroughSpan()
                        }
                        else -> {
                            StyleSpan(Typeface.BOLD)
                        }
                    }
                    spans.add(SpanItem(sp, span.start, span.end))
                }

                if (spans.isNotEmpty()) {
                    for (i in spans.indices) {
                        e.setSpan(spans[i].span, spans[i].start, spans[i].end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }

                getNewTextField(activity, blogItem.type, e, null)
            } else {
                getNewImage(activity, blogItem.content)
            }
            (child.layoutParams as LinearLayout.LayoutParams).setMargins(0, convertDpToPx(4), 0, convertDpToPx(4))
            binding.blogItemContainer.addView(child)
        }

        val name = post.admin.name
        val photo = post.admin.photo

        if (Build.VERSION.SDK_INT <= 27) {
            binding.blogMetadata.authorName.setTextColor(ContextCompat.getColor(activity, R.color.black))
        }


        binding.blogMetadata.adminPhoto.setImageURI(photo)
        binding.blogMetadata.authorName.text = name

        binding.blogMetadata.adminPhoto.setOnClickListener {
            activity.toFragment(ProfileFragment.newInstance(user = post.admin), ProfileFragment.TAG)
        }

        binding.blogMetadata.authorName.setOnClickListener {
            activity.toFragment(ProfileFragment.newInstance(user = post.admin), ProfileFragment.TAG)
        }

        binding.blogScroller.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > viewModel.windowInsets.value!!.first + convertDpToPx(56) + binding.blogItemContainer.getChildAt(0).measuredHeight / 2) {
                activity.mainBinding.apply {
                    primaryAppBar.apply {
                        visibility = View.VISIBLE
                        alpha = 1f
                    }
                    topDivider.apply {
                        visibility = View.VISIBLE
                        alpha = 1f
                    }
                    primaryToolbar.visibility = View.VISIBLE
                }
            } else {
                activity.mainBinding.apply {
                    primaryAppBar.apply {
                        visibility = View.INVISIBLE
                        alpha = 0f
                    }
                    topDivider.apply {
                        visibility = View.INVISIBLE
                        alpha = 0f
                    }
                    primaryToolbar.visibility = View.GONE
                }
            }
        }

    }

    // preparing for next try
    private fun getNewTextField(context: Context, type: String, initialText: Editable?, hintText: String?) : TextView {
        val textView = TextView(context)
        textView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        textView.apply {
            minHeight = convertDpToPx(48)
            setPadding(convertDpToPx(12), 0, convertDpToPx(12), 0)
            isSingleLine = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            text = initialText
            hint = hintText
            gravity = Gravity.CENTER_VERTICAL

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (activity.resources?.configuration?.isNightModeActive == true) {
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                } else {
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                }
            } else {
                if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                } else {
                    setTextColor(ContextCompat.getColor(context, R.color.black))
                }
            }

        }

        setEditTextType(textView, type)

        return textView
    }
    private fun setEditTextType(textView: TextView, type: String) {
        when (type) {
            "Heading" -> {
                textView.textSize = convertDpToPx(13f)
                textView.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textView.background = null
                textView.setBackgroundColor(Color.TRANSPARENT)
            }
            "SubHeading" -> {
                textView.textSize = convertDpToPx(10f)
                textView.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textView.background = null
                textView.setBackgroundColor(Color.TRANSPARENT)
            }
            "Paragraph" -> {
                textView.textSize = convertDpToPx(6f)
                textView.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textView.background = null
                textView.setBackgroundColor(Color.TRANSPARENT)
            }
            "Code" -> {
                textView.textSize = convertDpToPx(5f)
                textView.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textView.background = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (activity.resources?.configuration?.isNightModeActive == true) {
                        textView.setBackgroundColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                    } else {
                        textView.setBackgroundColor(ContextCompat.getColor(activity, R.color.grey))
                    }
                } else {
                    if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                        textView.setBackgroundColor(ContextCompat.getColor(activity, R.color.darkestGrey))
                    } else {
                        textView.setBackgroundColor(ContextCompat.getColor(activity, R.color.grey))
                    }
                }


                textView.setPadding(convertDpToPx(16), convertDpToPx(12), convertDpToPx(16), convertDpToPx(12))
            }
            "Image" -> {
                throw Exception("Cannot create edittext of type image")
            }
            "Quote" -> {
                textView.textSize = convertDpToPx(7f)
                textView.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (activity.resources?.configuration?.isNightModeActive == true) {
                        textView.background = ContextCompat.getDrawable(textView.context, R.drawable.quote_background_night)
                    } else {
                        textView.background = ContextCompat.getDrawable(textView.context, R.drawable.quote_background)
                    }
                } else {
                    if (activity.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                        textView.background = ContextCompat.getDrawable(textView.context, R.drawable.quote_background_night)
                    } else {
                        textView.background = ContextCompat.getDrawable(textView.context, R.drawable.quote_background)
                    }
                }
            }
        }
    }
    private fun getNewImage(context: Context, imgContent: String?): SimpleDraweeView {
        val img = SimpleDraweeView(context)
        img.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            convertDpToPx(300)
        )
        img.isLongClickable = true
        img.isClickable = true
        img.adjustViewBounds = true
        img.setImageURI(imgContent)

        return img
    }

    override fun onDestroy() {
        super.onDestroy()
        time = System.currentTimeMillis() - time
        if (time > IS_INTERESTED_DURATION) {
            viewModel.increaseProjectWeight(post!!)
        }
    }

    companion object {

        const val TAG = "BlogFragment"
        const val ARG_POST = "ARG_POST"
        const val ARG_POST_ID = "ARG_POST_ID"
        const val TITLE = ""

        @JvmStatic
        fun newInstance(id: String? = null, post: Post? = null) = BlogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_POST, post)
                putString(ARG_POST_ID, id)
            }
        }
    }

}