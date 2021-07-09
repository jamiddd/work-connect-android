package com.jamid.workconnect

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.ChatChannel
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleMessage
import java.io.File

@ExperimentalPagingApi
class MessagesRemoteMediator(
	private val repository: MainRepository,
	private val chatChannel: ChatChannel,
	private val externalImagesDir: File,
	private val externalDocumentsDir: File
): RemoteMediator<Int, SimpleMessage>() {

	private var lastSnapshot: DocumentSnapshot? = null
	var isEmpty = false

	override suspend fun load(
		loadType: LoadType,
		state: PagingState<Int, SimpleMessage>
	): MediatorResult {

		/*val key = state.anchorPosition?.let {
			state.closestPageToPosition(it)?.prevKey ?: state.closestPageToPosition(it)?.nextKey
		}*/

		val messagesCollectionRef = Firebase.firestore
				.collection(CHAT_CHANNELS)
				.document(chatChannel.chatChannelId)
				.collection(MESSAGES)

		val messagesResult = when (loadType) {
			LoadType.REFRESH -> {
				repository.firebaseUtility.getMessagesFromFirebase(messagesCollectionRef, state.config.initialLoadSize)
			}
			LoadType.PREPEND -> {
				return MediatorResult.Success(endOfPaginationReached = true)
			}
			LoadType.APPEND -> {
				if (lastSnapshot != null) {
					repository.firebaseUtility.getMessagesFromFirebase(
						messagesCollectionRef,
						state.config.pageSize,
						lastSnapshot
					)
				} else {
					Result.Error(Exception("Error occurred while appending messages."))
				}

			}
		}

		return when (messagesResult) {
			is Result.Success -> {
				val messages = messagesResult.data

				isEmpty = messages.isEmpty()

				if (messages.isEmpty()) {
					return MediatorResult.Success(endOfPaginationReached = true)
				}

				for (message in messages) {
					repository.usersMap[message.senderId]?.let {
						message.sender = it
					}
				}

				return if (messages.size < state.config.pageSize) {
					repository.insertMessages(externalImagesDir, externalDocumentsDir, messages, chatChannel)
					MediatorResult.Success(endOfPaginationReached = true)
				} else {
					repository.insertMessages(externalImagesDir, externalDocumentsDir, messages, chatChannel)
					MediatorResult.Success(endOfPaginationReached = false)
				}

			}
			is Result.Error -> MediatorResult.Error(messagesResult.exception)
		}
	}
}