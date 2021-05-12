package com.jamid.workconnect.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.ChatChannelViewHolder
import com.jamid.workconnect.adapter.paging2.GenericComparator
import com.jamid.workconnect.adapter.paging2.GenericMenuViewHolder
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.GenericMenuItem
import com.jamid.workconnect.model.User

@Suppress("UNCHECKED_CAST")
class GenericAdapter<T : Any>(val clazz: Class<T>) :
	ListAdapter<T, GenericViewHolder<T>>(GenericComparator(clazz)) {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder<T> {
		return when (clazz) {
			User::class.java -> UserItemViewHolder.newInstance(parent, R.layout.user_item)
			GenericMenuItem::class.java -> GenericMenuViewHolder.newInstance(parent, R.layout.generic_menu_item)
			ChatChannel::class.java -> ChatChannelViewHolder.newInstance(parent, R.layout.chat_channel_layout)
			else -> throw ClassCastException("This class is not supported or class unknown.")
		} as GenericViewHolder<T>
	}

	override fun onBindViewHolder(holder: GenericViewHolder<T>, position: Int) {
		holder.bind(getItem(position))
	}

}