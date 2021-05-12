package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.model.SimpleMessage

class DocumentAdapter(private val actContext: Context, val viewModel: MainViewModel): PagedListAdapter<SimpleMessage, DocumentViewHolder>(
	GenericComparator(SimpleMessage::class.java)
) {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
		return DocumentViewHolder.newInstance(parent, actContext, viewModel)
	}

	override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
		holder.bind(getItem(position))
	}

}