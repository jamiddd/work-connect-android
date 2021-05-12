package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.User

class UserHorizontalViewHolder(val view: View, actContext: Context): RecyclerView.ViewHolder(view) {

    private val userItemClickListener = actContext as UserItemClickListener

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

    fun bind(user: User?) {
        if (user != null) {
            val img = view.findViewById<SimpleDraweeView>(R.id.user_img)
            val name = view.findViewById<TextView>(R.id.user_name)
            val followBtn = view.findViewById<Button>(R.id.user_follow_btn)

            img.setImageURI(user.photo)
            name.text = user.name

            view.setOnClickListener {
                userItemClickListener.onUserPressed(user)
            }

            if (Firebase.auth.currentUser?.uid == user.id) {
                followBtn.visibility = View.INVISIBLE
            }

            setFollowButton(followBtn, user)
        }
    }

}