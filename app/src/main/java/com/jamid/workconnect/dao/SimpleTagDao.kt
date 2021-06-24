package com.jamid.workconnect.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.jamid.workconnect.model.SimpleTag

@Dao
abstract class SimpleTagDao: BaseDao<SimpleTag>() {

	@Query("SELECT DISTINCT simple_tags.tag FROM simple_tags ORDER BY searchRank DESC LIMIT 10")
	abstract fun topTags(): LiveData<List<String>>

}