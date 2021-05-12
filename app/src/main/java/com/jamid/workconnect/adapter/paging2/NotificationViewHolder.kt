package com.jamid.workconnect.adapter.paging2

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.jamid.workconnect.ACCEPT_PROJECT
import com.jamid.workconnect.DECLINE_PROJECT
import com.jamid.workconnect.JOIN_PROJECT
import com.jamid.workconnect.R
import com.jamid.workconnect.interfaces.NotificationClickListener
import com.jamid.workconnect.model.SimpleNotification
import com.jamid.workconnect.model.SimpleRequest

class NotificationViewHolder(val view: View, actContext: Context): RecyclerView.ViewHolder(view) {

	private val notificationClickListener = actContext as NotificationClickListener

	fun <T: Any> bind(obj: T?, clazz: Class<T>) {
		val image = view.findViewById<SimpleDraweeView>(R.id.relevantImgView)
		val content = view.findViewById<TextView>(R.id.notificationContent)
		val title = view.findViewById<TextView>(R.id.notificationTitle)
		val positiveBtn = view.findViewById<Button>(R.id.positiveBtn)
		val negativeBtn = view.findViewById<Button>(R.id.negativeBtn)
		when (clazz) {
			SimpleNotification::class.java -> {
				val notification = obj as SimpleNotification?
				if (notification != null) {

					val post = notification.post
					if (post != null) {
						image.setImageURI(post.img)
						title.text = post.title
						val user = notification.sender

						when (notification.type) {
							JOIN_PROJECT -> {

								val contentText = user.name + " wants to join your project - " + post.title
								content.text = contentText

								positiveBtn.visibility = View.VISIBLE
								negativeBtn.visibility = View.VISIBLE

								positiveBtn.setOnClickListener {
									notificationClickListener.onNotificationPositiveClicked(notification, clazz)
								}

								negativeBtn.setOnClickListener {
									notificationClickListener.onNotificationNegativeClicked(notification, clazz)
								}
							}
							ACCEPT_PROJECT -> {
								val contentText = user.name + " has accepted your project request - " + post.title
								content.text = contentText

								positiveBtn.visibility = View.GONE
								negativeBtn.visibility = View.GONE
							}
							DECLINE_PROJECT -> {
								val contentText = user.name + " has declined your project request - " + post.title
								content.text = contentText

								positiveBtn.visibility = View.GONE
								negativeBtn.visibility = View.GONE
							}
						}

						view.setOnClickListener {
							notificationClickListener.onNotificationItemClick(post.id)
						}
					}
				}
			}
			SimpleRequest::class.java -> {
				val request = obj as SimpleRequest?
				if (request != null) {
					val post = request.post

					image.setImageURI(post.img)
					title.text = post.title

					val contentText = "Your request to join the project has not been accepted yet."
					content.text = contentText

					positiveBtn.text = "Undo"
					positiveBtn.visibility = View.VISIBLE

					positiveBtn.setOnClickListener {
						notificationClickListener.onNotificationPositiveClicked(request, clazz)
					}

					negativeBtn.visibility = View.GONE
					negativeBtn.setOnClickListener {
						// not required here
					}

					view.setOnClickListener {
						notificationClickListener.onNotificationItemClick(post.id)
					}

				}
			}
			else -> throw ClassCastException("Incompatible class used.")
		}
	}

	companion object {

		@JvmStatic
		fun newInstance(parent: ViewGroup, actContext: Context): NotificationViewHolder {
			return NotificationViewHolder(
				LayoutInflater.from(parent.context)
					.inflate(
						R.layout.notification_item,
						parent,
						false
					),
					actContext
			)
		}

	}
}