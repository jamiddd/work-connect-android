package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jamid.workconnect.MainViewModel
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DocumentViewHolder(val view: View, actContext: Context, val viewModel: MainViewModel): RecyclerView.ViewHolder(view) {

	private val messageItemClickListener = actContext as MessageItemClickListener

	fun bind(message: SimpleMessage?) {
		if (message != null) {
			val name = view.findViewById<TextView>(R.id.doc_name_text)
			val meta = view.findViewById<TextView>(R.id.doc_meta)
			val externalDir = view.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
			val progress = view.findViewById<ProgressBar>(R.id.media_down_progress)

			val nameText = message.metaData?.originalFileName!!

			progress.visibility = View.GONE

			if (message.isDownloaded) {
				name.text = nameText

				val file = File(externalDir, nameText)
				if (file.exists()) {
					view.setOnClickListener {
						messageItemClickListener.onDocumentClick(message)
					}
				} else {
					onMediaNotDownloaded(view, message, externalDir)
				}
			} else {
				/*name.text = nameText
				onMediaNotDownloaded(view, message, externalDir)*/
			}

			val sender = message.sender
			val metaText = "Sent by ${sender.name} • " + SimpleDateFormat("hh:mm a dd/MM/yy", Locale.UK).format(message.createdAt)
			meta.text = metaText
		}
	}

	private fun onMediaNotDownloaded(view: View, message: SimpleMessage, externalDir: File?) {
		val progress = view.findViewById<ProgressBar>(R.id.media_down_progress)
		val downloadBtn = view.findViewById<Button>(R.id.doc_down_btn)
		downloadBtn.visibility = View.VISIBLE

		fun download() {
			progress.visibility = View.VISIBLE
			downloadBtn.visibility = View.INVISIBLE

//			viewModel.downloadMedia(externalDir, message)

			/*viewModel.mediaLiveData(simpleMedia.id).observe(viewLifecycleOwner) { m ->
				if (m != null) {
					downloadBtn.visibility = View.GONE
					progress.visibility = View.GONE

//                            messageItemClickListener.onDocumentClick(m)

					view.setOnClickListener {
//                                messageItemClickListener.onDocumentClick(m)
					}
				}
			}*/
		}

		view.setOnClickListener {
			download()
		}

		downloadBtn.setOnClickListener {
			download()
		}
	}

	companion object {

		@JvmStatic
		fun newInstance(parent: ViewGroup, actContext: Context, viewModel: MainViewModel) : DocumentViewHolder {
			return DocumentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.vertical_document_layout, parent, false), actContext, viewModel)
		}

	}

}