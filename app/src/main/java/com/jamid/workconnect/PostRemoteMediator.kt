package com.jamid.workconnect

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.PageKey
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.PostSource
import com.jamid.workconnect.model.Result

@ExperimentalPagingApi
class PostRemoteMediator(
	private val source: PostSource,
	private val repository: MainRepository
) : RemoteMediator<PageKey, Post>() {

	override suspend fun load(
		loadType: LoadType,
		state: PagingState<PageKey, Post>
	): MediatorResult {

		val key = state.anchorPosition?.let {
			state.closestPageToPosition(it)?.prevKey ?: state.closestPageToPosition(it)?.nextKey
		}

		val postsSnapshotResult = when (loadType) {
			LoadType.REFRESH -> {
				// TODO("Should posts be cleared?")
				repository.clearPosts()
				repository.firebaseUtility.getItems(source)
			}
			LoadType.PREPEND -> {
				repository.firebaseUtility.getItems(source, endBefore = key?.endBefore)
			}
			LoadType.APPEND -> {
				repository.firebaseUtility.getItems(source, startAfter = key?.startAfter)
			}
		}

		when (postsSnapshotResult) {
			is Result.Success -> {
				val postSnapshot = postsSnapshotResult.data

				if (postSnapshot.isEmpty) {
					return MediatorResult.Success(endOfPaginationReached = true)
				}

				val firstSnapshot = postSnapshot.first()
				repository.mapOfDocumentSnapshots[firstSnapshot.id] = firstSnapshot
				val lastSnapshot = postSnapshot.last()
				repository.mapOfDocumentSnapshots[lastSnapshot.id] = lastSnapshot

				val posts = postSnapshot.toObjects(Post::class.java)

				return if (postSnapshot.size() < state.config.pageSize) {
					repository.insertPosts(posts, source)
					MediatorResult.Success(endOfPaginationReached = true)
				} else {
					repository.insertPosts(posts, source)
					MediatorResult.Success(endOfPaginationReached = false)
				}
			}
			is Result.Error -> {
				return MediatorResult.Error(postsSnapshotResult.exception)
			}
		}
	}
}