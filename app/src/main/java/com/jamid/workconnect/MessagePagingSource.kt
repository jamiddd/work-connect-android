package com.jamid.workconnect

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.PageKey
import com.jamid.workconnect.model.SimpleMessage
import com.jamid.workconnect.model.User

class MessagePagingSource(private val chatChannelId: String, private val repository: MainRepository, private val contributors: List<User>): PagingSource<PageKey, SimpleMessage>() {

	override fun getRefreshKey(state: PagingState<PageKey, SimpleMessage>): PageKey? {
		return state.anchorPosition?.let {
			val page = state.closestPageToPosition(it)
			page?.prevKey ?: page?.nextKey
		}
	}

	override suspend fun load(params: LoadParams<PageKey>): LoadResult<PageKey, SimpleMessage> {
		/*val key = params.key
		val messages = repository.getMessages(chatChannelId, key, params.loadSize)
		return if (messages.isNotEmpty()) {
			*//*val firstSnapshot = repository.mapOfDocumentSnapshots[messages.first().messageId]
			val lastSnapshot = repository.mapOfDocumentSnapshots[messages.last().messageId]
			val pageKey = PageKey(firstSnapshot, lastSnapshot)*//*

			for (message in messages) {
				message.sender = contributors.find {
					it.id == message.senderId
				}?: User()
			}

			LoadResult.Page(messages, pageKey, pageKey)
		} else {
			LoadResult.Page(emptyList(), null, null)
		}*/
		TODO()
	}
}