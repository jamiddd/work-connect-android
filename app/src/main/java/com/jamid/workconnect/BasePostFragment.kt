package com.jamid.workconnect

import android.os.Build
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.adapter.paging3.CommentsPagingAdapter
import com.jamid.workconnect.databinding.PostMetaLayoutBinding
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.model.Post
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BasePostFragment(@LayoutRes layout: Int, tag: String, isPrimaryFragment: Boolean): SupportFragment(
    layout
) {

    val auth = Firebase.auth

    private var behavior: BottomSheetBehavior<MaterialCardView>? = null
    private var commentsAdapter: CommentsPagingAdapter? = null
    private lateinit var postItemClickListener: PostItemClickListener
    lateinit var bottomBinding: PostMetaLayoutBinding
    var post: Post? = null
    var postId: String? = null
    var job: Job? = null

    private fun getComments(commentChannelId: String) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getComments(commentChannelId).collectLatest {
                commentsAdapter?.submitData(it)
            }
        }
    }

    fun initLayoutChanges(scroller: NestedScrollView) {
        postItemClickListener = activity
        viewModel.windowInsets.observe(viewLifecycleOwner) { insets ->
            if (insets != null) {
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
        val bottomCardView = activity.findViewById<MaterialCardView>(R.id.projectData) ?: activity.findViewById(R.id.blogData)
        behavior = BottomSheetBehavior.from(bottomCardView)

        behavior?.peekHeight = convertDpToPx(164)
        behavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        behavior?.isHideable = false

        bottomBinding.commentsHeader.text = getString(R.string.comments)
        bottomBinding.commentsHeader.visibility = View.INVISIBLE

        behavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomBinding.commentsHeader.visibility = View.INVISIBLE
                } else {
                    if (bottomBinding.commentsRecyclerAlt.adapter != null) {
                        bottomBinding.commentsHeader.visibility = View.VISIBLE
                        /*commentsAdapter = CommentsPagingAdapter()
                        bottomBinding.commentsRecyclerAlt.apply {
                            adapter = commentsAdapter
                            layoutManager = LinearLayoutManager(activity)
                        }

                        getComments(post!!.commentChannelId)*/
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                ///
            }

        })

        setLikeButton()
        setDislikeButton()
        setCommentButton()
        setSaveButton()
        setMetaText()

        bottomBinding.commentsRecyclerAlt.overScrollMode = OVER_SCROLL_NEVER

    }

    private fun setCommentButton() {

        bottomBinding.postMetaCommentBtn.setOnClickListener {

            behavior?.state = if (behavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                BottomSheetBehavior.STATE_COLLAPSED
            } else {
                BottomSheetBehavior.STATE_EXPANDED
            }

            if (bottomBinding.commentsRecyclerAlt.adapter == null) {

                commentsAdapter = CommentsPagingAdapter()

                bottomBinding.commentsRecyclerAlt.apply {
                    adapter = commentsAdapter
                    layoutManager = LinearLayoutManager(activity)
                }

                getComments(post!!.commentChannelId)
            }
        }
    }

    private fun setSaveButton() {
        bottomBinding.postMetaSaveBtn.isSelected = post!!.postLocalData.isSaved

        bottomBinding.postMetaSaveBtn.setOnClickListener {
            if (auth.currentUser != null) {
                bottomBinding.postMetaSaveBtn.isSelected = !bottomBinding.postMetaSaveBtn.isSelected
                viewLifecycleOwner.lifecycleScope.launch {
                    post = postItemClickListener.onSavePressed(post!!)
                }
            } else {
                postItemClickListener.onNotSignedIn(post!!)
            }
        }
    }

    /*private fun setFollowButton() {
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
                } else {
                    postItemClickListener.onNotSignedIn(post!!)
                }
            }
        }
    }*/

    private fun setDislikeButton() {
        bottomBinding.postMetaDislikeBtn.isSelected = post!!.postLocalData.isDisliked

        bottomBinding.postMetaDislikeBtn.setOnClickListener {
            // the state before changes
            if (auth.currentUser != null) {
                bottomBinding.postMetaDislikeBtn.isSelected = !bottomBinding.postMetaDislikeBtn.isSelected
                if (post!!.postLocalData.isLiked) {
                    bottomBinding.postMetaLikeBtn.isSelected = false
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    post = postItemClickListener.onDislikePressed(post!!)
                }

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
                viewLifecycleOwner.lifecycleScope.launch {
                    post = postItemClickListener.onLikePressed(post!!)
                }
                setMetaText()
            } else {
                postItemClickListener.onNotSignedIn(post!!)
            }
        }
    }

    private fun setMetaText() {
        val post = post
        if (post != null) {
            val likesDislikesText = if (post.type == PROJECT) {
                "${post.likes} Likes • ${post.commentCount} Comments • ${post.dislikes} Dislikes • ${post.contributors?.size ?: 0} Contributors"
            } else {
                "${post.likes} Likes • ${post.commentCount} Comments • ${post.dislikes} Dislikes"
            }

            if (Build.VERSION.SDK_INT <= 27) {
                bottomBinding.likeDislikeText.setTextColor(ContextCompat.getColor(activity, R.color.black))
            }

            bottomBinding.likeDislikeText.text = likesDislikesText
        }
    }

}