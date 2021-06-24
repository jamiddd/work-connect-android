package com.jamid.workconnect.adapter.paging2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.User

class UserViewHolder(val view: View): RecyclerView.ViewHolder(view) {

    private val userItemClickListener = view.context as UserItemClickListener

    fun bind(user: User?) {
        if (user != null) {
            val userPhoto = view.findViewById<SimpleDraweeView>(R.id.user_horiz_photo)
            val userName = view.findViewById<TextView>(R.id.user_horiz_name)
            val secondaryText = view.findViewById<TextView>(R.id.user_horiz_about)
            val primaryBtn = view.findViewById<Button>(R.id.user_horiz_btn)

            userName.text = user.name
            userPhoto.setImageURI(user.photo)
            val usernameText = "@${user.username}"
            secondaryText.text = usernameText

            primaryBtn.setOnClickListener {
                Toast.makeText(view.context, "Not implemented yet.", Toast.LENGTH_LONG).show()
            }

            view.setOnClickListener {
                userItemClickListener.onUserPressed(user)
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(parent: ViewGroup) = UserViewHolder(LayoutInflater.from(parent.context).inflate(
            R.layout.user_horizontal_layout, parent, false))
    }
}