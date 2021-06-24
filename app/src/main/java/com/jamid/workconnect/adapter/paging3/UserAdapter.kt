package com.jamid.workconnect.adapter.paging3

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.UserItemViewHolder
import com.jamid.workconnect.model.User

class UserAdapter: PagingDataAdapter<User, UserItemViewHolder>(GenericComparator2(User::class.java)) {

    override fun onBindViewHolder(holder: UserItemViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserItemViewHolder {
        return UserItemViewHolder.newInstance(parent, R.layout.user_wide_layout, isWide = true, isHorizontal = false)
    }

}