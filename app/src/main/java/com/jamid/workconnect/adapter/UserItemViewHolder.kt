package com.jamid.workconnect.adapter

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.User

class UserItemViewHolder(parent: ViewGroup, @LayoutRes layout: Int): GenericViewHolder<User>(parent, layout) {

	private val userItemClickListener = itemView.context as UserItemClickListener

	private fun setFollowButton(btn: Button, otherUser: User) {

		fun setFollowText(isUserFollowed: Boolean) {
			if (isUserFollowed) {
				btn.text = "Unfollow"
			} else {
				btn.text = "Follow"
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
		val img = itemView.findViewById<SimpleDraweeView>(R.id.user_img)
		val name = itemView.findViewById<TextView>(R.id.user_name)
		val followBtn = itemView.findViewById<Button>(R.id.user_follow_btn)

		img.setImageURI(item.photo)
		name.text = item.name

		if (Build.VERSION.SDK_INT <= 27) {
			name.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
		}

		itemView.setOnClickListener {
			userItemClickListener.onUserPressed(item)
		}

		if (Firebase.auth.currentUser?.uid == item.id) {
			followBtn.visibility = View.INVISIBLE
		}

		setFollowButton(followBtn, item)
	}

	companion object {
		fun newInstance(parent: ViewGroup, @LayoutRes layout: Int): UserItemViewHolder {
			return UserItemViewHolder(parent, layout)
		}
	}

}