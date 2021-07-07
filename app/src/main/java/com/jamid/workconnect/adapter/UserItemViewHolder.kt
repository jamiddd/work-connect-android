package com.jamid.workconnect.adapter

import android.os.Build
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.facebook.drawee.view.SimpleDraweeView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.R
import com.jamid.workconnect.convertDpToPx
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.User

class UserItemViewHolder(parent: ViewGroup, @LayoutRes layout: Int, private val isWide: Boolean = false, private val isHorizontal: Boolean): GenericViewHolder<User>(parent, layout) {

	private val userItemClickListener = itemView.context as UserItemClickListener
	var administrators: List<String>? = null
	val auth = Firebase.auth

	private fun setFollowButton(btn: MaterialButton, otherUser: User) {

		if (auth.currentUser?.uid == otherUser.id) {
			btn.text = itemView.context.getString(R.string.admin)
			btn.icon = null
			btn.isEnabled = false
			return
		}

		fun setFollowText(isUserFollowed: Boolean) {
			if (isUserFollowed) {
				btn.text = itemView.context.getString(R.string.unfollow_text)
			} else {
				btn.text = itemView.context.getString(R.string.follow_text)
			}
		}

		setFollowText(otherUser.isUserFollowed)

		btn.setOnClickListener {
			if (Firebase.auth.currentUser != null) {
				setFollowText(!otherUser.isUserFollowed)
			}

			userItemClickListener.onFollowPressed(otherUser)
		}
	}

	override fun bind(item: User) {

		val images = intArrayOf(R.drawable.pattern, R.drawable.pattern1, R.drawable.pattern2, R.drawable.pattern3)

		val currentCover = images.random()

		val colorBlack = ContextCompat.getColor(itemView.context, R.color.black)

		if (isWide) {
			val cover: SimpleDraweeView = itemView.findViewById(R.id.user_profile_cover)
			val profilePhoto: SimpleDraweeView = itemView.findViewById(R.id.user_profile_photo)
			val displayName: TextView = itemView.findViewById(R.id.user_fullname_text)
			val aboutText: TextView = itemView.findViewById(R.id.user_about_text)
			val followBtn: MaterialButton = itemView.findViewById(R.id.user_follow_button)

			cover.setActualImageResource(currentCover)

			profilePhoto.setImageURI(item.photo)
			displayName.text = item.name
			aboutText.text = item.about

			if (Build.VERSION.SDK_INT <= 27) {
				displayName.setTextColor(colorBlack)
			}

			setFollowButton(followBtn, item)

		} else {
			val img: SimpleDraweeView = itemView.findViewById(R.id.user_horiz_photo) ?: itemView.findViewById(R.id.user_img)
			val name: TextView = itemView.findViewById(R.id.user_horiz_name) ?: itemView.findViewById(R.id.user_name)
			val followBtn: MaterialButton = itemView.findViewById(R.id.user_horiz_btn) ?: itemView.findViewById(R.id.user_follow_btn)
			val userAbout: TextView = itemView.findViewById(R.id.user_horiz_about) ?: itemView.findViewById(R.id.user_about)

			administrators?.let {
				if (it.contains(item.id)) {
					img.hierarchy.roundingParams?.let { params ->
						params.setBorder(ContextCompat.getColor(itemView.context, R.color.blue_500), convertDpToPx(2, itemView.context).toFloat())
						params.setPadding(convertDpToPx(4, itemView.context).toFloat())
					}
				}
			}

			img.setImageURI(item.photo)
			name.text = item.name

			if (Build.VERSION.SDK_INT <= 27) {
				name.setTextColor(colorBlack)
			}

			setFollowButton(followBtn, item)

			userAbout.text = item.about
		}

		itemView.setOnClickListener {
			if (Firebase.auth.currentUser?.uid != item.id) {
				userItemClickListener.onUserPressed(item)
			}
		}
	}

	companion object {

		@JvmStatic
		fun newInstance(parent: ViewGroup, @LayoutRes layout: Int, isWide: Boolean = false, isHorizontal: Boolean = false): UserItemViewHolder {
			return UserItemViewHolder(parent, layout, isWide, isHorizontal)
		}
	}

}