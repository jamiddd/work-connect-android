package com.jamid.workconnect

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import androidx.annotation.LayoutRes
import androidx.core.widget.NestedScrollView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.databinding.PostMetaLayoutBinding
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.model.Post

abstract class BasePostFragment(@LayoutRes layout: Int, tag: String, isPrimaryFragment: Boolean): SupportFragment(layout, tag, isPrimaryFragment) {

    val auth = Firebase.auth
    private lateinit var postItemClickListener: PostItemClickListener
    lateinit var bottomBinding: PostMetaLayoutBinding
    var post: Post? = null
    var postId: String? = null

    fun initLayoutChanges(scroller: NestedScrollView) {
        postItemClickListener = activity
        viewModel.windowInsets.observe(viewLifecycleOwner) { insets ->
            if (insets != null) {
                val statusBarHeight = insets.first
                val navBarHeight = insets.second

                bottomBinding.postMetaRoot.setPadding(0, 0, 0, navBarHeight)

                if (post?.type == PROJECT) {
                    scroller.setPadding(0, 0, 0, navBarHeight + convertDpToPx(100))
                } else {
                    scroller.setPadding(0, convertDpToPx(8), 0, navBarHeight + convertDpToPx(100))
                }

            }
        }
    }

    fun setPost() {
        setLikeButton()
        setDislikeButton()
//        setFollowButton()
        setSaveButton()
        setMetaText()
    }

    private fun setSaveButton() {
        bottomBinding.postMetaSaveBtn.isSelected = post!!.postLocalData.isSaved

        bottomBinding.postMetaSaveBtn.setOnClickListener {
            if (auth.currentUser != null) {
                bottomBinding.postMetaSaveBtn.isSelected = !bottomBinding.postMetaSaveBtn.isSelected
                post = postItemClickListener.onSavePressed(post!!)
            } else {
                postItemClickListener.onNotSignedIn(post!!)
            }
        }
    }

    private fun setFollowButton() {
        /*fun setFollowText(isUserFollowed: Boolean) {
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
                } else {
                    postItemClickListener.onNotSignedIn(post!!)
                }
            }
        }*/
    }

    private fun setDislikeButton() {
        bottomBinding.postMetaDislikeBtn.isSelected = post!!.postLocalData.isDisliked
        bottomBinding.postMetaDislikeBtn.text = (post?.dislikes ?: 0).toString()

        bottomBinding.postMetaDislikeBtn.setOnClickListener {
            // the state before changes
            if (auth.currentUser != null) {
                bottomBinding.postMetaDislikeBtn.isSelected = !bottomBinding.postMetaDislikeBtn.isSelected
                if (post!!.postLocalData.isLiked) {
                    bottomBinding.postMetaLikeBtn.isSelected = false
                }
                post = postItemClickListener.onDislikePressed(post!!)

                bottomBinding.postMetaLikeBtn.text = (post?.likes ?: 0).toString()
                bottomBinding.postMetaDislikeBtn.text = (post?.dislikes ?: 0).toString()

            } else {
                postItemClickListener.onNotSignedIn(post!!)
            }
        }
    }

    private fun setLikeButton() {
        bottomBinding.postMetaLikeBtn.isSelected = post!!.postLocalData.isLiked
        bottomBinding.postMetaLikeBtn.text = (post?.likes ?: 0).toString()

        bottomBinding.postMetaLikeBtn.setOnClickListener {
            // the state before changes
            if (auth.currentUser != null) {
                bottomBinding.postMetaLikeBtn.isSelected = !bottomBinding.postMetaLikeBtn.isSelected
                if (post!!.postLocalData.isDisliked) {
                    bottomBinding.postMetaDislikeBtn.isSelected = false
                }
                post = postItemClickListener.onLikePressed(post!!)

                bottomBinding.postMetaLikeBtn.text = (post?.likes ?: 0).toString()
                bottomBinding.postMetaDislikeBtn.text = (post?.dislikes ?: 0).toString()

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
//                bottomBinding.postMetaText.setTextColor(ContextCompat.getColor(activity, R.color.black))
            }

//            bottomBinding.postMetaText.text = sp
        }
    }

}