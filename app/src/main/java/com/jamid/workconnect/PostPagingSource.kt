package com.jamid.workconnect

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.PageKey
import com.jamid.workconnect.model.Post

class PostPagingSource(private val repository: MainRepository): PagingSource<PageKey, Post>() {

	override fun getRefreshKey(state: PagingState<PageKey, Post>): PageKey? {
		return state.anchorPosition?.let {
			val page = state.closestPageToPosition(it)
			page?.prevKey ?: page?.nextKey
		}
	}

	override suspend fun load(params: LoadParams<PageKey>): LoadResult<PageKey, Post> {
		val key = params.key
		Log.d(BUG_TAG, "Starting post paging source ... ")
		val posts = repository.getPosts(key, params.loadSize)
		return if (posts.isNotEmpty()) {
			Log.d(BUG_TAG, "Posts is not empty")
			val firstSnapshot = repository.mapOfDocumentSnapshots[posts.first().id]
			val lastSnapshot = repository.mapOfDocumentSnapshots[posts.last().id]
			val pageKey = PageKey(firstSnapshot, lastSnapshot)
			LoadResult.Page(posts, pageKey, pageKey)
		} else {
			Log.d(BUG_TAG, "Posts is empty")
			LoadResult.Page(emptyList(), null, null)
		}
	}
}