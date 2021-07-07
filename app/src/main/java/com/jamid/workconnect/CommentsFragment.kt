package com.jamid.workconnect

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.jamid.workconnect.adapter.paging3.CommentsPagingAdapter
import com.jamid.workconnect.databinding.FragmentCommentsBinding
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleComment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommentsFragment : PagingListFragment(R.layout.fragment_comments) {

    private lateinit var binding: FragmentCommentsBinding
    private lateinit var commentsAdapter: CommentsPagingAdapter

    private fun getComments(commentChannelId: String) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getComments(commentChannelId).collectLatest {
                commentsAdapter.submitData(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCommentsBinding.bind(view)
        val post = arguments?.getParcelable<Post>(ARG_POST) ?: return
        val isHalfFragment = arguments?.getBoolean(ARG_IS_HALF) ?: false

        if (isHalfFragment) {
            binding.commentsAppBar.visibility = View.GONE

            val params = binding.commentsRefresher.layoutParams as CoordinatorLayout.LayoutParams
            params.behavior = null
            binding.commentsRefresher.layoutParams = params
        }

        commentsAdapter = CommentsPagingAdapter()
        binding.commentsRecycler.setListAdapter(pagingAdapter = commentsAdapter, clazz = SimpleComment::class.java, onComplete = {
            getComments(post.commentChannelId)
        }, onEmptySet = {
            Toast.makeText(activity, "No Comments", Toast.LENGTH_SHORT).show()
        })

        binding.commentsRefresher.setSwipeRefresher {
            getComments(post.commentChannelId)
            stopRefreshProgress(it)
        }

        binding.commentText.doAfterTextChanged {
            binding.sendCommentBtn.isEnabled = !it.isNullOrBlank()
        }

        binding.commentsToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.user.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.commentsBottomLayout.visibility = View.VISIBLE
                binding.userImage.setImageURI(it.photo)
                activity.mainBinding.bottomNavBackground.visibility = View.GONE
            } else {
                binding.commentsBottomLayout.visibility = View.GONE
                activity.mainBinding.bottomNavBackground.visibility = View.VISIBLE
            }
        }

        viewModel.commentSentResult.observe(viewLifecycleOwner) {
            val commentSentResult = it ?: return@observe

            when (commentSentResult) {
                is Result.Error -> {
                    Toast.makeText(activity, "Something went wrong. Couldn't send comment. Reason - " + commentSentResult.exception.localizedMessage, Toast.LENGTH_SHORT).show()
                }
                is Result.Success -> {
                    getComments(post.commentChannelId!!)
                    Toast.makeText(activity, "Posted comment", Toast.LENGTH_SHORT).show()
                }
            }

            viewModel.commentSentResult.postValue(null)
        }

        viewModel.windowInsets.observe(viewLifecycleOwner) { (top, bottom) ->

            binding.commentsToolbar.updateLayout(marginTop = top)

            val user = viewModel.user.value
            if (user != null) {
                binding.commentsRecycler.setPadding(0, 0, 0, binding.commentsBottomLayout.measuredHeight)
            } else {
                binding.commentsRecycler.setPadding(0, 0, 0, bottom)
            }

            binding.commentText.updateLayout(marginLeft = convertDpToPx(8), marginTop = convertDpToPx(8), marginRight = convertDpToPx(4), marginBottom = bottom + convertDpToPx(8))
        }

        viewModel.replyJobStarted.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.sampleReply.visibility = View.VISIBLE
                setUpReplyComment(it)
                binding.sendCommentBtn.setOnClickListener { _ ->
                    val commentText = binding.commentText.text.toString()
                    viewModel.sendCommentReply(it, commentText)
                    binding.commentText.text.clear()
                    viewModel.replyJobStarted.postValue(null)
                    getComments(post.commentChannelId)
                }
            } else {
                binding.sampleReply.visibility = View.GONE
                binding.sendCommentBtn.setOnClickListener {
                    val comment = binding.commentText.text
                    viewModel.sendComment(post, comment.toString())
                    binding.commentText.text.clear()
                }
            }
        }

    }

    private fun setUpReplyComment(comment: SimpleComment) {

        binding.commentText.requestFocus()
        showKeyboard()

        binding.sampleReplyComment.apply {
            commentUserName.text = comment.sender.name
            commentUserImg.setImageURI(comment.sender.photo)
            commentText.maxLines = 2
            commentPostedAt.visibility = View.GONE
            commentText.text = comment.commentContent
            like.visibility = View.GONE
            reply.visibility = View.GONE

            val params = commentText.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = R.id.comment_user_name
            params.endToEnd = R.id.comment_root
            params.topToBottom = R.id.comment_user_name
            params.bottomToBottom = R.id.comment_root
            params.setMargins(0, 0, convertDpToPx(8), convertDpToPx(8))
            commentText.layoutParams = params
        }

        binding.cancelReplyBtn.setOnClickListener {
            viewModel.replyJobStarted.postValue(null)
        }

    }

    companion object {

        const val ARG_POST = "ARG_POST"
        const val ARG_IS_HALF = "ARG_IS_HALF"
        const val TAG = "CommentsFragment"

        @JvmStatic
        fun newInstance(post: Post, isHalfFragment: Boolean = false) = CommentsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_POST, post)
                putBoolean(ARG_IS_HALF, isHalfFragment)
            }
        }
    }
}