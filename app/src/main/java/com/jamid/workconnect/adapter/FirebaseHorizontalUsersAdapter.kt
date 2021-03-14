package com.jamid.workconnect.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.UserItemClickListener
import com.jamid.workconnect.model.UserMinimal

class FirebaseHorizontalUsersAdapter(options: FirestorePagingOptions<UserMinimal>, val userItemClickListener: UserItemClickListener): FirestorePagingAdapter<UserMinimal, FirebaseHorizontalUsersAdapter.FirebaseHorizontalUserViewHolder>(
    options) {

    inner class FirebaseHorizontalUserViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        fun bind(user: UserMinimal?) {
            if (user != null) {
                val img = view.findViewById<SimpleDraweeView>(R.id.user_img)
                val name = view.findViewById<TextView>(R.id.user_name)

                img.setImageURI(user.photo)
                name.text = user.name

                view.setOnClickListener {
                    userItemClickListener.onUserPressed(user.id)
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FirebaseHorizontalUserViewHolder {
        return FirebaseHorizontalUserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false))
    }

    override fun onBindViewHolder(holder: FirebaseHorizontalUserViewHolder, position: Int, model:UserMinimal) {
        holder.bind(getItem(position)?.toObject(model::class.java))
    }

}