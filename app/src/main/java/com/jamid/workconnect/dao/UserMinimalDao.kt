package com.jamid.workconnect.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import com.jamid.workconnect.model.UserMinimal

@Dao
abstract class UserMinimalDao: BaseDao<UserMinimal>() {

    @Query("SELECT * FROM user_minimals")
    abstract fun randomUsers(): DataSource.Factory<Int, UserMinimal>

}