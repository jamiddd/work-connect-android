package com.jamid.workconnect.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jamid.workconnect.model.SimpleRequest

@Dao
abstract class ActiveRequestDao: BaseDao<SimpleRequest>() {

	@Query("SELECT * FROM requests ORDER BY createdAt")
	abstract fun getPagedActiveRequests(): DataSource.Factory<Int, SimpleRequest>

	@Delete
	abstract suspend fun deleteRequest(request: SimpleRequest)

	@Query("SELECT * FROM requests WHERE id = :requestId")
	abstract suspend fun getRequest(requestId: String): SimpleRequest?

	@Query("DELETE from requests")
	abstract fun clearRequests()

}