package com.jamid.workconnect

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.databinding.FragmentBlogBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.profile.ProfileFragment
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

class BlogFragment : BasePostFragment(R.layout.fragment_blog) {

    private lateinit var binding: FragmentBlogBinding
    private val db = Firebase.firestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentBlogBinding.bind(view)

        val postObj = arguments?.getParcelable<Post>(ARG_POST)
        val postId = arguments?.getString(ARG_POST_ID)

        hideKeyboard()

        postObj?.let {
            setMetadata(it, binding.blogMetadata, binding.blogBottomBlur, binding.blogScroller)
        }


        if (postId != null) {
            db.collection(POSTS).document(postId)
                .get()
                .addOnSuccessListener {
                    if (it != null && it.exists()) {
                        val p = it.toObject(Post::class.java)!!
                        setMetadata(p, binding.blogMetadata, binding.blogBottomBlur, binding.blogScroller)
                        initEverything()
                    }
                }.addOnFailureListener {

                }
        } else {
            initEverything()
        }

    }

    private fun initEverything() {

        val titleEditable = SpannableStringBuilder(post.title)

        val title = getNewTextField(activity, "Heading", titleEditable, null)
        (title.layoutParams as LinearLayout.LayoutParams).setMargins(0, convertDpToPx(4), 0, convertDpToPx(4))
        binding.blogItemContainer.addView(title)

        OverScrollDecoratorHelper.setUpOverScroll(binding.blogScroller)

        post.items?.forEach { blogItem ->
            val child = if (blogItem.type != "Image") {
                val e = SpannableStringBuilder(blogItem.content)

                val listOfSpans = arrayListOf<CharacterStyle>()

                for (spanText in blogItem.spans) {
                    val span = when (spanText) {
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
                    listOfSpans.add(span)
                }

                val spanRangesStart = blogItem.spanRangesStart
                val spanRangesEnd = blogItem.spanRangesEnd

                for (i in listOfSpans.indices) {
                    e.setSpan(listOfSpans[i], spanRangesStart[i], spanRangesEnd[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                getNewTextField(activity, blogItem.type, e, blogItem.hint)
            } else {
                getNewImage(activity, blogItem.content)
            }
            (child.layoutParams as LinearLayout.LayoutParams).setMargins(0, convertDpToPx(4), 0, convertDpToPx(4))
            binding.blogItemContainer.addView(child)
        }

        val name = post.admin["name"] as String
        val photo = post.admin["photo"] as String?

        binding.blogMetadata.adminPhoto.setImageURI(photo)

        // OBSERVE FOR SIGN IN CHANGES
        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.id == post.uid) {
                    binding.blogMetadata.adminFollowBtn.visibility = View.GONE
                    binding.blogMetadata.authorName.text = name
                } else {
                    binding.blogMetadata.adminFollowBtn.visibility = View.VISIBLE
                    binding.blogMetadata.authorName.text = "$name •"
                }
            }
        }

        binding.blogMetadata.adminPhoto.setOnClickListener {
            toProfileFragment()
        }

        binding.blogMetadata.authorName.setOnClickListener {
            toProfileFragment()
        }
    }

    private fun toProfileFragment() {
        val bundle = Bundle().apply {
            putString(ProfileFragment.ARG_UID, post.uid)
        }
        findNavController().navigate(R.id.profileFragment, bundle)
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
            setTextColor(ContextCompat.getColor(context, R.color.black))
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
                textView.textSize = convertDpToPx(7f)
                textView.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textView.background = null
                textView.setBackgroundColor(Color.TRANSPARENT)
            }
            "Code" -> {
                textView.textSize = convertDpToPx(5f)
                textView.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textView.background = null
                textView.setBackgroundColor(Color.parseColor("#F1F1F1"))
                textView.setPadding(convertDpToPx(16), convertDpToPx(12), convertDpToPx(16), convertDpToPx(12))
            }
            "Image" -> {
                throw Exception("Cannot create edittext of type image")
            }
            "Quote" -> {
                textView.textSize = convertDpToPx(7f)
                textView.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                textView.background = ContextCompat.getDrawable(textView.context, R.drawable.quote_background)
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

    companion object {

        const val TAG = "BlogFragment"
        const val ARG_POST = "ARG_POST"
        const val ARG_POST_ID = "ARG_POST_ID"

        @JvmStatic
        fun newInstance(id: String? = null, post: Post? = null) = BlogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_POST, post)
                putString(ARG_POST_ID, id)
            }
        }
    }
}