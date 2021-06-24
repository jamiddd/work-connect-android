package com.jamid.workconnect

import android.util.Log
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
import com.jamid.workconnect.model.User
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

		val key = state.anchorPosition?.let {
			state.closestPageToPosition(it)?.prevKey ?: state.closestPageToPosition(it)?.nextKey
		}

		val messagesCollectionRef = Firebase.firestore
				.collection(CHAT_CHANNELS)
				.document(chatChannel.chatChannelId)
				.collection(MESSAGES)

		val messagesSnapshotResult = when (loadType) {
			LoadType.REFRESH -> {
				repository.firebaseUtility.getMessagesFromFirebase(messagesCollectionRef, state.config.initialLoadSize)
			}
			LoadType.PREPEND -> {
				return MediatorResult.Success(endOfPaginationReached = true)
			}
			LoadType.APPEND -> {
				if (lastSnapshot != null) {
					Log.d("MessageMediator", "Appending with lastSnapshot not null ${lastSnapshot!!["content"]}")
					repository.firebaseUtility.getMessagesFromFirebase(
						messagesCollectionRef,
						state.config.pageSize,
						lastSnapshot
					)
				} else {
					Log.d("MessageMediator", "Appending with lastSnapshot null")
					Result.Error(Exception("Shit man"))
				}

			}
		}

		return when (messagesSnapshotResult) {
			is Result.Success -> {
				val messagesSnapshot = messagesSnapshotResult.data

				isEmpty = messagesSnapshot.isEmpty

				if (messagesSnapshot.isEmpty) {
					Log.d("MessageMediator", "Empty results")
					return MediatorResult.Success(endOfPaginationReached = true)
				}


				/*val firstSnapshot = messagesSnapshot.first()
				repository.mapOfDocumentSnapshots[firstSnapshot.id] = firstSnapshot
				val lastSnapshot = messagesSnapshot.last()
				repository.mapOfDocumentSnapshots[lastSnapshot.id] = lastSnapshot*/
				lastSnapshot = messagesSnapshot.last()

				val messages = messagesSnapshot.toObjects(SimpleMessage::class.java)

				for (message in messages) {
					repository.firebaseUtility.usersMap[message.senderId]?.let {
						message.sender = it
					}
				}

				return if (messagesSnapshot.size() < state.config.pageSize) {
					repository.insertMessages(externalImagesDir, externalDocumentsDir, messages, chatChannel)
					MediatorResult.Success(endOfPaginationReached = true)
				} else {
					repository.insertMessages(externalImagesDir, externalDocumentsDir, messages, chatChannel)
					MediatorResult.Success(endOfPaginationReached = false)
				}

			}
			is Result.Error -> MediatorResult.Error(messagesSnapshotResult.exception)
		}
	}
}