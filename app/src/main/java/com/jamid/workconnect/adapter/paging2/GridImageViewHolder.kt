package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMessage
import java.io.File

class GridImageViewHolder(val view: View, val context: Context): RecyclerView.ViewHolder(view) {

	private val messageItemClickListener = context as MessageItemClickListener

	fun bind(message: SimpleMessage?) {
		if (message != null) {
			val imageView = view.findViewById<SimpleDraweeView>(R.id.square_image_grid_item)
			ViewCompat.setTransitionName(imageView, message.messageId)
			view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
				val file = File(it, message.metaData!!.originalFileName)

				val uri = FileProvider.getUriForFile(view.context, "com.jamid.workconnect.fileprovider", file)
				imageView.setImageURI(uri.toString())
			}

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