package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMessage

class GridImageViewHolder(val view: View, val context: Context): RecyclerView.ViewHolder(view) {

	private val messageItemClickListener = context as MessageItemClickListener

	fun bind(message: SimpleMessage?) {
		if (message != null) {
			val imageView = view.findViewById<SimpleDraweeView>(R.id.square_image_grid_item)
			/*val width = getWindowWidth() / 3
			imageView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, width)*/
			imageView.setImageURI(message.content)

			imageView.setOnClickListener {
				messageItemClickListener.onImageClick(imageView, message)
			}
		}
	}

	companion object {

		@JvmStatic
		fun newInstance(parent: ViewGroup, actContext: Context): GridImageViewHolder {
			return GridImageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.square_image_grid, parent, false), actContext)
		}

	}
}