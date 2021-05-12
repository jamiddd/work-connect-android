package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaMetaData(
	var size_b: Long,
	var originalFileName: String,
	var extension: String
): Parcelable {
	constructor(): this(0, "", "")
}