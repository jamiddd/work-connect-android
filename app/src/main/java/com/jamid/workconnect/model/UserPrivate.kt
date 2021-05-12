package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserPrivate(
	var interests: List<String> = emptyList(),
    var likedPosts: List<String> = emptyList(),
    var dislikedPosts: List<String> = emptyList(),
    var savedPosts: List<String> = emptyList(),
    var collaborationIds: List<String> = emptyList(),
    var projectIds: List<String> = emptyList(),
    var blogIds: List<String> = emptyList(),
    var followers: List<String> = emptyList(),
    var followings: List<String> = emptyList(),
    var activeRequests: List<String> = emptyList(),
    var requestIds: List<String> = emptyList(),
    var chatChannels: List<String> = emptyList(),
    var registrationTokens: List<String> = emptyList(),
    var notificationIds: List<String> = emptyList(),
    var notificationReferences: List<String> = emptyList()
): Parcelable {
	constructor(): this(emptyList())
}