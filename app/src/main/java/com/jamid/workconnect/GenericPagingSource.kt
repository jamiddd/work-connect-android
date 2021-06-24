package com.jamid.workconnect

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.jamid.workconnect.data.MainRepository

class GenericPagingSource<T: Any>(private val repo: MainRepository, private val clazz: Class<T>): PagingSource<String, T>() {
	override fun getRefreshKey(state: PagingState<String, T>): String? {
		state.anchorPosition?.let {
			val anchorPage = state.closestPageToPosition(it)
			anchorPage?.prevKey
		}
		TODO()
	}

	override suspend fun load(params: LoadParams<String>): LoadResult<String, T> {
		return try {
			/*return when (clazz) {
				Post::class.java -> {
					val posts = repo.getLocalPosts(params)
					val nextKey = posts.lastOrNull()
					val prevKey = posts.firstOrNull()
					LoadResult.Page(repo.getLocalPosts(params), prevKey, nextKey)
				}
				else -> LoadResult.Error(Exception("Clazz is not supported."))
			}*/
			TODO()
		} catch (e: Exception) {
			LoadResult.Error(e)
		}
	}
}
