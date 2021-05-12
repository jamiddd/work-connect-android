package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "simple_tags")
@Parcelize
data class SimpleTag(
	var tag: String,
	var searchRank: Long,
	@PrimaryKey (autoGenerate = true)
	var id: Int = 0
): Parcelable {
	constructor() : this("", 0)
}
