package com.jamid.workconnect.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserMinimal (
    val id: String,
    val name: String,
    val email: String,
    val username: String,
    val photo: String?
): Parcelable {
    constructor(): this("", "", "", "", null)
}