package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import com.jamid.workconnect.model.User

class UserAdapter(val context: Context): PagedListAdapter<User, UserViewHolder>(GenericComparator(User::class.java)) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder.newInstance(parent, context)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}