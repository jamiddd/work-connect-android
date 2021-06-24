package com.jamid.workconnect

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jamid.workconnect.data.MainRepository
import com.jamid.workconnect.model.Result
import com.jamid.workconnect.model.SimpleRequest

@OptIn(ExperimentalPagingApi::class)
class SimpleRequestRemoteMediator(private val repository: MainRepository): RemoteMediator<Int, SimpleRequest>() {

	private var lastSnapshot: DocumentSnapshot? = null

	override suspend fun load(
		loadType: LoadType,
		state: PagingState<Int, SimpleRequest>
	): MediatorResult {

		val initialQuery = Firebase.firestore.collection(USERS).document(repository.currentLocalUser.value!!.id).collection(
			REQUESTS).orderBy(CREATED_AT, Query.Direction.DESCENDING)

		val notificationSnapshotResult = when (loadType) {
			LoadType.REFRESH -> {
				repository.clearRequests()
				repository.firebaseUtility.getPagedRequests(initialQuery, state.config.initialLoadSize, null)
			}
			LoadType.PREPEND -> {
				return MediatorResult.Success(endOfPaginationReached = true)
			}
			LoadType.APPEND -> {
				if (lastSnapshot != null) {
					repository.firebaseUtility.getPagedRequests(initialQuery, state.config.pageSize, lastSnapshot)
				} else {
					return MediatorResult.Success(endOfPaginationReached = true)
				}
			}
		}

		return when (notificationSnapshotResult) {
			is Result.Success -> {
				val notificationSnapshot = notificationSnapshotResult.data

				if (notificationSnapshot.isEmpty) {
					return MediatorResult.Success(endOfPaginationReached = true)
				}

				lastSnapshot = notificationSnapshot.last()

				val requests = notificationSnapshot.toObjects(SimpleRequest::class.java)

				return if (notificationSnapshot.size() < state.config.pageSize) {
					repository.insertRequests(requests)
					MediatorResult.Success(endOfPaginationReached = true)
				} else {
					repository.insertRequests(requests)
					MediatorResult.Success(endOfPaginationReached = false)
				}
			}
			is Result.Error -> {
				MediatorResult.Error(notificationSnapshotResult.exception)
			}
		}
	}
}