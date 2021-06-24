package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PostAdmin(val name: String, val username: String, val photo: String? = null, val registrationTokens: List<String> = emptyList()): Parcelable {
    constructor(): this("", "")
}