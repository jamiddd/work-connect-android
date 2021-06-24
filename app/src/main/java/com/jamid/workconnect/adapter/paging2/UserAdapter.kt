package com.jamid.workconnect.adapter.paging2

import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.workconnect.model.User

class UserAdapter(): PagingDataAdapter<User, UserViewHolder>(GenericComparator(User::class.java)) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        return UserViewHolder.newInstance(parent)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}