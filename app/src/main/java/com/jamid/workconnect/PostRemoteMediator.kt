package com.jamid.workconnect

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.PageKey
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result

@ExperimentalPagingApi
class PostRemoteMediator(
	private val repository: MainRepository,
	private val extras: Map<String, Any?>? = null
) : RemoteMediator<PageKey, Post>() {
	override suspend fun load(
		loadType: LoadType,
		state: PagingState<PageKey, Post>
	): MediatorResult {
		Log.d(BUG_TAG, "Post remote mediator started.")
		val key = state.anchorPosition?.let {
			state.closestPageToPosition(it)?.prevKey ?: state.closestPageToPosition(it)?.nextKey
		}

		val postsCollectionRef = Firebase.firestore.collection(POSTS)

		val postsSnapshotResult = when (loadType) {
			LoadType.REFRESH -> {
				Log.d(BUG_TAG, "Refreshing posts ... ")
				repository.clearPosts()
				repository.firebaseUtility.getItemsFromFirebase(
					query = postsCollectionRef,
					pageSize = state.config.initialLoadSize,
					extras = extras
				)
			}
			LoadType.PREPEND -> {
				Log.d(BUG_TAG, "Prepending posts ... ")
				repository.firebaseUtility.getItemsFromFirebase(
					key?.endBefore,
					null,
					postsCollectionRef,
					state.config.pageSize,
					extras
				)
			}
			LoadType.APPEND -> {
				Log.d(BUG_TAG, "Appending posts ... ")
				repository.firebaseUtility.getItemsFromFirebase(
					null,
					key?.startAfter,
					postsCollectionRef,
					state.config.pageSize,
					extras
				)
			}
		}

		Log.d(BUG_TAG, "Got the posts ... ")

		when (postsSnapshotResult) {
			is Result.Success -> {
				Log.d(BUG_TAG, "Got the posts .. it was a success ... ")
				val postSnapshot = postsSnapshotResult.data

				Log.d(BUG_TAG, postSnapshot.size().toString())

				if (postSnapshot.isEmpty) {
					return MediatorResult.Success(endOfPaginationReached = true)
				}

				Log.d(BUG_TAG, "Post snapshot is not empty ..")

				val firstSnapshot = postSnapshot.first()
				repository.mapOfDocumentSnapshots[firstSnapshot.id] = firstSnapshot
				val lastSnapshot = postSnapshot.last()
				repository.mapOfDocumentSnapshots[lastSnapshot.id] = lastSnapshot

				val posts = postSnapshot.toObjects(Post::class.java)

				Log.d(BUG_TAG, "ARGGHH")

				return if (postSnapshot.size() < state.config.pageSize) {
					repository.insertPosts(posts, extras)
					MediatorResult.Success(endOfPaginationReached = true)
				} else {
					repository.insertPosts(posts, extras)
					MediatorResult.Success(endOfPaginationReached = false)
				}
			}
			is Result.Error -> {
				Log.d(BUG_TAG, "Didn't get the posts .. it was an error ... ${postsSnapshotResult.exception.localizedMessage}")
				return MediatorResult.Error(postsSnapshotResult.exception)
			}
		}
	}
}