package com.jamid.workconnect.model

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostLocalData(
	var isLiked: Boolean = false,
	var isSaved: Boolean = false,
	var isDisliked: Boolean = false,
	var isUserFollowed: Boolean = false,
	var isCreator: Boolean = false,
	var isCollaboration: Boolean  = false,
	var inFeed: Boolean = false
): Parcelable {
	constructor(): this(false, false, false, false, false, false, false)
}
