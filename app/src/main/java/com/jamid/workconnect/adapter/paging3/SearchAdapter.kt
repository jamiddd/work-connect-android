package com.jamid.workconnect.adapter.paging3

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.jamid.workconnect.R
import com.jamid.workconnect.adapter.paging2.GenericComparator
import com.jamid.workconnect.adapter.paging2.SearchItemViewHolder

class SearchAdapter<T: Any>(private val clazz: Class<T>, private val context: Context): PagingDataAdapter<T, SearchItemViewHolder>(
    GenericComparator(clazz)
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder {
        return SearchItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false), context)
    }

    override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
        holder.bind(getItem(position), clazz)
    }

}