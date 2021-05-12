package com.jamid.workconnect.adapter.paging3

import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.PostItemClickListener
import com.jamid.workconnect.model.BlogItemConverter
import com.jamid.workconnect.model.Post
import java.text.SimpleDateFormat
import java.util.*

class PostViewHolder(
	val view: View
): RecyclerView.ViewHolder(view) {

	private val postItemClickListener = view.context as PostItemClickListener
	private val auth = Firebase.auth
	private lateinit var post: Post

	fun bind(p: Post?) {
		if (p != null) {
			post = p
			setStaticContent()
			setPost()
			view.setOnClickListener {
				postItemClickListener.onItemClick(post, this)
			}
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
		val progressBar = view.findViewById<ProgressBar>(R.id.miniProjectProgressBar)


		// same for both project and blog
		title.text = post.title
		userName.text = post.admin.name
		userImage.setImageURI(post.admin.photo)

		val time = SimpleDateFormat("hh:mm a", Locale.UK).format(post.updatedAt)

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

	}

	private fun setPost() {
		val likeBtn = view.findViewById<Button>(R.id.projectLikeBtn)
		val dislikeBtn = view.findViewById<Button>(R.id.projectDislikeBtn)
		val saveBtn = view.findViewById<Button>(R.id.projectSaveBtn)
		val followBtn = view.findViewById<Button>(R.id.projectAdminFollowBtn)

		setLikeButton(likeBtn, dislikeBtn)
		setDislikeButton(likeBtn, dislikeBtn)
		setFollowButton(followBtn)
		setSaveButton(saveBtn)
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
				post = postItemClickListener.onLikePressed(post)
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
				post = postItemClickListener.onDislikePressed(post)
			} else {
				postItemClickListener.onNotSignedIn(post)
			}
		}
	}

	private fun setFollowButton(followBtn: Button) {

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
	}

	private fun setSaveButton(saveBtn: Button) {
		saveBtn.isSelected = post.postLocalData.isSaved

		saveBtn.setOnClickListener {
			if (auth.currentUser != null) {
				saveBtn.isSelected = !saveBtn.isSelected
				post = postItemClickListener.onSavePressed(post)
			} else {
				postItemClickListener.onNotSignedIn(post)
			}
		}
	}

	companion object {

		@JvmStatic
		fun newInstance(view: View) =
			PostViewHolder(view)

	}

}