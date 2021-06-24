package com.jamid.workconnect.adapter.paging2

import androidx.recyclerview.widget.DiffUtil
import com.jamid.workconnect.model.*

class GenericComparator<T: Any>(private val clazz: Class<T>): DiffUtil.ItemCallback<T>() {
	override fun areItemsTheSame(oldItem: T, newItem: T): Boolean {
		return when (clazz) {
			User::class.java -> {
				val oldUser = oldItem as User
				val newUser = newItem as User

				return oldUser.id == newUser.id
			}
			SimpleMessage::class.java -> {
				val oldMessage = oldItem as SimpleMessage
				val newMessage = newItem as SimpleMessage

				return oldMessage.messageId == newMessage.messageId
			}
			Post::class.java -> {
				val oldPost = oldItem as Post
				val newPost = newItem as Post

				return oldPost.id == newPost.id
			}
			SimpleNotification::class.java -> {
				val oldNotification = oldItem as SimpleNotification
				val newNotification = newItem as SimpleNotification

				return oldNotification.id == newNotification.id
			}
			ChatChannel::class.java -> {
				val oldChannel = oldItem as ChatChannel
				val newChatChannel = newItem as ChatChannel

				return oldChannel.chatChannelId == newChatChannel.chatChannelId
			}
			GenericMenuItem::class.java -> {
				val oldMenuItem = oldItem as GenericMenuItem
				val newMenuItem = newItem as GenericMenuItem

				return oldMenuItem.item == newMenuItem.item
			}
			InterestItem::class.java -> {
				val oldMenuItem = oldItem as InterestItem
				val newMenuItem = newItem as InterestItem

				return oldMenuItem.id == newMenuItem.id
			}
			else -> true
		}
	}

	override fun areContentsTheSame(oldItem: T, newItem: T): Boolean {
		return when (clazz) {
			User::class.java -> {
				val oldUser = oldItem as User
				val newUser = newItem as User

				return oldUser == newUser
			}
			SimpleMessage::class.java -> {
				val oldMessage = oldItem as SimpleMessage
				val newMessage = newItem as SimpleMessage

				return oldMessage == newMessage
			}
			Post::class.java -> {
				val oldPost = oldItem as Post
				val newPost = newItem as Post

				return oldPost == newPost
			}
			SimpleNotification::class.java -> {
				val oldNotification = oldItem as SimpleNotification
				val newNotification = newItem as SimpleNotification

				return oldNotification == newNotification
			}
			ChatChannel::class.java -> {
				val oldChannel = oldItem as ChatChannel
				val newChatChannel = newItem as ChatChannel

				return oldChannel == newChatChannel
			}
			GenericMenuItem::class.java -> {
				val oldMenuItem = oldItem as GenericMenuItem
				val newMenuItem = newItem as GenericMenuItem

				return oldMenuItem == newMenuItem
			}
			InterestItem::class.java -> {
				val oldMenuItem = oldItem as InterestItem
				val newMenuItem = newItem as InterestItem

				return oldMenuItem == newMenuItem
			}
			else -> true
		}
	}
}