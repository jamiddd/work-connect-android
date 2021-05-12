package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.model.User

class UserHorizontalAdapter(
    private val context: Context
): PagedListAdapter<User, UserHorizontalViewHolder>(GenericComparator(User::class.java)) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHorizontalViewHolder {
        return UserHorizontalViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.user_item, parent, false), context)
    }

    override fun onBindViewHolder(holder: UserHorizontalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}