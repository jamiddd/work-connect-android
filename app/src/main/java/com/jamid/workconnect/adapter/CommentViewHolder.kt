package com.jamid.workconnect.adapter

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.IS_MINIMIZED
import com.jamid.workconnect.R
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.getTextForTime
import com.jamid.workconnect.interfaces.CommentClickListener
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SimpleComment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CommentViewHolder(private val parent: ViewGroup, layout: Int, val post: Post? = null, private val isMinimized: Boolean = true): GenericViewHolder<SimpleComment>(parent, layout){

    private val commentClickListener = parent.context as CommentClickListener

    override fun bind(item: SimpleComment) {
        val scope = parent.findViewTreeLifecycleOwner()?.lifecycleScope ?: itemView.findViewTreeLifecycleOwner()?.lifecycleScope ?: return
        val userImage = itemView.findViewById<SimpleDraweeView>(R.id.comment_user_img)!!
        val name = itemView.findViewById<TextView>(R.id.comment_user_name)!!
        val comment = itemView.findViewById<TextView>(R.id.comment_text)!!
        val time = itemView.findViewById<TextView>(R.id.comment_posted_at)!!
        val likeBtn = itemView.findViewById<MaterialButton>(R.id.like)!!
        val replyBtn = itemView.findViewById<MaterialButton>(R.id.reply)!!
        val recycler = itemView.findViewById<RecyclerView>(R.id.repliesRecycler)!!

        if (Build.VERSION.SDK_INT <= 27) {
            val colorBlack = ContextCompat.getColor(itemView.context, R.color.black)
            comment.setTextColor(colorBlack)
            name.setTextColor(colorBlack)
        }

        post?.let {
            itemView.setOnClickListener { _ ->
                commentClickListener.onCommentClick(it, item)
            }
        }

        if (item.repliesCount > 0 && item.commentLevel < 2) {
            itemView.findViewById<View>(R.id.threadLine)?.visibility = View.VISIBLE

            scope.launch {
                val comments = commentClickListener.onFetchReplies(item)
                if (comments.isNotEmpty()) {
                    recycler.visibility = View.VISIBLE
                    val commentsAdapter = GenericAdapter(SimpleComment::class.java, mapOf(IS_MINIMIZED to isMinimized))

                    recycler.apply {
                        adapter = commentsAdapter
                        layoutManager = LinearLayoutManager(itemView.context)
                    }

                    commentsAdapter.submitList(comments)
                } else {
                    recycler.visibility = View.GONE
                }
            }
        }

        if (isMinimized) {
            likeBtn.visibility = View.GONE
            replyBtn.visibility = View.GONE
            time.visibility = View.GONE
            val params = comment.layoutParams as ConstraintLayout.LayoutParams
            params.startToStart = R.id.comment_user_name
            params.endToEnd = R.id.comment_root
            params.topToBottom = R.id.comment_user_name
            params.bottomToBottom = R.id.comment_root
            params.setMargins(0, 0, convertDpToPx(8, itemView.context), convertDpToPx(8, itemView.context))
            comment.layoutParams = params
        } else {
            val metaText = getTextForTime(item.postedAt) + " • ${item.likes} Likes • ${item.repliesCount} Replies"
            time.text = metaText
            time.visibility = View.VISIBLE
            likeBtn.visibility = View.VISIBLE
            replyBtn.visibility = View.VISIBLE
            setLikeButton(item, likeBtn, scope)
        }

        scope.launch {
            commentClickListener.onFetchUserData(item)?.let {
                userImage.setImageURI(it.photo)
                name.text = it.name
                item.sender = it
                setReplyButton(item, replyBtn)

                userImage.setOnClickListener {
                    commentClickListener.onCommentUserClick(item)
                }
                name.setOnClickListener {
                    commentClickListener.onCommentUserClick(item)
                }

                itemView.setOnLongClickListener {
                    commentClickListener.onCommentOptionClick(item)
                    true
                }
            }
        }

        comment.text = item.commentContent

        if (Firebase.auth.currentUser == null) {
            replyBtn.visibility = View.GONE
            likeBtn.visibility = View.GONE
        }

    }

    private var likeJob: Job? = null
    private var replyJob: Job? = null

    private fun setLikeButton(item: SimpleComment, likeBtn: MaterialButton, scope: CoroutineScope) {
        likeBtn.isSelected = item.isLiked

        likeBtn.setOnClickListener {
            likeJob?.cancel()
            likeJob = scope.launch {
                commentClickListener.onCommentLikeClick(item)?.let {
                    setLikeButton(it, likeBtn, scope)
                }
            }
        }
    }

    private fun setReplyButton(item: SimpleComment, replyBtn: MaterialButton) {
        replyBtn.setOnClickListener {
            replyJob?.cancel()
            replyJob = itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                commentClickListener.onCommentReplyClick(item)?.let {
                    setReplyButton(it, replyBtn)
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(parent: ViewGroup, layout: Int, post: Post?, isMinimized: Boolean = true) =
            CommentViewHolder(parent, layout, post, isMinimized)

    }
}