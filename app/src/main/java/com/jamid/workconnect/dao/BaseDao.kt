package com.jamid.workconnect.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
abstract class BaseDao<T> {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(item : T)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(items: List<T>)

    @Update
    abstract suspend fun updateItem(item: T)

    @Update
    abstract suspend fun updateItems(items: List<T>)

}