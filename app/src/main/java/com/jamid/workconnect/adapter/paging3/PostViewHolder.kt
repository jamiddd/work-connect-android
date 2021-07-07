package com.jamid.workconnect.adapter.paging3

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.POST
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.GenericAdapter
import com.jamid.workconnect.adapter.GenericViewHolder
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.model.BlogItemConverter
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.SimpleComment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PostViewHolder(
	val parent: ViewGroup,
	@LayoutRes val layout: Int,
): GenericViewHolder<Post>(parent, layout) {

	private val view = itemView
	private val postItemClickListener = view.context as PostItemClickListener
	private val auth = Firebase.auth
	private lateinit var post: Post

	override fun bind(item: Post) {
		post = item
		setStaticContent()
		setPost()
		view.setOnClickListener {
			postItemClickListener.onItemClick(post, this)
		}
		
	}

	private fun setStaticContent() {
		val title = view.findViewById<TextView>(R.id.projectTitle)
		val userImage = view.findViewById<SimpleDraweeView>(R.id.projectAdminImg)
		val userName = view.findViewById<TextView>(R.id.projectAdminName)
		val dateLocationText = view.findViewById<TextView>(R.id.projectMetaText)
		val content = view.findViewById<TextView>(R.id.projectMiniContent)

		// special cases
		val optionBtn = view.findViewById<Button>(R.id.projectOptionsBtn)


		// same for both project and blog
		title.text = post.title
		userName.text = post.admin.name
		userImage.setImageURI(post.admin.photo)

		val time = SimpleDateFormat("hh:mm a, dd MMM", Locale.UK).format(post.updatedAt)

		val metaText = if (post.location?.place == null) {
			time
		} else {
			"$time • ${post.location!!.place}"
		}

		dateLocationText.text = metaText

		// different for different types
		val projectImage = view.findViewById<SimpleDraweeView>(R.id.projectThumb)
		val blogImage = view.findViewById<SimpleDraweeView>(R.id.projectBlogImage)
		if (post.type == PROJECT) {
			blogImage.visibility = View.GONE
			content.text = post.content
			projectImage.visibility = View.VISIBLE
			projectImage.setImageURI(post.thumbnail)
		} else {
			projectImage.visibility = View.GONE
			var hasImage = false
			var firstImgPos = 0

			val items = post.items

			if (!items.isNullOrEmpty()) {
				for (i in items.indices) {
					val blogItem = BlogItemConverter(items[i])

					if (blogItem.type == IMAGE) {
						hasImage = true
						firstImgPos = i
						break
					}
				}

				if (hasImage) {
					blogImage.setImageURI(BlogItemConverter(items[firstImgPos]).content)
					blogImage.visibility = View.VISIBLE
				} else {
					blogImage.visibility = View.GONE
				}

				content.text = BlogItemConverter(items[0]).content
			}
		}

		if (Build.VERSION.SDK_INT <= 27) {
			val colorBlack = ContextCompat.getColor(view.context, R.color.black)
			userName.setTextColor(colorBlack)
			content.setTextColor(colorBlack)
			title.setTextColor(colorBlack)
		}

		optionBtn.setOnClickListener {
			postItemClickListener.onOptionClick(post)
		}

		itemView.setOnLongClickListener {
			postItemClickListener.onOptionClick(post)
			true
		}

	}

	private fun setPost() {
		val likeBtn = view.findViewById<Button>(R.id.projectLikeBtn)
		val dislikeBtn = view.findViewById<Button>(R.id.projectDislikeBtn)
		val saveBtn = view.findViewById<Button>(R.id.projectSaveBtn)
		val recycler = view.findViewById<RecyclerView>(R.id.post_comments_recycler)
		val commentBtn = view.findViewById<Button>(R.id.projectCommentBtn)

		setLikeButton(likeBtn, dislikeBtn)
		setDislikeButton(likeBtn, dislikeBtn)
		setMetadata()
		setSaveButton(saveBtn)
		setUpComments(recycler)

		commentBtn?.setOnClickListener {
			postItemClickListener.onCommentClick(post)
		}
	}

	private fun setUpComments(recycler: RecyclerView) {
		val moreComments = itemView.findViewById<TextView>(R.id.more_comments) ?: return
		when {
			post.commentCount > 2 -> moreComments.visibility = View.VISIBLE
			post.commentCount == 0.toLong() -> recycler.visibility = View.GONE
			else -> moreComments.visibility = View.GONE
		}

		setComments(recycler, moreComments)
	}

	private fun setComments(recycler: RecyclerView, textView: TextView) {

		val commentAdapter = GenericAdapter(SimpleComment::class.java, mapOf(POST to post))

		parent.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {

			val comments = postItemClickListener.onFetchComments(post)

			if (comments.isEmpty()) {
				recycler.visibility = View.GONE
				textView.visibility = View.GONE
			} else {
				for (comment in comments) {
					comment.postTitle = post.title
				}

				recycler.apply {
					visibility = View.VISIBLE
					adapter = commentAdapter
					layoutManager = LinearLayoutManager(recycler.context)
				}

				commentAdapter.submitList(comments)

				textView.setOnClickListener {
					if (post.commentCount > 2) {
						postItemClickListener.onCommentClick(post)
					}
				}
			}

		}

		/*val task = Firebase.firestore.collection(COMMENT_CHANNELS)
			.document(post.commentChannelId)
			.collection(COMMENTS)
			.orderBy(POSTED_AT, Query.Direction.DESCENDING)
			.limit(2)
			.get()

		task.addOnSuccessListener {


		}.addOnFailureListener {
			Log.e(BUG_TAG, "Couldn't get comments. Reason - ${it.localizedMessage}")
		}*/

	}


	private fun setLikeButton(likeBtn: Button, dislikeBtn: Button) {
		likeBtn.isSelected = post.postLocalData.isLiked

		likeBtn.setOnClickListener {
			// the state before changes
			if (auth.currentUser != null) {
				likeBtn.isSelected = !likeBtn.isSelected
				if (post.postLocalData.isDisliked) {
					dislikeBtn.isSelected = false
				}
				itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
					post = postItemClickListener.onLikePressed(post)
				}
				setMetadata()
			} else {
				postItemClickListener.onNotSignedIn(post)
			}
		}
	}

	private fun setDislikeButton(likeBtn: Button, dislikeBtn: Button) {
		dislikeBtn.isSelected = post.postLocalData.isDisliked

		dislikeBtn.setOnClickListener {
			// the state before changes
			if (auth.currentUser != null) {
				dislikeBtn.isSelected = !dislikeBtn.isSelected
				if (post.postLocalData.isLiked) {
					likeBtn.isSelected = false
				}
				itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
					post = postItemClickListener.onDislikePressed(post)
				}
				setMetadata()
			} else {
				postItemClickListener.onNotSignedIn(post)
			}
		}
	}

	private fun setMetadata() {
		val text = view.findViewById<TextView>(R.id.meta)
		val ft = if (post.type == PROJECT) {
			"${post.likes} Likes • ${post.dislikes} Dislikes • ${post.commentCount} Comments • ${post.contributors?.size ?: 0} Contributors"
		} else {
			"${post.likes} Likes • ${post.dislikes} Dislikes • ${post.commentCount} Comments"
		}
		text?.text = ft

		if (post.commentCount > 0) {
			text?.setOnClickListener {
				postItemClickListener.onCommentClick(post)
			}
		}
	}

	private fun setSaveButton(saveBtn: Button) {
		saveBtn.isSelected = post.postLocalData.isSaved

		saveBtn.setOnClickListener {
			if (auth.currentUser != null) {
				saveBtn.isSelected = !saveBtn.isSelected
				itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
					post = postItemClickListener.onSavePressed(post)
				}
			} else {
				postItemClickListener.onNotSignedIn(post)
			}
		}
	}

	companion object {

		@JvmStatic
		fun newInstance(parent: ViewGroup, @LayoutRes layout: Int) =
			PostViewHolder(parent, layout)

	}

}

/*private fun setFollowButton(followBtn: Button) {

		fun setFollowText(isUserFollowed: Boolean) {
			if (isUserFollowed) {
				followBtn.text = "Unfollow"
			} else {
				followBtn.text = "Follow"
			}
		}

		if (post.postLocalData.isCreator) {
			followBtn.visibility = View.GONE
		} else {
			followBtn.visibility = View.VISIBLE

			setFollowText(post.postLocalData.isUserFollowed)

			followBtn.setOnClickListener {
				if (auth.currentUser != null) {
					setFollowText(!post.postLocalData.isUserFollowed)
					post = postItemClickListener.onFollowPressed(post)
				} else {
					postItemClickListener.onNotSignedIn(post)
				}
			}
		}
	}*/