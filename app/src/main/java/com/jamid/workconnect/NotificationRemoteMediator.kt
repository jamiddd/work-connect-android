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
import com.jamid.workconnect.model.SimpleNotification

@OptIn(ExperimentalPagingApi::class)
class NotificationRemoteMediator(private val repository: MainRepository): RemoteMediator<Int, SimpleNotification>() {

	private var lastSnapshot: DocumentSnapshot? = null

	override suspend fun load(
		loadType: LoadType,
		state: PagingState<Int, SimpleNotification>
	): MediatorResult {

		val initialQuery = Firebase.firestore.collection(USERS).document(repository.currentLocalUser.value!!.id).collection(
			NOTIFICATIONS).orderBy(CREATED_AT, Query.Direction.DESCENDING)

		val notificationSnapshotResult = when (loadType) {
			LoadType.REFRESH -> {
				repository.clearNotifications()
				repository.firebaseUtility.getPagedNotifications(initialQuery, state.config.initialLoadSize, null)
			}
			LoadType.PREPEND -> {
				return MediatorResult.Success(endOfPaginationReached = true)
			}
			LoadType.APPEND -> {
				if (lastSnapshot != null) {
					repository.firebaseUtility.getPagedNotifications(initialQuery, state.config.pageSize, lastSnapshot)
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

				val notifications = notificationSnapshot.toObjects(SimpleNotification::class.java)

				return if (notificationSnapshot.size() < state.config.pageSize) {
					repository.insertNotifications(notifications)
					MediatorResult.Success(endOfPaginationReached = true)
				} else {
					repository.insertNotifications(notifications)
					MediatorResult.Success(endOfPaginationReached = false)
				}
			}
			is Result.Error -> {
				MediatorResult.Error(notificationSnapshotResult.exception)
			}
		}

	}
}