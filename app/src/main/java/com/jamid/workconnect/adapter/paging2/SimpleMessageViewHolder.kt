package com.jamid.workconnect.adapter.paging2

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.*
import com.jamid.workconnect.interfaces.MessageItemClickListener
import com.jamid.workconnect.model.SimpleMessage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SimpleMessageViewHolder(
	val view: View,
	val viewType: Int,
	val activity: MainActivity,
	private val viewModel: MainViewModel,
) : RecyclerView.ViewHolder(view) {

	private val messageItemClickListener = activity as MessageItemClickListener

	val uid = Firebase.auth.currentUser?.uid ?: " "

	@SuppressLint("SetTextI18n")
	fun bind(message: SimpleMessage?) {
		if (message != null) {
			val context = view.context
			val isCurrentUserMessage = message.senderId == uid
			if (isCurrentUserMessage) {
				val parent1 = view.findViewById<ConstraintLayout>(R.id.chatRightParent)
				val parent2 = view.findViewById<ConstraintLayout>(R.id.chatImgRightParent)

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
				val docParent1 = view.findViewById<ConstraintLayout>(R.id.chatDocRightParent)

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
						imgMsg.visibility = View.VISIBLE
						val progress = view.findViewById<ProgressBar>(R.id.rightMsgProgress)
						if (!message.isDownloaded) {
							imgMsg.setColorFilter(ContextCompat.getColor(view.context, R.color.semiTransparentDark))
							progress.visibility = View.VISIBLE
						}
						initiateImageMessage(imgMsg, context, parent2, time2, null, message, progress, viewModel)
					}
					CURRENT_USER_AT_START_DOC -> {
						setupDoc(message, docParent1)
					}
					CURRENT_USER_AT_END -> {
						textMsg.text = message.content
						tail1.visibility = View.VISIBLE

						view.updateLayout(marginBottom = convertDpToPx(8, view.context))
					}
					CURRENT_USER_AT_END_IMAGE -> {
						imgMsg.visibility = View.VISIBLE
						tail2.visibility = View.GONE
						val progress = view.findViewById<ProgressBar>(R.id.rightMsgProgress)
						if (!message.isDownloaded) {
							imgMsg.setColorFilter(ContextCompat.getColor(view.context, R.color.semiTransparentDark))
							progress.visibility = View.VISIBLE
						}
						initiateImageMessage(imgMsg, context, parent2, time2, null, message, progress, viewModel)
						view.updateLayout(marginBottom = convertDpToPx(8, view.context))
					}
					CURRENT_USER_AT_END_DOC -> {
						setupDoc(message, docParent1)
						view.updateLayout(marginBottom = convertDpToPx(8, view.context))
					}
				}

				textMsg?.setOnClickListener {
					messageItemClickListener.onTextClick(timeT)
				}

			} else {

				val parent1 = view.findViewById<ConstraintLayout>(R.id.chatLeftParent)
				val parent2 = view.findViewById<ConstraintLayout>(R.id.chatImageLeftParent)

				val timeT = view.findViewById<TextView>(R.id.otherUserMessageTime)
				val time2 = view.findViewById<TextView>(R.id.otherUserImgMessageTime)

				val tail1 = view.findViewById<ImageView>(R.id.leftMsgTail)
				val tail2 = view.findViewById<ImageView>(R.id.leftImgMsgTail)
				val imgMsg = view.findViewById<SimpleDraweeView>(R.id.otherUserImageMessage)
				val textMsg = view.findViewById<TextView>(R.id.otherUserMessage)
				val otherUserT = view.findViewById<SimpleDraweeView>(R.id.otherUserPhoto)
				val otherUserI = view.findViewById<SimpleDraweeView>(R.id.otherUserImgMsgPhoto)
				val otherUserD = view.findViewById<SimpleDraweeView>(R.id.otherUserDocMsgPhoto)

				val docName2 = view.findViewById<TextView>(R.id.otherUserDocName)
				val docParent2 = view.findViewById<ConstraintLayout>(R.id.chatDocLeftParent)

				timeT?.text = SimpleDateFormat(
					"hh:mm a",
					Locale.UK
				).format(message.createdAt) + " • Sent by ${message.sender.name}"

				time2?.text = SimpleDateFormat(
					"hh:mm a",
					Locale.UK
				).format(message.createdAt) + " • Sent by ${message.sender.name}"

				when (viewType) {
					OTHER_USER_AT_START -> {
						otherUserT.visibility = View.INVISIBLE
						textMsg.text = message.content
						tail1.visibility = View.GONE
					}
					OTHER_USER_AT_MIDDLE -> {
						otherUserT.visibility = View.INVISIBLE
						textMsg.text = message.content
						tail1.visibility = View.GONE

						view.updateLayout(margin = 0)
					}
					OTHER_USER_AT_START_IMAGE -> {
						otherUserI.visibility = View.INVISIBLE
						imgMsg.visibility = View.VISIBLE
						val progress = view.findViewById<ProgressBar>(R.id.leftMsgProgress)
						if (!message.isDownloaded) {
							imgMsg.setColorFilter(ContextCompat.getColor(view.context, R.color.semiTransparentDark))
							progress.visibility = View.VISIBLE
						}
						initiateImageMessage(imgMsg, context, parent2, time2, otherUserI, message, progress, viewModel, false)
					}
					OTHER_USER_AT_START_DOC -> {
						setupDoc(message, docParent2)
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
						val progress = view.findViewById<ProgressBar>(R.id.leftMsgProgress)
						imgMsg.visibility = View.VISIBLE
						if (!message.isDownloaded) {
							imgMsg.setColorFilter(ContextCompat.getColor(view.context, R.color.semiTransparentDark))
							progress.visibility = View.VISIBLE
						}
						initiateImageMessage(imgMsg, context, parent2, time2, otherUserI, message, progress, viewModel, false)
						view.updateLayout(marginBottom = convertDpToPx(8, view.context))
					}
					OTHER_USER_AT_END_DOC -> {
						otherUserD.setImageURI(message.sender.photo)
						otherUserD.visibility = View.VISIBLE
						setupDoc(message, docParent2)
						view.updateLayout(marginBottom = convertDpToPx(8, view.context))
					}
				}

				textMsg?.setOnClickListener {
					messageItemClickListener.onTextClick(timeT)
				}
			}
		}
	}

	private fun setupDoc(message: SimpleMessage, view: ViewGroup) {

		val externalDir = view.context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

		val downloadBtnLeft = view.findViewById<Button>(R.id.downloadDocLeft)
		val downloadBtnRight = view.findViewById<Button>(R.id.downloadDocRight)
		val progressBarLeft = view.findViewById<ProgressBar>(R.id.docProgressLeft)
		val progressBarRight = view.findViewById<ProgressBar>(R.id.docProgressRight)

		progressBarLeft?.visibility = View.GONE
		progressBarRight?.visibility = View.GONE

		if (message.isDownloaded) {
			// if the file was previously downloaded
			setDocUi(view, message)

			val file = File(externalDir, message.metaData?.originalFileName!!)
			if (file.exists()) {
				downloadBtnRight?.visibility = View.GONE
				downloadBtnLeft?.visibility = View.GONE
				// file exists at given location

				val messageItemLeft = view.findViewById<LinearLayout>(R.id.otherUserDocMessage)
				val messageItemRight = view.findViewById<LinearLayout>(R.id.currentUserDocMsg)

				messageItemLeft?.setOnClickListener {
					messageItemClickListener.onDocumentClick(message)
				}

				messageItemRight?.setOnClickListener {
					messageItemClickListener.onDocumentClick(message)
				}
			} else {
				// if the file was removed externally
				onMediaNotDownloaded(message, view, externalDir)
			}
		} else {
			// if the file was never downloaded
			onMediaNotDownloaded(message, view, externalDir)
		}

	}

	private fun setDocUi(view: ViewGroup, message: SimpleMessage) {

		val currentUserDocName = view.findViewById<TextView>(R.id.currentUserDocName)
		val currentUserDocSize = view.findViewById<TextView>(R.id.currentUserDocSize)
		val otherUserDocName = view.findViewById<TextView>(R.id.otherUserDocName)
		val otherUserDocSize = view.findViewById<TextView>(R.id.otherUserDocSize)

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

		otherUserDocSize?.text = sizeText
		currentUserDocSize?.text = sizeText

		currentUserDocName?.text = message.metaData?.originalFileName
		otherUserDocName?.text = message.metaData?.originalFileName

	}

	private fun onMediaNotDownloaded(message: SimpleMessage, view: ViewGroup, externalDir: File?) {

		setDocUi(view, message)

		val downloadBtnLeft = view.findViewById<Button>(R.id.downloadDocLeft)
		val downloadBtnRight = view.findViewById<Button>(R.id.downloadDocRight)
		val progressBarLeft = view.findViewById<ProgressBar>(R.id.docProgressLeft)
		val progressBarRight = view.findViewById<ProgressBar>(R.id.docProgressRight)

		downloadBtnLeft?.visibility = View.VISIBLE
		downloadBtnRight?.visibility = View.VISIBLE

		fun download(p: ProgressBar, b: Button) {
			p.visibility = View.VISIBLE
			b.visibility = View.GONE

			viewModel.downloadMedia(externalDir, message)
		}

		downloadBtnLeft?.setOnClickListener {
			download(progressBarLeft, downloadBtnLeft)
		}

		downloadBtnRight?.setOnClickListener {
			download(progressBarRight, downloadBtnRight)
		}

		view.setOnClickListener {
			progressBarLeft?.let { download(progressBarLeft, downloadBtnLeft) }
			progressBarRight?.let { download(progressBarRight, downloadBtnRight) }
		}
	}

	private fun initiateImageMessage(
		v: SimpleDraweeView,
		context: Context,
		parent: ConstraintLayout,
		timeText: TextView,
		userPhoto: SimpleDraweeView?,
		message: SimpleMessage,
		progress: ProgressBar,
		viewModel: MainViewModel,
		right: Boolean = true
	) {
		val controller = MessageImageControllerListener(v, parent, timeText, userPhoto, context, progress, right, message, viewModel)
		val imgRequest = ImageRequest.fromUri(message.content)

		val imgController = Fresco.newDraweeControllerBuilder()
			.setImageRequest(imgRequest)
			.setControllerListener(controller)
			.build()

		v.controller = imgController

		v.setOnClickListener {
			if (controller.measuredWidth != 0) {
				messageItemClickListener.onImageClick(v, message)
			}
		}
	}

}