package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    var name: String,
    var username: String,
    var email: String,
    var about: String? = null,
    var photo: String? = null,
    var searchRank: Long = 0,
    var indices: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis(),
    var weightage: Double = 0.0,
    @Embedded(prefix = "user_")
    @Exclude @set: Exclude @get: Exclude
    var userPrivate: UserPrivate = UserPrivate(),
    @Exclude @set: Exclude @get: Exclude
    var isUserFollowed: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    var isUserFollowingMe: Boolean = false,
    @Exclude @set: Exclude @get: Exclude
    var isCurrentUser: Boolean = false
): Parcelable {
    constructor(): this("", "", "", "")
}

