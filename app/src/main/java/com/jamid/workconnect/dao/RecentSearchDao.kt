package com.jamid.workconnect.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jamid.workconnect.model.RecentSearch
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecentSearchDao: BaseDao<RecentSearch>() {

    /*@Query("SELECT * FROM recent_search ORDER BY createdAt DESC")
    abstract fun allRecentSearches(): LiveData<List<RecentSearch>>*/

    @Query("SELECT * FROM recent_search WHERE type = :type ORDER BY createdAt DESC")
    abstract fun getRecentSearchesByType(type: String) : Flow<List<RecentSearch>>

    @Query("DELETE FROM recent_search WHERE `query` = :query")
    abstract suspend fun deleteRecentSearch(query: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertRecentSearch(recentSearch: RecentSearch)

}