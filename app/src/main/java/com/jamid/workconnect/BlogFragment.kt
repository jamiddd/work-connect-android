package com.jamid.workconnect

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.chip.Chip
import com.jamid.workconnect.databinding.BlogExtraLayoutBinding
import com.jamid.workconnect.databinding.FragmentBlogBinding
import com.jamid.workconnect.interfaces.OnChipClickListener
import com.jamid.workconnect.model.BlogItemConverter
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SpanItem
import com.jamid.workconnect.profile.ProfileFragment
import com.jamid.workconnect.views.zoomable.ImageControllerListener
import com.jamid.workconnect.views.zoomable.ImageViewFragment


class BlogFragment : BasePostFragment(R.layout.fragment_blog, TAG, false) {

    private lateinit var binding: FragmentBlogBinding
    private var time: Long = 0
    private lateinit var onChipClickListener: OnChipClickListener
    private val imageControllerListenerList = mutableMapOf<String, ImageControllerListener>()

    @SuppressLint("Recycle")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentBlogBinding.bind(view)

        time = System.currentTimeMillis()
        postId = arguments?.getString(ARG_POST_ID)
        bottomBinding = binding.blogMetadata
        initLayoutChanges(binding.blogScroller)

        onChipClickListener = activity

        binding.blogFragmentToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        hideKeyboard()

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


    private fun initBlogItems(post: Post) {

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, _) ->
            binding.blogFragmentToolbar.updateLayout(marginTop = top)
        }

        viewModel.extras[ProjectFragment.ARG_POST] = post

        binding.blogMetadata.postJoinBtn.visibility = View.GONE

        val titleEditable = SpannableStringBuilder(post.title)

        val title = getNewTextField(activity, HEADING, titleEditable, null)
        (title.layoutParams as LinearLayout.LayoutParams).setMargins(0, convertDpToPx(4), 0, convertDpToPx(4))
        binding.blogItemContainer.addView(title)

        val extraView = layoutInflater.inflate(R.layout.blog_extra_layout, null)
        val extraBinding = BlogExtraLayoutBinding.bind(extraView)
        binding.blogItemContainer.addView(extraView)

        val name = post.admin.name
        val photo = post.admin.photo

        if (Build.VERSION.SDK_INT <= 27) {
            extraBinding.blogAuthorName.setTextColor(ContextCompat.getColor(activity, R.color.black))
        }

        extraBinding.blogAuthorImg.setImageURI(photo)
        extraBinding.blogAuthorName.text = name

        val options = navOptions {
            anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
                popEnter = R.anim.slide_in_left
                popExit = R.anim.slide_out_right
            }
            popUpTo(findNavController().findDestination(R.id.blogFragment)!!.id) {
                saveState = true
            }
            restoreState = true
        }

        extraBinding.blogAuthorImg.setOnClickListener {
            findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, post.admin) }, options)
//            activity.toFragment(ProfileFragment.newInstance(user = post.admin), ProfileFragment.TAG)
        }

        extraBinding.blogAuthorName.setOnClickListener {
            findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, post.admin) }, options)
