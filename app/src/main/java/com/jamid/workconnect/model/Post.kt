package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.jamid.workconnect.PROJECT
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = false)
    var id: String,
    var title: String,
    @Embedded(prefix = "admin_")
    var admin: User,
    var uid: String,
    var commentChannelId: String,
    var likes: Long = 0,
    var dislikes: Long = 0,
    var commentCount: Long = 0,
    var chatChannelId: String? = null,
    var guidelines: String? = null,
    @Embedded(prefix = "location_")
    var location: SimpleLocation? = null,
    var content: String? = null,
    var images: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var tags: List<String> = emptyList(),
    var links: List<String> = emptyList(),
    var contributors: List<String>? = null,
    var indices: List<String> = emptyList(),
    var searchRank: Long = 0,
    var weightage: Double = 0.0,
    var items: List<String>? = null,
    var type: String = PROJECT,
    @Embedded(prefix = "post_local_")
    @Exclude @set: Exclude @get: Exclude
    var postLocalData: PostLocalData = PostLocalData()
): Parcelable {
    constructor(): this(
    "",
    "",
    User(),
    "",
    ""
    )
}