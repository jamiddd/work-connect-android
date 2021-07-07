package com.jamid.workconnect

import android.util.Log
import android.view.View
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.*
import kotlinx.coroutines.tasks.await

class PostPagingSource(private val postSource: PostSource, private val repository: MainRepository): PagingSource<PageKey, Post>() {

	override fun getRefreshKey(state: PagingState<PageKey, Post>): PageKey? {
		return state.anchorPosition?.let {
			val page = state.closestPageToPosition(it)
			page?.prevKey ?: page?.nextKey
		}
	}

	override suspend fun load(params: LoadParams<PageKey>): LoadResult<PageKey, Post> {
		val key = params.key

		val postsSnapshotResult = if (key != null) {
			when {
				key.startAfter != null -> {
					repository.firebaseUtility.getItems(postSource, startAfter = key.startAfter)
				}
				key.endBefore != null -> {
					repository.firebaseUtility.getItems(postSource, endBefore = key.endBefore)
				}
				else -> {
					Log.d(BUG_TAG, "Key is null")
					return LoadResult.Page(emptyList(), null, null)
				}
			}
		} else {
			Log.d(BUG_TAG, "Key is null - getting random")
			repository.firebaseUtility.getItems(postSource)
		}

		return when (postsSnapshotResult) {
			is Result.Error -> {
				LoadResult.Error(postsSnapshotResult.exception)
			}
			is Result.Success -> {
				val postSnapshot = postsSnapshotResult.data

				if (postSnapshot.isEmpty) {
					return LoadResult.Page(emptyList(), null, null)
				}

				Log.d(BUG_TAG, postSnapshot.size().toString())

				val firstSnapshot = postSnapshot.first()
				repository.mapOfDocumentSnapshots[firstSnapshot.id] = firstSnapshot
				val lastSnapshot = postSnapshot.last()
				repository.mapOfDocumentSnapshots[lastSnapshot.id] = lastSnapshot

				val unfilteredPosts = postSnapshot.toObjects(Post::class.java)
				val posts = repository.filterPosts(unfilteredPosts)

				return if (postSnapshot.size() < params.loadSize) {
					LoadResult.Page(posts, PageKey(startAfter = null, endBefore = firstSnapshot), PageKey(startAfter = lastSnapshot, endBefore = null))
				} else {
					LoadResult.Page(posts, PageKey(startAfter = null, endBefore = firstSnapshot), PageKey(startAfter = lastSnapshot, endBefore = null))
				}
			}
		}
	}
}