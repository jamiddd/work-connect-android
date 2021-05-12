package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MicroPost(val id: String, val title: String, val img: String? = null, val chatChannelId: String? = null): Parcelable {
	constructor(): this("", "")
}