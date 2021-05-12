package com.jamid.workconnect.data

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.jamid.workconnect.dao.BaseDao
import com.jamid.workconnect.model.SimpleNotification

@Dao
abstract class NotificationDao: BaseDao<SimpleNotification>() {

	@Query("SELECT * FROM notifications ORDER BY createdAt DESC")
	abstract fun getPagedNotifications(): DataSource.Factory<Int, SimpleNotification>

	@Delete
	abstract fun deleteNotification(notification: SimpleNotification)

}