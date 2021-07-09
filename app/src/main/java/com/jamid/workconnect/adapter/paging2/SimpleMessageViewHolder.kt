package com.jamid.workconnect.adapter.paging2

import android.annotation.SuppressLint
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMessage
import com.jamid.workconnect.views.zoomable.ImageControllerListener
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SimpleMessageViewHolder(
	val view: View,
	val viewType: Int,
	private val isGrid: Boolean = false
) : RecyclerView.ViewHolder(view) {

	private val messageItemClickListener = view.context as MessageItemClickListener

	val uid = Firebase.auth.currentUser?.uid ?: " "

	@SuppressLint("SetTextI18n")
	fun bind(message: SimpleMessage?) {
		if (message != null) {
			val currentAdapter = (bindingAdapter as SimpleMessageAdapter)

			if (!currentAdapter.colorUserMap.containsKey(message.senderId)) {

				var selectedColorId = currentAdapter.colors.random()
				while (currentAdapter.colorUserMap.containsValue(selectedColorId)) {
					selectedColorId = currentAdapter.colors.random()
				}
				currentAdapter.colorUserMap[message.senderId] = selectedColorId
			}

			val downloadBtn = view.findViewById<Button>(R.id.downloadBtn)
			val downloadTextBtn = view.findViewById<TextView>(R.id.downloadTextButton)

			if (isGrid) {
				val imageView = view.findViewById<SimpleDraweeView>(R.id.square_image_grid_item)
				imageView.setImageURI(message.content)

				imageView.setOnClickListener {
					messageItemClickListener.onImageClick(imageView, message)
				}
			} else {
				val isCurrentUserMessage = message.senderId == uid
				if (isCurrentUserMessage) {
					val timeT = view.findViewById<TextView>(R.id.currentUserMessageTime)
					val time2 = view.findViewById<TextView>(R.id.currentUserImgMsgTime)

					timeT?.text = SimpleDateFormat(
						"hh:mm a",
						Locale.UK
					).format(message.createdAt)

					time2?.text = SimpleDateFormat(
						"hh:mm a",
						Locale.UK
					).format(message.createdAt)

					val tail1 = view.findViewById<ImageView>(R.id.rightMsgTail)
					val tail2 = view.findViewById<ImageView>(R.id.rightImgMsgTail)
					val imgMsg = view.findViewById<SimpleDraweeView>(R.id.currentUserImgMsg)
					val textMsg = view.findViewById<TextView>(R.id.currentUserMessage)
					val progress = view.findViewById<ProgressBar>(R.id.rightMsgProgress)

					when (viewType) {
						CURRENT_USER_AT_START -> {
							tail1?.visibility = View.GONE
							textMsg.text = message.content
							textMsg.visibility = View.VISIBLE
						}
						CURRENT_USER_AT_MIDDLE -> {
							tail1?.visibility = View.GONE
							textMsg.text = message.content
							textMsg.visibility = View.VISIBLE
							view.updateLayout(margin = 0)
						}
						CURRENT_USER_AT_START_IMAGE -> {
							if (message.isDownloaded) {
								setUpImageMessageAfterDownload(imgMsg, message, downloadBtn, downloadTextBtn, progress)
							} else {
								setUpImageMessageBeforeDownload(message, downloadBtn, downloadTextBtn, progress)
							}
						}
						CURRENT_USER_AT_START_DOC -> {
							setUpDocumentMessage(message)
						}
						CURRENT_USER_AT_END -> {
							textMsg.text = message.content
							tail1.visibility = View.VISIBLE

							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
						}
						CURRENT_USER_AT_END_IMAGE -> {
							imgMsg.visibility = View.VISIBLE
							tail2.visibility = View.GONE

							if (message.isDownloaded) {
								setUpImageMessageAfterDownload(imgMsg, message, downloadBtn, downloadTextBtn, progress)
							} else {
								setUpImageMessageBeforeDownload(message, downloadBtn, downloadTextBtn, progress)
							}

							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
						}
						CURRENT_USER_AT_END_DOC -> {
							setUpDocumentMessage(message)
							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
						}
					}

					textMsg?.setOnClickListener {
						messageItemClickListener.onTextClick(timeT)
					}

				} else {
					val timeT = view.findViewById<TextView>(R.id.otherUserMessageTime)
					val time2 = view.findViewById<TextView>(R.id.otherUserImgMessageTime)

					val tail1 = view.findViewById<ImageView>(R.id.leftMsgTail)
					val tail2 = view.findViewById<ImageView>(R.id.leftImgMsgTail)
					val imgMsg = view.findViewById<SimpleDraweeView>(R.id.otherUserImageMessage)
					val textMsg = view.findViewById<TextView>(R.id.otherUserMessage)
					val otherUserT = view.findViewById<SimpleDraweeView>(R.id.otherUserPhoto)
					val otherUserI = view.findViewById<SimpleDraweeView>(R.id.otherUserImgMsgPhoto)
					val otherUserD = view.findViewById<SimpleDraweeView>(R.id.otherUserDocMsgPhoto)
					val progress = view.findViewById<ProgressBar>(R.id.leftMsgProgress)

					val senderName = view.findViewById<TextView>(R.id.senderName)
					currentAdapter.colorUserMap[message.senderId]?.let {
						senderName?.setTextColor(ContextCompat.getColor(view.context, it))
					}

					senderName?.setOnClickListener {
						messageItemClickListener.onUserClick(message.sender)
					}

					timeT?.text = SimpleDateFormat(
						"hh:mm a",
						Locale.UK
					).format(message.createdAt) + " • Sent by ${message.sender.name}"

					time2?.text = SimpleDateFormat(
						"hh:mm a",
						Locale.UK
					).format(message.createdAt) + " • Sent by ${message.sender.name}"

					when (viewType) {
						OTHER_USER_SINGLE_MESSAGE -> {
							otherUserT.setImageURI(message.sender.photo)
							tail1.visibility = View.VISIBLE
							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
							otherUserT.visibility = View.VISIBLE
							textMsg.text = message.content
							senderName.visibility = View.VISIBLE
							senderName.text = message.sender.name
						}
						OTHER_USER_AT_START -> {
							otherUserT.visibility = View.INVISIBLE
							textMsg.text = message.content
							tail1.visibility = View.GONE
							senderName.visibility = View.VISIBLE
							senderName.text = message.sender.name
						}
						OTHER_USER_AT_MIDDLE -> {
							otherUserT.visibility = View.INVISIBLE
							textMsg.text = message.content
							tail1.visibility = View.GONE

							view.updateLayout(margin = 0)
						}
						OTHER_USER_SINGLE_MESSAGE_IMAGE -> {
							otherUserI.setImageURI(message.sender.photo)
							otherUserI.visibility = View.VISIBLE
							tail2.visibility = View.GONE
							senderName.visibility = View.VISIBLE
							senderName.text = message.sender.name

							if (message.isDownloaded) {
								setUpImageMessageAfterDownload(imgMsg, message, downloadBtn, downloadTextBtn, progress)
							} else {
								setUpImageMessageBeforeDownload(message, downloadBtn, downloadTextBtn, progress)
							}
							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
						}
						OTHER_USER_AT_START_IMAGE -> {
							otherUserI.visibility = View.INVISIBLE

							if (message.isDownloaded) {
								setUpImageMessageAfterDownload(imgMsg, message, downloadBtn, downloadTextBtn, progress)
							} else {
								setUpImageMessageBeforeDownload(message, downloadBtn, downloadTextBtn, progress)
							}
						}
						OTHER_USER_SINGLE_MESSAGE_DOC -> {
							senderName.visibility = View.VISIBLE
							senderName.text = message.sender.name
							otherUserD.setImageURI(message.sender.photo)
							otherUserD.visibility = View.VISIBLE
							setUpDocumentMessage(message)
							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
						}
						OTHER_USER_AT_START_DOC -> {
							setUpDocumentMessage(message)
						}
						OTHER_USER_AT_END -> {
							otherUserT.visibility = View.VISIBLE
							otherUserT.setImageURI(message.sender.photo)
							tail1.visibility = View.VISIBLE
							textMsg.text = message.content
							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
						}
						OTHER_USER_AT_END_IMAGE -> {
							otherUserI.setImageURI(message.sender.photo)
							otherUserI.visibility = View.VISIBLE
							tail2.visibility = View.GONE

							if (message.isDownloaded) {
								setUpImageMessageAfterDownload(imgMsg, message, downloadBtn, downloadTextBtn, progress)
							} else {
								setUpImageMessageBeforeDownload(message, downloadBtn, downloadTextBtn, progress)
							}

							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
						}
						OTHER_USER_AT_END_DOC -> {
							otherUserD.setImageURI(message.sender.photo)
							otherUserD.visibility = View.VISIBLE
							setUpDocumentMessage(message)
							view.updateLayout(marginBottom = convertDpToPx(8, view.context))
						}
					}

					textMsg?.setOnClickListener {
						messageItemClickListener.onTextClick(timeT)
					}
				}
			}
		}
	}

	private fun setUpDocumentMessage(message: SimpleMessage) {
		val size = message.metaData?.size_b ?: 0

		val sizeText = when {
			size > (1024 * 1024) -> {
				val sizeInMB = size.toFloat()/(1024 * 1024)
				sizeInMB.toString().take(4) + " MB"
			}
			size/1024 > 100 -> {
				val sizeInMB = size.toFloat()/(1024 * 1024)
				sizeInMB.toString().take(4) + " MB"
			}
			else -> {
				val sizeInKB = size.toFloat()/1024
				sizeInKB.toString().take(3) + " KB"
			}
		}

		val parent = view.findViewById<View>(R.id.docMessageRoot)
		val docName = view.findViewById<TextView>(R.id.docMessageName)
		val docSize = view.findViewById<TextView>(R.id.docMessageSize)
		val downloadBtn = view.findViewById<Button>(R.id.downloadBtn)
		val downloadTextBtn = view.findViewById<Button>(R.id.downloadTextButton)
		val docProgress = view.findViewById<ProgressBar>(R.id.docProgress)

		docName.text = message.metaData?.originalFileName
		docSize.text = sizeText

		if (message.isDownloaded) {
			setUpDocumentMessageAfterDownload(parent, message, downloadBtn, downloadTextBtn, docProgress)
		} else {
			setUpDocumentMessageBeforeDownload(message, downloadBtn, downloadTextBtn, docProgress)
		}

	}

	private fun setUpDocumentMessageBeforeDownload(message: SimpleMessage, downloadBtn: Button, downloadTextBtn: TextView, progress: ProgressBar) {
		downloadBtn.visibility = View.VISIBLE
		downloadTextBtn.visibility = View.VISIBLE

		downloadBtn.setOnClickListener {
			progress.visibility = View.VISIBLE
			downloadBtn.visibility = View.GONE
			downloadTextBtn.visibility = View.GONE
			download(message)
		}

		downloadTextBtn.setOnClickListener {
			progress.visibility = View.VISIBLE
			download(message)
		}

	}

	private fun setUpDocumentMessageAfterDownload(v: View, message: SimpleMessage, downloadBtn: Button, downloadTextBtn: TextView, progress: ProgressBar) {
		downloadBtn.visibility = View.GONE
		downloadTextBtn.visibility = View.GONE
		progress.visibility = View.GONE

		v.setOnClickListener {
			messageItemClickListener.onDocumentClick(message)
		}
	}

	private fun setUpImageMessageBeforeDownload(
		message: SimpleMessage,
		downloadBtn: Button,
		downloadTextBtn: TextView,
		progress: ProgressBar
	) {
		downloadBtn.visibility = View.VISIBLE
		downloadTextBtn.visibility = View.VISIBLE

		downloadBtn.setOnClickListener {
			progress.visibility = View.VISIBLE
			downloadBtn.visibility = View.GONE
			downloadTextBtn.visibility = View.GONE
			download(message)
		}

		downloadTextBtn.setOnClickListener {
			progress.visibility = View.VISIBLE
			downloadBtn.visibility = View.GONE
			downloadTextBtn.visibility = View.GONE
			download(message)
		}

	}

	private fun setUpImageMessageAfterDownload(imageView: SimpleDraweeView, message: SimpleMessage, downloadBtn: Button, downloadTextBtn: TextView, progress: ProgressBar) {
		downloadBtn.visibility = View.GONE
		downloadTextBtn.visibility = View.GONE
		progress.visibility = View.GONE


		ViewCompat.setTransitionName(imageView, message.messageId)

		view.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let {
			val file = File(it, message.metaData!!.originalFileName)

			val uri = FileProvider.getUriForFile(view.context, "com.jamid.workconnect.fileprovider", file)

			val imageControllerListener = ImageControllerListener()

			val imageRequest = ImageRequest.fromUri(uri)
			val controller: DraweeController = Fresco.newDraweeControllerBuilder()
				.setImageRequest(imageRequest)
				.setControllerListener(imageControllerListener)
				.build()

			imageView.controller = controller

			messageItemClickListener.onImageSet(message, imageControllerListener)
		}

		imageView.setOnClickListener {
			messageItemClickListener.onImageClick(imageView, message)
		}
	}

	private fun download(message: SimpleMessage) {
		messageItemClickListener.onMediaDownloadClick(this, message)
	}

}