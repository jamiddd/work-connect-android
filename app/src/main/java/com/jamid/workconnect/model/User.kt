package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    var name: String,
    var username: String,
    var email: String,
    var about: String?,
    var photo: String?,
    var interests: List<String>,
    var likedPosts: List<String>,
    var dislikedPosts: List<String>,
    var savedPosts: List<String>,
    var collaborationIds: List<String>,
    var projectIds: List<String>,
    val blogIds: List<String>,
    var followers: List<String>,
    var followings: List<String>,
    var activeRequests: List<String>,
    var chatChannels: List<String>,
    val createdAt: Long,
    var registrationTokens: List<String>
): Parcelable {
    constructor(): this("", "", "", "", null, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), System.currentTimeMillis(), emptyList())
}