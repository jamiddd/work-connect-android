package com.jamid.workconnect.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.workconnect.model.SimpleMedia

@Dao
interface SimpleMediaDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE)
    suspend fun insertSimpleMedia(medias: List<SimpleMedia>?)

    @Query("SELECT * FROM simple_media WHERE id = :id LIMIT 1")
    suspend fun getSimpleMedia(id: String): SimpleMedia?

    @Query("SELECT * FROM simple_media WHERE id = :messageId LIMIT 1")
    fun getSimpleMediaLive(messageId: String): LiveData<SimpleMedia>
}