package com.jamid.workconnect.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.ChatChannelViewHolder
import com.jamid.workconnect.adapter.paging2.GenericComparator
import com.jamid.workconnect.adapter.paging2.GenericMenuViewHolder
import com.jamid.workconnect.adapter.paging3.PostViewHolder
import com.jamid.workconnect.model.*

@Suppress("UNCHECKED_CAST")
class GenericAdapter<T : Any>(val clazz: Class<T>, val extras: Map<String, Any>? = null) :
	ListAdapter<T, GenericViewHolder<T>>(GenericComparator(clazz)) {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder<T> {
		return when (clazz) {
			User::class.java -> {
				val isHorizontal = extras?.get("isHorizontal") == true
				val isWide = extras?.get("isWide") == false

				val administrators = extras?.get("administrators") as List<String>?

				when {
					isHorizontal -> {
						val userItemViewHolder = UserItemViewHolder.newInstance(parent, R.layout.user_horizontal_layout, isWide, isHorizontal)
						userItemViewHolder.administrators = administrators
						userItemViewHolder
					}
					isWide -> {
						UserItemViewHolder.newInstance(parent, R.layout.user_wide_layout, isWide, isHorizontal)
					}
					else -> {
						val userItemViewHolder = UserItemViewHolder.newInstance(parent, R.layout.user_item, isWide, isHorizontal)
						userItemViewHolder.administrators = administrators
						userItemViewHolder
					}
				}
			}
			GenericMenuItem::class.java -> GenericMenuViewHolder.newInstance(parent, R.layout.generic_menu_item)
			ChatChannel::class.java -> ChatChannelViewHolder.newInstance(parent, R.layout.chat_channel_layout)
			RecentSearch::class.java -> RecentSearchViewHolder.newInstance(parent, R.layout.search_item_layout)
			TagsHolder::class.java -> TagsHolderViewHolder.newInstance(parent, R.layout.tags_holder_layout)
			Post::class.java -> PostViewHolder.newInstance(parent, R.layout.mini_project_item)
			else -> throw ClassCastException("This class is not supported or class unknown.")
		} as GenericViewHolder<T>
	}

	override fun onBindViewHolder(holder: GenericViewHolder<T>, position: Int) {
		holder.bind(getItem(position))
	}

}