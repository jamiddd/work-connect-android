package com.jamid.workconnect

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import androidx.annotation.LayoutRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.github.mmin18.widget.RealtimeBlurView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.databinding.PostMetaLayoutBinding
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.model.Post

abstract class BasePostFragment(@LayoutRes layout: Int, tag: String, isPrimaryFragment: Boolean): SupportFragment(layout, tag, isPrimaryFragment) {

    val auth = Firebase.auth
    private lateinit var behavior: BottomSheetBehavior<MaterialCardView>
    private lateinit var postItemClickListener: PostItemClickListener
    lateinit var bottomBinding: PostMetaLayoutBinding
    var post: Post? = null
    var postId: String? = null

    fun initLayoutChanges(bottomBlur: RealtimeBlurView, scroller: NestedScrollView) {
        postItemClickListener = activity
        viewModel.windowInsets.observe(viewLifecycleOwner) { insets ->
            if (insets != null) {
                val statusBarHeight = insets.first
                val navBarHeight = insets.second

                bottomBinding.postMetaRoot.setPadding(0, 0, 0, navBarHeight + 8)
                behavior.setPeekHeight(navBarHeight + convertDpToPx(70), true)

                val params = bottomBlur.layoutParams as CoordinatorLayout.LayoutParams
                params.height = navBarHeight
                params.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
                bottomBlur.layoutParams = params

//                scroller.setPadding(0, statusBarHeight + convertDpToPx(56), 0, navBarHeight + convertDpToPx(100))

                if (post?.type == PROJECT) {
                    scroller.setPadding(0, 0, 0, navBarHeight + convertDpToPx(100))
                } else {
                    scroller.setPadding(0, statusBarHeight + convertDpToPx(64), 0, navBarHeight + convertDpToPx(100))
                }

                val exitButton = activity.findViewById<Button>(R.id.exitBlogBtn) ?: activity.findViewById(R.id.exitProjectBtn)
                exitButton?.updateLayout(marginTop = statusBarHeight + convertDpToPx(8), marginLeft = convertDpToPx(8))

                exitButton.setOnClickListener {
                    activity.onBackPressed()
                }

                activity.findViewById<Button>(R.id.projectJoinBtn)
                    ?.updateLayout(marginTop = statusBarHeight + convertDpToPx(8), marginRight = convertDpToPx(8))

            }
        }
        val parent = bottomBinding.root.parent as MaterialCardView
        behavior = BottomSheetBehavior.from(parent)
//        OverScrollDecoratorHelper.setUpOverScroll(scroller)

        parent.setOnClickListener {
            if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED || behavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                /*postMetaShowBtn.rotation = 180f*/
            } else {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                /*postMetaShowBtn.rotation = 0f*/
            }
        }

    }

    fun setPost() {
        setLikeButton()
        setDislikeButton()
        setFollowButton()
        setSaveButton()
        setMetaText()
    }

    private fun setSaveButton() {
        bottomBinding.postMetaSaveBtn.isSelected = post!!.postLocalData.isSaved

        bottomBinding.postMetaSaveBtn.setOnClickListener {
            if (auth.currentUser != null) {
                bottomBinding.postMetaSaveBtn.isSelected = !bottomBinding.postMetaSaveBtn.isSelected
                post = postItemClickListener.onSavePressed(post!!)
                activity.currentViewHolder?.bind(post)
            } else {
                postItemClickListener.onNotSignedIn(post!!)
            }
        }
    }

    private fun setFollowButton() {
        fun setFollowText(isUserFollowed: Boolean) {
            if (isUserFollowed) {
                bottomBinding.adminFollowBtn.text = getString(R.string.unfollow_text)
            } else {
                bottomBinding.adminFollowBtn.text = getString(R.string.follow_text)
            }
        }

        if (post!!.postLocalData.isCreator) {
            bottomBinding.adminFollowBtn.visibility = View.GONE
        } else {

            bottomBinding.adminFollowBtn.visibility = View.VISIBLE

            setFollowText(post!!.postLocalData.isUserFollowed)

            bottomBinding.adminFollowBtn.setOnClickListener {
                if (auth.currentUser != null) {
                    setFollowText(!post!!.postLocalData.isUserFollowed)
                    post = postItemClickListener.onFollowPressed(post!!)
                    activity.currentViewHolder?.bind(post)
                } else {
                    postItemClickListener.onNotSignedIn(post!!)
                }
            }
        }
    }

    private fun setDislikeButton() {
        bottomBinding.postMetaDislikeBtn.isSelected = post!!.postLocalData.isDisliked

        bottomBinding.postMetaDislikeBtn.setOnClickListener {
            // the state before changes
            if (auth.currentUser != null) {
                bottomBinding.postMetaDislikeBtn.isSelected = !bottomBinding.postMetaDislikeBtn.isSelected
                if (post!!.postLocalData.isLiked) {
                    bottomBinding.postMetaLikeBtn.isSelected = false
                }
                post = postItemClickListener.onDislikePressed(post!!)
                activity.currentViewHolder?.bind(post)
                setMetaText()
            } else {
                postItemClickListener.onNotSignedIn(post!!)
            }
        }
    }

    private fun setLikeButton() {
        bottomBinding.postMetaLikeBtn.isSelected = post!!.postLocalData.isLiked

        bottomBinding.postMetaLikeBtn.setOnClickListener {
            // the state before changes
            if (auth.currentUser != null) {
                bottomBinding.postMetaLikeBtn.isSelected = !bottomBinding.postMetaLikeBtn.isSelected
                if (post!!.postLocalData.isDisliked) {
                    bottomBinding.postMetaDislikeBtn.isSelected = false
                }
                post = postItemClickListener.onLikePressed(post!!)
                activity.currentViewHolder?.bind(post)
                setMetaText()
            } else {
                postItemClickListener.onNotSignedIn(post!!)
            }
        }
    }

    private fun setMetaText() {
        val post = post
        if (post != null) {
            val likesDislikesText = "${post.likes} Likes • ${post.dislikes} Dislikes"
            val sp = SpannableString(likesDislikesText)
            sp.setSpan(StyleSpan(Typeface.BOLD), 0, 0 + post.likes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(StyleSpan(Typeface.BOLD), post.likes.toString().length + 9, post.likes.toString().length + 9 + post.dislikes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (Build.VERSION.SDK_INT <= 27) {
                bottomBinding.postMetaText.setTextColor(ContextCompat.getColor(activity, R.color.black))
            }

            bottomBinding.postMetaText.text = sp
        }
    }

}