package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.jamid.workconnect.R

class SearchAdapter<T: Any>(private val clazz: Class<T>, private val context: Context): ListAdapter<T, SearchItemViewHolder>(
	GenericComparator(clazz)
) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder {
		return SearchItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false), context)
	}

	override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
		holder.bind(getItem(position), clazz)
	}

}