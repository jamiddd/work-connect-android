package com.jamid.workconnect

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.PageKey
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.User
import com.jamid.workconnect.model.UserSource

@ExperimentalPagingApi
class UserRemoteMediator(private val userSource: UserSource, private val repository: MainRepository): RemoteMediator<PageKey, User>() {
	override suspend fun load(
		loadType: LoadType,
		state: PagingState<PageKey, User>
	): MediatorResult {

		Log.d(FOLLOWERS, "Starting User remote mediator .. ")

		val key = state.anchorPosition?.let {
			state.closestPageToPosition(it)?.prevKey ?: state.closestPageToPosition(it)?.nextKey
		}
		
		val userSnapshotResult = when (loadType) {
			LoadType.REFRESH -> {
				Log.d(FOLLOWERS, "User remote mediator .. refreshing")
				repository.clearUsers()
				repository.firebaseUtility.getItems(userSource, 20)
			}
			LoadType.PREPEND -> {
				Log.d(FOLLOWERS, "User remote mediator .. prepending")
				repository.firebaseUtility.getItems(userSource, 20, endBefore = key?.endBefore)
			}
			LoadType.APPEND -> {
				Log.d(FOLLOWERS, "User remote mediator .. appending")
				repository.firebaseUtility.getItems(userSource, 20, startAfter = key?.startAfter)
			}
		}

		when (userSnapshotResult) {
			is Result.Success -> {
				Log.d(FOLLOWERS, "User remote mediator .. user snapshot is success")
				val userSnapshot = userSnapshotResult.data

				if (userSnapshot.isEmpty) {
					Log.d(FOLLOWERS, "User remote mediator .. user snapshot is empty")
					return MediatorResult.Success(endOfPaginationReached = true)
				}

				val firstSnapshot = userSnapshot.first()
				repository.mapOfDocumentSnapshots[firstSnapshot.id] = firstSnapshot
				val lastSnapshot = userSnapshot.last()
				repository.mapOfDocumentSnapshots[lastSnapshot.id] = lastSnapshot

				val users = userSnapshot.toObjects(User::class.java)

				return if (userSnapshot.size() < state.config.pageSize) {
					Log.d(FOLLOWERS, "User remote mediator .. user snapshot has reached end with size ${userSnapshot.size()}")
					repository.insertUsers(users, userSource)
					MediatorResult.Success(endOfPaginationReached = true)
				} else {
					Log.d(FOLLOWERS, "User remote mediator .. user snapshot has not reached end with size ${userSnapshot.size()}")
					repository.insertUsers(users, userSource)
					MediatorResult.Success(endOfPaginationReached = false)
				}
			}
			is Result.Error -> {
				Log.d(FOLLOWERS, userSnapshotResult.exception.localizedMessage.toString())
				return MediatorResult.Error(userSnapshotResult.exception)
			}
		}
	}
}