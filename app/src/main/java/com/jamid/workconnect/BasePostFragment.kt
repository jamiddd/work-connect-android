package com.jamid.workconnect

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.LayoutRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.github.mmin18.widget.RealtimeBlurView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.platform.MaterialSharedAxis
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.databinding.PostMetaLayoutBinding
import com.jamid.workconnect.model.Post

abstract class BasePostFragment(@LayoutRes layout: Int): Fragment(layout) {
    
    val auth = Firebase.auth 
    val viewModel: MainViewModel by activityViewModels()
    protected lateinit var post: Post
    protected lateinit var activity: MainActivity
    protected lateinit var behavior: BottomSheetBehavior<MaterialCardView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as MainActivity
    }

    fun setMetadata(p: Post, bottomBinding: PostMetaLayoutBinding, bottomBlur: RealtimeBlurView, scroller: ScrollView) {
        post = p
        val parent = bottomBinding.root.parent as MaterialCardView
        behavior = BottomSheetBehavior.from(parent)

        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { insets ->
            if (insets != null) {
                val statusBarHeight = insets.first
                val navBarHeight = insets.second

                bottomBinding.postMetaRoot.setPadding(0, 0, 0, navBarHeight + 8)
                behavior.setPeekHeight(navBarHeight + convertDpToPx(64), true)

                val params = bottomBlur.layoutParams as CoordinatorLayout.LayoutParams
                params.height = navBarHeight
                params.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
                bottomBlur.layoutParams = params

                scroller.setPadding(0, statusBarHeight + convertDpToPx(56), 0, navBarHeight + convertDpToPx(100))

            }
        }

        parent.setOnClickListener {
            if (behavior.state == BottomSheetBehavior.STATE_COLLAPSED || behavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                /*postMetaShowBtn.rotation = 180f*/
            } else {
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                /*postMetaShowBtn.rotation = 0f*/
            }
        }


        setLikeButton(bottomBinding.postMetaLikeBtn, bottomBinding.postMetaDislikeBtn)
        setDislikeButton(bottomBinding.postMetaLikeBtn, bottomBinding.postMetaDislikeBtn)
        setSaveButton(bottomBinding.postMetaSaveBtn)
        setFollowButton(bottomBinding.adminFollowBtn)
        setMetaText(bottomBinding.postMetaText)

        viewModel.likesMap.observe(viewLifecycleOwner) { map ->
            if (map != null) {
                val isLiked = map[post.id] ?: false
                bottomBinding.postMetaLikeBtn.isSelected = isLiked
            }
        }

        viewModel.dislikesMap.observe(viewLifecycleOwner) { map ->
            if (map != null) {
                val isDisliked = map[post.id] ?: false
                bottomBinding.postMetaDislikeBtn.isSelected = isDisliked
            }
        }

        viewModel.likesCountMap.observe(viewLifecycleOwner) { map ->
            if (map != null) {
                val count = map[post.id] ?: 0
                post.likes = count
                setMetaText(bottomBinding.postMetaText)
            }
        }

        viewModel.dislikesCountMap.observe(viewLifecycleOwner) { map ->
            if (map != null) {
                val count = map[post.id] ?: 0
                post.dislikes = count
                setMetaText(bottomBinding.postMetaText)
            }
        }

        viewModel.savesMap.observe(viewLifecycleOwner) { map ->
            if (map != null) {
                val isSaved = map[post.id] ?: false
                bottomBinding.postMetaSaveBtn.isSelected = isSaved
            }
        }

        viewModel.followingsMap.observe(viewLifecycleOwner) { map ->
            if (map != null) {
                val isFollowing = map[post.uid] ?: false
                updateFollowButton(bottomBinding.adminFollowBtn, isFollowing)
            }
        }
    }

    private fun setMetaText(metaTextView: TextView) {
        val likesDislikesText = "${post.likes} Likes • ${post.dislikes} Dislikes"
        val sp = SpannableString(likesDislikesText)
        sp.setSpan(StyleSpan(Typeface.BOLD), 0, 0 + post.likes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(StyleSpan(Typeface.BOLD), post.likes.toString().length + 9, post.likes.toString().length + 9 + post.dislikes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        metaTextView.text = sp
    }

    private fun setLikeButton(likeBtn: Button, dislikeBtn: Button) {
        // set the initial state of the like button
        likeBtn.isSelected = false

        // set like button actions
        likeBtn.setOnClickListener {
            if (auth.currentUser != null) {
                if (likeBtn.isSelected) {
                    post.likes = post.likes - 1
                    viewModel.onLikePressed(post, prevL = true, prevD = false)
                } else {
                    post.likes = post.likes + 1
                    if (dislikeBtn.isSelected) {
                        dislikeBtn.isSelected = false
                        post.dislikes = post.dislikes - 1
                        viewModel.onLikePressed(post, prevL = false, prevD = true)
                    } else {
                        viewModel.onLikePressed(post, prevL = false, prevD = false)
                    }
                }
            } else {
                activity.invokeSignIn()
            }
        }
    }

    private fun setDislikeButton(likeBtn: Button, dislikeBtn: Button) {
        // set the initial state og the dislike button
        dislikeBtn.isSelected = false

        // set dislike button actions
        dislikeBtn.setOnClickListener {
            if (auth.currentUser != null) {
                if (dislikeBtn.isSelected) {
                    post.dislikes = post.dislikes - 1
                    viewModel.onDislikePressed(post, prevL = true, prevD = false)
                } else {
                    post.dislikes = post.dislikes + 1
                    if (likeBtn.isSelected) {
                        likeBtn.isSelected = false
                        post.likes = post.likes - 1
                        viewModel.onDislikePressed(post, prevL = true, prevD = false)
                    } else {
                        viewModel.onDislikePressed(post, prevL = false, prevD = false)
                    }
                }
            } else {
                activity.invokeSignIn()
            }
        }
    }

    private fun setSaveButton(saveBtn: Button) {
        // initial state of save button
        saveBtn.isSelected = false

        // save button actions
        saveBtn.setOnClickListener {
            if (auth.currentUser != null) {
                if (saveBtn.isSelected) {
                    viewModel.onSavePressed(post, true)
                } else {
                    viewModel.onSavePressed(post, false)
                }
            } else {
                activity.invokeSignIn()
            }
        }
    }

    private fun setFollowButton(followBtn: Button) {
        followBtn.isSelected = false
        followBtn.text = getString(R.string.follow_text)

        // set follow button actions
        followBtn.setOnClickListener {
            if (auth.currentUser != null) {
                if (followBtn.isSelected) {
                    viewModel.onFollowPressed(post.uid, true)
                } else {
                    viewModel.onFollowPressed(post.uid, false)
                }
            } else {
                activity.invokeSignIn()
            }
        }
    }

    private fun updateFollowButton(followBtn: Button, state: Boolean) {
        followBtn.isSelected = state
        if (state) {
            followBtn.text = getString(R.string.unfollow_text)
        } else {
            followBtn.text = getString(R.string.follow_text)
        }
    }
}