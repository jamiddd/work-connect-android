package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "recent_search")
@Parcelize
data class RecentSearch(
    @PrimaryKey
    val query: String,
    val searchId: String,
    val type: String,
    val createdAt: Long = System.currentTimeMillis(),
    @Embedded(prefix = "recent_post_")
    val recentPost: Post? = null,
    @Embedded(prefix = "recent_user_")
    val recentUser: User? = null
): Parcelable {
    constructor(): this("", "", "", 0)
}