package com.jamid.workconnect

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.PageKey
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.User
import com.jamid.workconnect.model.UserSource

class UserPagingSource(private val userSource: UserSource, private val repository: MainRepository): PagingSource<PageKey, User>() {

	override fun getRefreshKey(state: PagingState<PageKey, User>): PageKey? {
		return state.anchorPosition?.let {
			val page = state.closestPageToPosition(it)
			page?.prevKey ?: page?.nextKey
		}
	}

	override suspend fun load(params: LoadParams<PageKey>): LoadResult<PageKey, User> {
		val key = params.key

		val userSnapshotResult = if (key != null) {
			when {
				key.startAfter != null -> {
					repository.firebaseUtility.getItems(userSource, params.loadSize, startAfter = key.startAfter)
				}
				key.endBefore != null -> {
					repository.firebaseUtility.getItems(userSource, params.loadSize, endBefore = key.endBefore)
				}
				else -> {
					return LoadResult.Page(emptyList(), null, null)
				}
			}
		} else {
			repository.firebaseUtility.getItems(userSource, params.loadSize)
		}

		when (userSnapshotResult) {
			is Result.Success -> {

				// TODO("Must filter the users before sending to recyclerview")

				val userSnapshot = userSnapshotResult.data

				if (userSnapshot.isEmpty) {
					return LoadResult.Page(emptyList(), null, null)
				}

				val firstSnapshot = userSnapshot.first()
				repository.mapOfDocumentSnapshots[firstSnapshot.id] = firstSnapshot
				val lastSnapshot = userSnapshot.last()
				repository.mapOfDocumentSnapshots[lastSnapshot.id] = lastSnapshot

				val users = userSnapshot.toObjects(User::class.java)

				return if (userSnapshot.size() < params.loadSize) {
					LoadResult.Page(users, PageKey(null, firstSnapshot), PageKey(null, null))
				} else {
					LoadResult.Page(users, PageKey(null, firstSnapshot), PageKey(lastSnapshot, null))
				}
			}
			is Result.Error -> {
				return LoadResult.Error(userSnapshotResult.exception)
			}
		}
	}
}