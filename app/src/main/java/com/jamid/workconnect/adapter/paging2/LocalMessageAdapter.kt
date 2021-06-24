package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.DOCUMENT
import com.jamid.workconnect.IMAGE
import com.jamid.workconnect.LINKS
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.model.SimpleMessage

class MessageAdapter(
	private val viewModel: MainViewModel
): ListAdapter<SimpleMessage, RecyclerView.ViewHolder>(GenericComparator(SimpleMessage::class.java)) {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return when (viewType) {
			1 -> GridImageViewHolder.newInstance(parent, parent.context)
			2 -> LinksViewHolder.newInstance(parent, parent.context)
			3 -> DocumentViewHolder.newInstance(parent, parent.context, viewModel)
			else -> GridImageViewHolder.newInstance(parent, parent.context)
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val message = getItem(position)
		when (holder) {
			is GridImageViewHolder -> holder.bind(message)
			is LinksViewHolder -> holder.bind(message)
			is DocumentViewHolder -> holder.bind(message)
		}
	}

	override fun getItemViewType(position: Int): Int {
		val message = getItem(position)
		return if (message != null) {
			when (message.type) {
				IMAGE -> 1
				LINKS -> 2
				DOCUMENT -> 3
				else -> 0
			}
		} else {
			super.getItemViewType(position)
		}
	}

}