package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.R
import com.jamid.workconnect.model.SimpleMessage

class LinksViewHolder(val view: View, val actContext: Context): RecyclerView.ViewHolder(view) {
	fun bind(message: SimpleMessage?) {
		if (message != null) {
			//
		}
	}

	companion object {

		@JvmStatic
		fun newInstance(parent: ViewGroup, actContext: Context) : LinksViewHolder {
			return LinksViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.vertical_document_layout, parent, false), actContext)
		}

	}
}