//            activity.toFragment(ProfileFragment.newInstance(user = post.admin), ProfileFragment.TAG)
        }

        binding.blogAuthorImage.setImageURI(post.admin.photo)
        binding.blogFragmentToolbar.title = ""
        binding.blogFragmentToolbar.subtitle = ""

        binding.blogAuthorImage.setOnClickListener {
            findNavController().navigate(R.id.profileFragment, Bundle().apply { putParcelable(ProfileFragment.ARG_USER, post.admin) }, options)
        }

        binding.blogScroller.setOnScrollChangeListener { _, _, scrollY, _, _ ->

            if (scrollY > binding.blogItemContainer.getChildAt(0).measuredHeight) {
                binding.blogFragmentToolbar.title = post.title
            } else {
                binding.blogFragmentToolbar.title = ""
            }

            if (scrollY > binding.blogItemContainer.getChildAt(0).measuredHeight + binding.blogItemContainer.getChildAt(1).measuredHeight) {
                binding.blogFragmentToolbar.subtitle = "- ${post.admin.name}"
                binding.blogAuthorImage.visibility = View.VISIBLE
            } else {
                binding.blogFragmentToolbar.subtitle = ""
                binding.blogAuthorImage.visibility = View.GONE
            }

        }

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

            if (child is SimpleDraweeView) {

                child.setOnClickListener {
                    val extras = FragmentNavigatorExtras(child to blogItem.content)
                    val bundle = Bundle().apply {
                        putString(ImageViewFragment.ARG_TRANSITION_NAME, blogItem.content)
                        putString(ImageViewFragment.ARG_IMAGE, blogItem.content)
                        putInt(ImageViewFragment.ARG_WIDTH, imageControllerListenerList[blogItem.content]!!.params.first)
                        putInt(ImageViewFragment.ARG_HEIGHT, imageControllerListenerList[blogItem.content]!!.params.second)
                    }
                    findNavController().navigate(
                        R.id.imageViewFragment,
                        bundle,
                        null,
                        extras
                    )
                }
            }
        }


      /*  binding.blogScroller.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > binding.blogItemContainer.getChildAt(0).measuredHeight) {
                activity.setFragmentTitle(post.title)
            } else {
                activity.setFragmentTitle("")
            }
        }
*/

        /*binding.blogScroller.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
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
        }*/

        for (tag in post.tags) {
            addNewChip(tag)
        }

        if (post.tags.isEmpty()) {
            binding.blogTags.visibility = View.GONE
            binding.blogTagsHeader.visibility = View.GONE
        }

    }

    private fun addNewChip(s: String) {
        val chip = LayoutInflater.from(activity).inflate(R.layout.chip, null) as Chip
        chip.text = s

        chip.isChecked = true

        chip.setOnClickListener {
            chip.isChecked = true
            onChipClickListener.onChipClick(s)
//            activity.toFragment(TagPostsFragment.newInstance(s), TagPostsFragment.TAG)
        }

        binding.blogTags.addView(chip)
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
        textView.setTextIsSelectable(true)
        setEditTextType(textView, type)
        return textView
    }

    private fun setEditTextType(textView: TextView, type: String) {

        textView.setTextIsSelectable(true)

        when (type) {
            HEADING -> {
                textView.setTextAppearance(R.style.TextAppearance_AppCompat_Display1)
                textView.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.black))
            }
            SUB_HEADING -> {
                textView.setTextAppearance(R.style.TextAppearance_AppCompat_Large)
                textView.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.black))
            }
            PARAGRAPH -> {
                textView.setTextAppearance(R.style.TextAppearance_AppCompat_Medium)
                textView.setTextColor(ContextCompat.getColor(textView.context, R.color.black))
            }
            CODE -> {
                textView.textSize = convertDpToPx(5f)
                textView.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textView.background = null
                textView.setBackgroundColor(getColorBasedOnTheme(textView.context))
                textView.setPadding(convertDpToPx(16), convertDpToPx(12), convertDpToPx(16), convertDpToPx(12))
            }
            IMAGE -> {
                throw Exception("Cannot create edittext of type image")
            }
            QUOTE -> {
                textView.setTextAppearance(R.style.TextAppearance_AppCompat_Small)
                textView.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textView.background = getDrawableBasedOnTheme(textView.context)
            }
        }
    }


    private fun getDrawableBasedOnTheme(context: Context): Drawable? {

        val quoteBackgroundNight = ContextCompat.getDrawable(context, R.drawable.quote_background_night)
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


    private fun getNewImage(context: Context, imgContent: String): SimpleDraweeView {
        val img = SimpleDraweeView(context)
        img.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            convertDpToPx(300)
        )
        img.setBackgroundColor(ContextCompat.getColor(context, R.color.grey))
        img.isLongClickable = true
        img.isClickable = true
        img.adjustViewBounds = true
        val imageControllerListener = ImageControllerListener()

        val imageRequest = ImageRequest.fromUri(imgContent)
        val controller: DraweeController = Fresco.newDraweeControllerBuilder()
            .setImageRequest(imageRequest)
            .setControllerListener(imageControllerListener)
            .build()

        imageControllerListenerList[imgContent] = imageControllerListener

        img.controller = controller

        ViewCompat.setTransitionName(img, imgContent)

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