package com.jamid.workconnect.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contributors")
data class Contributor (
	@PrimaryKey
	val uid: String,
	@Embedded(prefix="user_")
	val user: User,
	val projects: List<String>,
	val chatChannels: List<String>
)
