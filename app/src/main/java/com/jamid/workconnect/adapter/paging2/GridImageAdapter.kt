package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import com.jamid.workconnect.model.SimpleMessage

class GridImageAdapter(
	val actContext: Context
) : PagedListAdapter<SimpleMessage, GridImageViewHolder>(GenericComparator(SimpleMessage::class.java)) {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridImageViewHolder {
		return GridImageViewHolder.newInstance(parent, actContext)
	}

	override fun onBindViewHolder(holder: GridImageViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

}