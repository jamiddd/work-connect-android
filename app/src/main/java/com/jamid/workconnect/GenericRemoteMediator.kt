package com.jamid.workconnect

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.firebase.firestore.DocumentSnapshot
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.Post
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.User
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class GenericRemoteMediator<T: Any>(
	private val clazz: Class<T>,
	private val repo: MainRepository,
	private val extras: Map<String, Any>?
): RemoteMediator<String, T>(){

	override suspend fun load(loadType: LoadType, state: PagingState<String, T>): MediatorResult {
		return try {
			val loadKey: String? = when(loadType) {
				LoadType.REFRESH -> null
				LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
				LoadType.APPEND -> {
					val lastItem = state.lastItemOrNull()
						?: return MediatorResult.Success(endOfPaginationReached = true)

					when (clazz) {
						User::class.java -> {
							val lastUser = lastItem as User
							lastUser.id
						}
						Post::class.java -> {
							val lastPost = lastItem as Post
							lastPost.id
						}
						else -> return MediatorResult.Error(ClassCastException("This class is not supported: ${clazz::getCanonicalName}"))
					}
				}
			}

			return if (loadKey != null) {
				when (val docResult = repo.firebaseUtility.getSnapshot(loadKey, clazz)) {
					is Result.Success -> {
						val documentSnapshot = docResult.data
						getItems(loadType, documentSnapshot)
					}
					is Result.Error -> MediatorResult.Error(docResult.exception)
				}
			} else {
				getItems(loadType)
			}
		} catch (e: IOException) {
			MediatorResult.Error(e)
		} catch (e: Exception) {
			MediatorResult.Error(e)
		}
	}

	private suspend fun getItems(loadType: LoadType, documentSnapshot: DocumentSnapshot? = null): MediatorResult {
		/*val query = Firebase.firestore.collection(POSTS)
			.orderBy(CREATED_AT, Query.Direction.DESCENDING)
		return when (val querySnapshotResult = repo.firebaseUtility.fetchItems(documentSnapshot, query, PAGE_SIZE)) {
			is Result.Success -> {
				val querySnapshot = querySnapshotResult.data
				when (clazz) {
					Post::class.java -> {
						if (loadType == LoadType.REFRESH) {
							repo.clearPosts()
						}
						val posts = querySnapshot.toObjects(clazz)
						repo.insertPosts(posts, extras)
						MediatorResult.Success(endOfPaginationReached = querySnapshot.size() < PAGE_SIZE)
					}
					else -> MediatorResult.Error(ClassCastException("This class is not supported: ${clazz::getCanonicalName}"))
				}
			}
			is Result.Error -> MediatorResult.Error(querySnapshotResult.exception)
		}*/
		TODO()
	}

	companion object {

		private const val PAGE_SIZE: Long = 20

	}
}
