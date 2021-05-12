package com.jamid.workconnect

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.PageKey
import com.jamid.workconnect.model.SimpleMessage

@ExperimentalPagingApi
class MessagesRemoteMediator(
	private val repository: MainRepository,
	private val extras: Map<String, Any?>? = null
): RemoteMediator<PageKey, SimpleMessage> {

	override suspend fun load(
		loadType: LoadType,
		state: PagingState<PageKey, SimpleMessage>
	): MediatorResult {
		val key = state.anchorPosition?.let {
			state.closestPageToPosition(it)?.prevKey ?: state.closestPageToPosition(it)?.nextKey
		}
		if (extras != null) {
			val chatChannel = extras[CHAT_CHANNEL] as ChatChannel?
			if (chatChannel != null) {
				val messagesCollectionRef = Firebase.firestore.collection(CHAT_CHANNELS).document(chatChannel.chatChannelId)
					.collection(MESSAGES)

				val messagesSnapshotResult = when (loadType) {
					LoadType.REFRESH -> {
//						repository.clearMessages(chatChannel.chatChannelId)
					}
					LoadType.PREPEND -> {

					}
					LoadType.APPEND -> {

					}
				}

			}
		}
	}
}